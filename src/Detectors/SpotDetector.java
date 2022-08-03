package Detectors;

import Helpers.MeasureCalibration;
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
 * Class that analyzes the spot images
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
    private boolean saveMaximaImage;
    private boolean saveMaximaRois;
    private boolean saveThresholdImage;
    private boolean saveThresholdRois;



    //    Showing images
    private boolean showMaximaImage;
    private boolean showThresholdImage;
    private final boolean showPreprocessedImage;


    //    Preprocessing
    //    --> substract background
    private boolean useRollingBallSize;
    private double rollingBallSize;

//    --> macro
    private String macroText;

    //Detection of spots
    //    --> find maxima
    private ImagePlus findMaximaIP;
    private boolean spotByfindMaxima;
    private double prominence;

//    --> threshold
    private boolean spotByThreshold;
    private ImagePlus thresholdIP;
//    private String thresholdMethod;
//    private double minSizeSpot;
    private boolean useWatershed;
    private final Detector detector;

    /**
     *
     * @param image
     * @param spotName
     * @param nameExperiment
     * @param resultsDirectory
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
        this.spotByfindMaxima = false;
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


    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    public void setRollingBallSize(double rollingBallSize){
        this.useRollingBallSize = true;
        this.rollingBallSize = rollingBallSize;
    }
    public void setSpotByfindMaxima(double prominence, boolean showMaximaImage) {
        this.spotByfindMaxima = true;
        this.prominence = prominence;
        this.showMaximaImage = showMaximaImage;
    }

    public void setMaximaSaving(boolean saveMaximaImage,boolean saveMaximaRois){
        this.saveMaximaImage=saveMaximaImage;
        this.saveMaximaRois=saveMaximaRois;
    }

    public void setSpotByThreshold(String thresholdMethod, double minSizeSpot, boolean useWatershed, boolean showThresholdImage) {
        this.spotByThreshold = true;
        this.showThresholdImage = showThresholdImage;
//        this.thresholdMethod = thresholdMethod;
//        this.minSizeSpot = minSizeSpot;
        this.useWatershed = useWatershed;
        macroText = null;
        detector.setThresholdParameters(thresholdMethod,false,minSizeSpot);
    }

    public void setThresholdSaving(boolean saveThresholdImage,boolean saveThresholdRois){
        this.saveThresholdImage = saveThresholdImage;
        this.saveThresholdRois=saveThresholdRois;
    }
    public void setPreprocessingMacro(String macroText){
        this.macroText = macroText;
    }
//    public void setRegionROI(Roi[] regionROI) {
//        this.regionROI = regionROI;
//    }

    public String getNameExperiment() {
        return nameExperiment;
    }

    public String getImageTitle() {
        return image.getTitle();
    }

    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
//        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
//            thresholdIP = preprocessed.duplicate();
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (useWatershed)   detector.getWatershed(thresholdIP.getProcessor()).show();
            }
            if (spotByfindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                /*Find maxima*/
                findMaxima(findMaximaIP);
            }
