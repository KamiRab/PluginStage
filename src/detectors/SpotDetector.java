package detectors;

import helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;

/**
 * Author : Camille RABIER
 * Date : 07/06/2022
 * Class for
 * - analyzing the spot images
 * It uses the ROIs obtained from NucleiDetector to analyze per nucleus
 * It analyzes either by threshold+particle analyzer or by find Maxima method
 */
public class SpotDetector {
//    Images
    private final ImagePlus image; /*Image without modifications*/
    private ImagePlus imageToMeasure; /*Image that will be measured : only with projection*/

//    Infos for results
    private final String nameExperiment; /*Part of image name that differentiates them from*/
    private final String spotName;
    private MeasureCalibration measureCalibration;

//    Saving results infos
    private final String resultsDirectory;
    private boolean saveImage;
    private boolean saveRois;


    //    Showing images
    private boolean showMaximaImage;
    private boolean showThresholdImage;
    private final boolean showPreprocessedImage;


    //    Preprocessing
    //    --> subtract background
    private boolean useRollingBallSize;
    private double rollingBallSize;

//    --> macro
    private String macroText;

    //Detection of spots
    //    --> find maxima
    private ImagePlus findMaximaIP;
    private boolean spotByFindMaxima;
    private double prominence;

//    --> threshold
    private boolean spotByThreshold;
    private ImagePlus thresholdIP;
    private boolean useWatershed;
    private final Detector detector;

    /**
     *
     * @param image : image corresponding to spots
     * @param spotName : name of protein analyzed
     * @param nameExperiment : name of image without channel specific information
     * @param resultsDirectory : directory to save results
//     * @param showImage
     */
    public SpotDetector(ImagePlus image, String spotName, String nameExperiment, String resultsDirectory, boolean showPreprocessedImage) {
        detector = new Detector(image, spotName);
        this.resultsDirectory = resultsDirectory;
        this.showPreprocessedImage = showPreprocessedImage;
        this.image = image;
        this.spotName = spotName;
        if (nameExperiment.endsWith("_")) {
            this.nameExperiment = nameExperiment.substring(0, nameExperiment.length() - 1);
        } else {
            this.nameExperiment = nameExperiment;
        }
        this.rollingBallSize = 0;
        this.useRollingBallSize = false;
        this.spotByThreshold = false;
        this.spotByFindMaxima = false;
//        this.regionROI = null;

    }

    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
        this.measureCalibration = measureCalibration;
    }

    /**
     * Set all parameters for projection if necessary
     * @param zStackProj Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for unifying the background
     * @param rollingBallSize : size of ball that should be of the size of the biggest object the user wants to analyze
     */
    public void setRollingBallSize(double rollingBallSize){
        this.useRollingBallSize = true;
        this.rollingBallSize = rollingBallSize;
    }

    /**
     * Set parameters for search of local intensity maxima
     * @param prominence : minimal difference of intensity with neighbors needed to be considered a local maxima
     * @param showMaximaImage : choice to show resulting mask
     */
    public void setSpotByFindMaxima(double prominence, boolean showMaximaImage) {
        this.spotByFindMaxima = true;
        this.prominence = prominence;
        this.showMaximaImage = showMaximaImage;
    }

    /**
     * Set parameters for thresholding and
     * @param thresholdMethod : method of thresholding
     * @param minSizeSpot : minimum size of particle to consider
     * @param useWatershed : if true, use watershed method in addition to thresholding
     * @param showThresholdImage : chooice to show the resulting image
     */
    public void setSpotByThreshold(String thresholdMethod, double minSizeSpot, boolean useWatershed, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
        this.useWatershed = useWatershed;
        macroText = null;
        detector.setThresholdParameters(thresholdMethod,false,minSizeSpot); /*does not exclude spot on edges*/
    }

    /**
     * Set saving's choice
     * @param saveImage : save find maxima or threshold image
     * @param saveRois : save corresponding regions
     */
    public void setSaving(boolean saveImage, boolean saveRois){
        this.saveImage =saveImage;
        this.saveRois =saveRois;
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText){
        this.macroText = macroText;
    }

    /**
     *
     * @return name of image without channel specific information
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    /**
     *
     * @return name of image
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    /**
     *
     */
    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (subtract background, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (useWatershed)   detector.getWatershed(thresholdIP.getProcessor()).show();
            }
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                /*Find maxima*/
                findMaxima(findMaximaIP);
            }
        }
    }
    /**
     * Prepare for measurement
     * - does the preprocessing
     * - if selected by user show preprocessing image
     * - detect spot in the entire image (by threshold and/or find maxima)
     * - if selected by user : show masks with spot detected by chosen method(s)
     * @return true if no error
     */
    public boolean prepare(){
//        Preprocessing
        ImagePlus preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
//            Detection by threshold
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (useWatershed){
                    thresholdIP = detector.getWatershed(thresholdIP.getProcessor());
                    detector.renameImage(thresholdIP,"watershed");
                }
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (resultsDirectory != null && saveImage){
                    if (IJ.saveAsTiff(thresholdIP, resultsDirectory +"/Results/Images/"+thresholdIP.getTitle())){
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " was saved in "+ resultsDirectory+"/Results/Images/");
                    }else {
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " could not be saved in  "+ resultsDirectory+"/Results/Images/");
                    }
                }
            }
