package Detectors;

import GUI.PluginCellProt;
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
//TODO Measures : nb de spots
//TODO watershed
//TODO what to do if number of cell and nucleus does not corresponds ?

/**
 * Class that analyzes the spot images
 * It uses the ROIs obtained from NucleiDetector to analyze per nucleus
 * It analyzes either by threshold+particle analyzer or by find Maxima method
 */
public class SpotDetector {
    private final ImagePlus image;
    private ImagePlus imageToMeasure;
    private final String nameExperiment;
    private final String spotName;
    private final MeasureCalibration measureCalibration;
    private final String resultsDirectory;

    private boolean useRollingBallSize;
    private double rollingBallSize;

    private ImagePlus findMaximaIP;
    private boolean spotByfindMaxima;
    private double prominence;
    private boolean showMaximaImage;
    private boolean saveMaximaImage;
    private boolean saveMaximaRois;

    private ImagePlus thresholdIP;
    private String thresholdMethod;
    private double minSizeSpot;
    private boolean spotByThreshold;
    private boolean useWatershed;
    private boolean showThresholdImage;
    private boolean saveThresholdImage;
    private boolean saveThresholdRois;

    private Roi[] nucleiROIs;
    private final Detector detector;
    private boolean useMacro;
    private final boolean showPreprocessedImage;
    private String macroText;


    /**
     *
     * @param image
     * @param spotName
     * @param nameExperiment
     * @param measureCalibration
     * @param resultsDirectory
//     * @param showImage
     */
    public SpotDetector(ImagePlus image, String spotName, String nameExperiment, MeasureCalibration measureCalibration, String resultsDirectory, boolean showPreprocessedImage) {
        detector = new Detector(image, spotName, measureCalibration);
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
        this.nucleiROIs = null;
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
        this.thresholdMethod = thresholdMethod;
        this.minSizeSpot = minSizeSpot;
        this.useWatershed = useWatershed;
        detector.setThresholdParameters(thresholdMethod,false,minSizeSpot);
    }

    public void setThresholdSaving(boolean saveThresholdImage,boolean saveThresholdRois){
        this.saveThresholdImage = saveThresholdImage;
        this.saveThresholdRois=saveThresholdRois;
    }
    public void setPreprocessingMacro(String macroText){
        this.useMacro = true;
        this.macroText = macroText;
    }
    public void setNucleiROIs(Roi[] nucleiROIs) {
        this.nucleiROIs = nucleiROIs;
    }

    public String getNameExperiment() {
        return nameExperiment;
    }