//            PluginCellProt.closeAllWindows("Preview is done",idsToKeep);
        }
    }

    public boolean prepare(){
        ImagePlus preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
//            thresholdIP = preprocessed.duplicate();
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (useWatershed){
                    thresholdIP = detector.getWatershed(thresholdIP.getProcessor());
                    detector.renameImage(thresholdIP,"watershed");
                }
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (resultsDirectory != null && saveThresholdImage){
                    if (IJ.saveAsTiff(thresholdIP, resultsDirectory +"/Results/Images/"+thresholdIP.getTitle())){
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " was saved in "+ resultsDirectory+"/Results/Images/");
                    }else {
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " could not be saved in  "+ resultsDirectory+"/Results/Images/");
                    }
                }
            }
            if (spotByfindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                if (resultsDirectory != null) {
                    if (saveMaximaRois){
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
                    if (saveMaximaImage){
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

    public void analysisPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableFinal, String type) {
        imageToMeasure.setRoi(regionROI);
        ResultsTable rawMesures = new ResultsTable();
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.MEAN +Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
        analyzer.measure();
//        rawMesures.show("Spot results");
        detector.setResultsAndRename(rawMesures,resultsTableFinal,0,type + "_"+ spotName); /*always first line, because analyzer replace line*/
        if (spotByfindMaxima) {
            findMaximaPerRegion(regionROI, resultsTableFinal,type);
        }
        if (spotByThreshold) {
            findThresholdPerRegion(regionID,regionROI, resultsTableFinal,type);
        }
    }

    private void findThresholdPerRegion(int regionID,Roi regionROI, ResultsTable resultsTableToAdd, String type) {
        RoiManager roiManagerFoci=null;
        int numberSpot = 0;
        if (regionROI!=null){
            thresholdIP.setRoi(regionROI);
            thresholdIP.getProcessor().invertLut();
            roiManagerFoci = detector.analyzeParticles(thresholdIP);
            numberSpot = roiManagerFoci.getCount();
            if (resultsDirectory!=null&&saveThresholdRois && numberSpot>0){
                if (roiManagerFoci.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle() + "_threshold_"+type +(regionID+1)+"_roi.zip")){
                    IJ.log("The ROIs of the "+type + " "+(regionID+1)+" of the image "+image.getTitle() + " by threshold method were saved in "+ resultsDirectory+"/Results/ROIs/");
                }else {
                    IJ.log("The ROIs of the "+type + " "+ (regionID+1)+" of the image "+image.getTitle() + " by threshold method could not be saved in "+ resultsDirectory+"/Results/ROIs/");
                }
            }
        }
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

    private void findMaximaPerRegion(Roi regionROI, ResultsTable resultsTableToAdd, String type) {
//        TODO if region roi == null ?
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
//        resultsTableToAdd.addValue(type + "_"+ spotName + " prominence used", prominence);
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima nr. spots", size);
        resultsTableToAdd.addValue(type + "_"+ spotName + " maxima mean", mean);
    }

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
            if (useRollingBallSize){
                imageToReturn = getSubstractBackground();
            }
            return imageToReturn;
        }else return null;
    }

    private ImagePlus getSubstractBackground() {
        ImagePlus wobkgIP = imageToMeasure.duplicate();
        detector.renameImage(wobkgIP, "withoutBackground");
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkgIP.getProcessor(), rollingBallSize, false, false, false, true, false);
        return wobkgIP;
    }

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
//        findMaxima_IP.setRoi(roi_maxima);
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", "C:/Users/Camille/Downloads/Camille_Stage2022",true);
        nucleiDetector.setMeasureCalibration(new MeasureCalibration());
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setSegmentation(false,true);
        nucleiDetector.setThresholdMethod("Li", 1000, false, true);
        nucleiDetector.prepare();

        ImagePlus protein = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        SpotDetector spotDetector = new SpotDetector(protein, "CY5", "WT_HU_Ac-2re--cell003", "C:/Users/Camille/Downloads/Camille_Stage2022",true);
        spotDetector.setMeasureCalibration(new MeasureCalibration());
        spotDetector.setRollingBallSize(10);
        spotDetector.setzStackParameters("Maximum projection");
//        spotDetector.setSpotByfindMaxima(1000);
        spotDetector.setSpotByThreshold("Li", 10, true,true);
//        spotDetector.setRegionROI(nucleiDetector.getRoiArray());
//        spotDetector.prepare();
        spotDetector.preview();
//        ResultsTable test = new ResultsTable();
//        int numberNuclei = nucleiDetector.getRoiArray().length;
//        for (int nucleus = 0; nucleus < numberNuclei; nucleus++) {
//            spotDetector.analysisPerRegion(nucleus, test,"nucleus");
//            test.incrementCounter();
//        }
//        test.show("Final proteins");;
    }
}