//            Detection by find maxima
            if (spotByFindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                if (resultsDirectory != null) {
                    if (saveRois){
                        findMaximaIP.setRoi((Roi)null);
                        PointRoi roiMaxima = findMaxima(findMaximaIP);
                        findMaximaIP.setRoi(roiMaxima);
                        boolean wasSaved = RoiEncoder.save(roiMaxima, resultsDirectory +"/Results/ROI/" + image.getShortTitle() + "findMaxima_all_roi.roi");
                        if (!wasSaved) {
                            IJ.error("Could not save ROIs");
                        }else {
                            IJ.log("The ROIs of the spot found by find Maxima method of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/" + image.getShortTitle() + "findMaxima_all_roi.roi");
                        }
                    }
                    if (saveImage){
                        ImagePlus toSave = findMaximaIP.flatten();
                        if(IJ.saveAsTiff(toSave, resultsDirectory +"/Results/Images/"+findMaximaIP.getTitle())){
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " was saved in "+ resultsDirectory+"/Results/Images/"+findMaximaIP.getTitle());
                        } else {
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/Images/");
                        }
                    }
                }
            }
            return true;
        }else return false;
    }

    /**
     * Detect spot in region wanted : nucleus, cell or cytoplasm
     * @param regionID : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableFinal : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    public void analysisPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableFinal, String type) {
        imageToMeasure.setRoi(regionROI);
        ResultsTable rawMeasures = new ResultsTable();
//        Measures of mean and raw intensities in the whole ROI are always done for spot images
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.MEAN +Measurements.INTEGRATED_DENSITY, rawMeasures);
        analyzer.measure();
        detector.setResultsAndRename(rawMeasures,resultsTableFinal,0,type + "_"+ spotName); /*always first line, because analyzer replace line*/
//        Detection and measurements
        if (spotByFindMaxima) {
            findMaximaPerRegion(regionROI, resultsTableFinal,type);
        }
        if (spotByThreshold) {
            findThresholdPerRegion(regionID,regionROI, resultsTableFinal,type);
        }
    }

    /**
     * Detect and measure spot in specific region by threshold
     * @param regionID : id of objet (number in list of ROI) for saving of threshold roi
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    private void findThresholdPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        RoiManager roiManagerFoci=null;
        int numberSpot = 0; /*count number of spot detected*/
//        Detection
        if (regionROI!=null){
            thresholdIP.setRoi(regionROI);
            thresholdIP.getProcessor().invertLut();
            roiManagerFoci = detector.analyzeParticles(thresholdIP);
            numberSpot = roiManagerFoci.getCount();
//            --> Saving
            if (resultsDirectory!=null&&saveRois && numberSpot>0){
                if (roiManagerFoci.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle() + "_threshold_"+type +(regionID+1)+"_roi.zip")){
                    IJ.log("The ROIs of the "+type + " "+(regionID+1)+" of the image "+image.getTitle() + " by threshold method were saved in "+ resultsDirectory+"/Results/ROIs/");
                }else {
                    IJ.log("The ROIs of the "+type + " "+ (regionID+1)+" of the image "+image.getTitle() + " by threshold method could not be saved in "+ resultsDirectory+"/Results/ROIs/");
                }
            }
        }
//        Measurement
        resultsTableToAdd.addValue(type+"_"+spotName+" threshold nr. spot",numberSpot);
        if (numberSpot > 0) {
            ResultsTable resultsTable = new ResultsTable();
            Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable);
            for (int spot = 0; spot < numberSpot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();
            }
            detector.setSummarizedResults(resultsTable,resultsTableToAdd,type + "_"+ spotName);
        } else {
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (pixel)", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (" + measureCalibration.getUnit() + ")", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Mean", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold RawIntDen", Double.NaN);
        }
    }

    /**
     * Detect and measure spots by searching local maxima
     * @param regionROI : ROI where the spot have to be detected
     * @param resultsTableToAdd : results table to fill
     * @param type : image, cell, nucleus or cytoplasm
     */
    private void findMaximaPerRegion(Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        findMaximaIP.setRoi(regionROI);
//                    Find maxima
        PointRoi roiMaxima = findMaxima(findMaximaIP);
//                    Get statistics
        int size = 0;
        float mean = 0;
        for (Point p : roiMaxima) {
            size++;
            mean += findMaximaIP.getProcessor().getPixelValue(p.x, p.y);
        }
        mean = mean / size;
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima nr. spots", size);
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima mean", mean);
    }

    /**
     * Preprocess image for better segmentation
     * - Projection
     * - Macro
     * - Subtract background
     * @return ImagePlus
     */
    private ImagePlus preprocessing() {
        if(detector.getImage()!=null){
            IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            imageToMeasure=detector.getImage();
            ImagePlus imageToReturn = detector.getImage().duplicate();
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (macroText!=null){
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
//            SUBTRACT BACKGROUND : correct background with rolling ball algorithm
            if (useRollingBallSize){
                imageToReturn = getSubtractBackground();
            }
            return imageToReturn;
        }else return null;
    }

    /**
     * use rolling ball algorithm to correct background
     * @return image with corrected background
     */
    private ImagePlus getSubtractBackground() {
        ImagePlus wobkgIP = imageToMeasure.duplicate();
        detector.renameImage(wobkgIP, "withoutBackground");
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkgIP.getProcessor(), rollingBallSize, false, false, false, true, false);
        return wobkgIP;
    }

    /**
     * The algorithm scan the image to find all values all the pixel of
     * @param findMaximaIP : image to use for finding local maxima
     * @return : pointRoi corresponding to all local maxima
     */
    private PointRoi findMaxima(ImagePlus findMaximaIP) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor();
        Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaximaIP.setRoi(roiMaxima);
        if (showMaximaImage) {
            findMaximaIP.flatten();
            findMaximaIP.show();
        }
        return roiMaxima;
    }
}