    public String getImageTitle() {
        return image.getTitle();
    }

    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
            thresholdIP = preprocessed.duplicate();
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(thresholdIP);
                thresholdIP.show();
                if (useWatershed)   detector.getWatershed(thresholdIP.getProcessor()).show();
            }
            if (spotByfindMaxima) {
                findMaximaIP = preprocessed.duplicate();
                detector.renameImage(findMaximaIP, "maxima");
                /*Find maxima*/
                findMaxima(findMaximaIP);
            }
            PluginCellProt.closeAllWindows("Preview is done",idsToKeep);
        }
    }

    public boolean prepare(){
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (preprocessed!=null){
            if (showPreprocessedImage){
                preprocessed.show();
            }
            thresholdIP = preprocessed.duplicate();
            if (spotByThreshold) {
                thresholdIP = detector.getThresholdMask(preprocessed);
                if (useWatershed){
                    thresholdIP = detector.getWatershed(thresholdIP.getProcessor());
                }
                if (showThresholdImage){
                    thresholdIP.show();
                }
                if (resultsDirectory != null && saveThresholdImage){
                    if (IJ.saveAsTiff(thresholdIP, resultsDirectory +"\\Results\\Images\\"+thresholdIP.getTitle())){
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " was saved in "+ resultsDirectory+"\\Results\\Images\\");
                    }else {
                        IJ.log("The spot binary mask "+thresholdIP.getTitle() + " could not be saved in  "+ resultsDirectory+"\\Results\\Images\\");
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
                        boolean wasSaved = RoiEncoder.save(roiMaxima, resultsDirectory +"\\Results\\ROI\\" + image.getShortTitle() + "findMaxima_all_roi.roi");
                        if (!wasSaved) {
                            IJ.error("Could not save ROIs");
                        }else {
                            IJ.log("The ROIs of the spot found by find Maxima method of "+image.getTitle() + " were saved in "+ resultsDirectory+"\\Results\\ROI\\" + image.getShortTitle() + "findMaxima_all_roi.roi");
                        }
                    }
                    if (saveMaximaImage){
                        ImagePlus toSave = findMaximaIP.flatten();
                        if(IJ.saveAsTiff(toSave, resultsDirectory +"\\Results\\Images\\"+findMaximaIP.getTitle())){
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " was saved in "+ resultsDirectory+"\\Results\\Images\\"+findMaximaIP.getTitle());
                        } else {
                            IJ.log("The find maxima spots image "+findMaximaIP.getTitle() + " could not be saved in "+ resultsDirectory+"\\Results\\Images\\");
                        }
                    }
                }
            }
            if (resultsDirectory!=null){
                String parameterFilename = resultsDirectory+"\\Results\\Parameters.txt";
                try {
                    FileWriter fileWriter = new FileWriter(parameterFilename,true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.append("\nSPOT ").append(spotName).append(" Parameters");
                    if (spotByfindMaxima){
                        bufferedWriter.append("\nFind by maxima parameters:");
                        bufferedWriter.append("\nProminence :").append(String.valueOf(prominence));
                    } if (spotByThreshold){
                        bufferedWriter.append("\nThresholding parameters : ");
                        bufferedWriter.append("\nThreshold method :").append(thresholdMethod);
                        bufferedWriter.append("\nMinimum spot diameter :").append(String.valueOf(minSizeSpot));
                        bufferedWriter.append("\nThresholding watershed ").append(useWatershed?"yes":"no");
                    }
                    bufferedWriter.close();
                }catch (IOException e){
                    e.printStackTrace();
                    IJ.log("The parameters could not be written.");
                }
            }
            return true;
        }else return false;
    }

    public void analysisPerRegion(int nucleus, ResultsTable resultsTableFinal, String type) {
        if (nucleus==0){
            detector.setLocalisation(type);
        }
        imageToMeasure.setRoi(nucleiROIs[nucleus]);
        ResultsTable rawMesures = new ResultsTable();
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.MEAN +Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
        analyzer.measure();
        detector.setResultsAndRename(rawMesures,resultsTableFinal,0); /*always first line, because analyzer replace line*/
        if (spotByfindMaxima) {
            findMaximaPerRegion(nucleus, resultsTableFinal,type);
        }
        if (spotByThreshold) {
            findThresholdPerRegion(nucleus, resultsTableFinal,type);
        }
    }

    private void findThresholdPerRegion(int region, ResultsTable resultsTableToAdd, String type) {
        RoiManager roiManagerFoci;
        thresholdIP.setRoi(nucleiROIs[region]);
        thresholdIP.getProcessor().invertLut();
        roiManagerFoci = detector.analyzeParticles(thresholdIP);
        if (resultsDirectory!=null&&saveThresholdRois){
            if (roiManagerFoci.save(resultsDirectory +"\\Results\\ROI\\"+ image.getShortTitle() + "_threshold_"+type +(region+1)+"_roi.zip")){
                IJ.log("The ROIs of the "+type + " "+(region+1)+" of the image "+image.getTitle() + " by threshold method were saved in "+ resultsDirectory+"\\Results\\ROIs\\");
            }else {
                IJ.log("The ROIs of the "+type + " "+ (region+1)+" of the image "+image.getTitle() + " by threshold method could not be saved in "+ resultsDirectory+"\\Results\\ROIs\\");
            }
        }
        int numberSpot = roiManagerFoci.getCount();
        ResultsTable resultsTable = new ResultsTable();
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable);
        resultsTableToAdd.addValue(type+"_"+spotName+" threshold nr. spot",numberSpot);
        if (numberSpot > 0) {
            for (int spot = 0; spot < numberSpot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();
            }
            detector.setSummarizedResults(resultsTable,resultsTableToAdd);
        } else {
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (pixel)", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Area (" + measureCalibration.getUnit() + ")", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold Mean", Double.NaN);
            resultsTableToAdd.addValue(type + "_"+ spotName + " threshold RawIntDen", Double.NaN);
        }
    }

    private void findMaximaPerRegion(int region, ResultsTable resultsTableToAdd, String type) {
        findMaximaIP.setRoi(nucleiROIs[region]);
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
            if (useMacro){
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                imageToReturn = WindowManager.getCurrentImage();
                imageToReturn.changes=false;
                imageToReturn.close();
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
        backgroundSubtracter.rollingBallBackground(wobkgIP.getProcessor(), rollingBallSize, false, false, false, true, false); /*Hmm correctCorners ?*/
        return wobkgIP;
    }

    private PointRoi findMaxima(ImagePlus findMaximaIP) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaximaProc = findMaximaIP.getProcessor();
        if (showMaximaImage) {
            findMaximaIP.show();
        }
        Polygon maxima = maximumFinder.getMaxima(findMaximaProc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaximaIP.setRoi(roiMaxima);
        return roiMaxima;
//        findMaxima_IP.setRoi(roi_maxima);
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", /*false,*/ new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022",true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li", 1000, false, true, false,true);
        nucleiDetector.prepare();

        ImagePlus protein = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        SpotDetector spotDetector = new SpotDetector(protein, "CY5", "WT_HU_Ac-2re--cell003", new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022",true);
        spotDetector.setRollingBallSize(10);
        spotDetector.setzStackParameters("Maximum projection");
//        spotDetector.setSpotByfindMaxima(1000);
        spotDetector.setSpotByThreshold("Li", 10, true,true);
        spotDetector.setNucleiROIs(nucleiDetector.getRoiArray());
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
