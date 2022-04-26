package Detectors;

import Helpers.Calibration;
import Helpers.ImageToAnalyze;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.RoiEncoder;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.WindowOrganizer;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;
//TODO test if stack
//TODO ROlling ball checkbox
//TODO Measures
//TODO exclude on edges ?
public class ProteinDetector {
    private final ImagePlus image;
    private ImagePlus imageToMeasure;
    private final String name_experiment;
    private final String protein_name;
    private final Calibration calibration;
    private boolean zStack = false;
    private final String directory;

    private final double rollingBallSize;

    private ImagePlus findMaximaIP;
    private boolean spotByfindMaxima;
    private double prominence;

    private ImagePlus thresholdIP;
    private boolean spotByThreshold;

    private Roi[] nucleiROIs;
    private final Detector detector;
    private boolean useMacro;
    private final boolean showImage;
    private String macroText;


    public ProteinDetector(ImagePlus image, String protein_name, String name_experiment, boolean zStack, String zStackProj, int zStackFirstSlice, int zStackLastSlice, double rollingBallSize, boolean spotByfindMaxima, boolean spotByThreshold, double prominence, String thresholdMethod, double minSizeSpot, Roi[] roiManager_nuclei, /*boolean isPreview,*/ Calibration calibration, String fromDir, boolean showImage) {
        this(image, protein_name, name_experiment, rollingBallSize, /*isPreview,*/ calibration, fromDir,showImage);
        if (zStack) {
            setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
        }
        if (spotByfindMaxima) {
            setSpotByfindMaxima(prominence);
        } else {
            this.spotByfindMaxima = false;
        }

        if (spotByThreshold) {
            setSpotByThreshold(thresholdMethod, minSizeSpot);
        } else {
            this.spotByThreshold = false;
        }
        this.nucleiROIs = roiManager_nuclei;
    }

    public ProteinDetector(ImagePlus image, String protein_name, String name_experiment, double rollingBallSize,/* boolean isPreview,*/ Calibration calibration, String directory,boolean showImage) {
        detector = new Detector(image, protein_name, calibration);
        this.directory = directory;
        this.showImage = showImage;
        this.image = image;
        this.protein_name = protein_name;
        if (name_experiment.endsWith("_")) {
            this.name_experiment = name_experiment.substring(0, name_experiment.length() - 1);
        } else {
            this.name_experiment = name_experiment;
        }
        this.rollingBallSize = rollingBallSize;
        this.spotByThreshold = false;
        this.spotByfindMaxima = false;
//        this.isPreview = isPreview;
        this.nucleiROIs = null;
        this.calibration = calibration;

    }


    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        this.zStack = true;
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    public void setSpotByfindMaxima(double prominence) {
        this.spotByfindMaxima = true;
        this.prominence = prominence;
    }

    public void setSpotByThreshold(String thresholdMethod, double minSizeSpot) {
        this.spotByThreshold = true;
        detector.setThresholdParameters(thresholdMethod,false,minSizeSpot);
    }
    public void setPreprocessingMacro(String macroText){
        this.useMacro = true;
        this.macroText = macroText;
    }
    public void setNucleiROIs(Roi[] nucleiROIs) {
        this.nucleiROIs = nucleiROIs;
    }

    public String getName_experiment() {
        return name_experiment;
    }

    public ImagePlus getImage() {
        return image;
    }

    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (showImage){
            preprocessed.show();
        }
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed);
            if (directory!=null){
                IJ.save(thresholdIP,directory+"\\Results\\Images\\"+thresholdIP.getTitle());
            }
            if (showImage){
                thresholdIP.show();
            }
        }
        if (spotByfindMaxima) {
            findMaximaIP = preprocessed.duplicate();
            detector.rename_image(findMaximaIP, "maxima");
            /*Find maxima*/
            findMaxima(findMaximaIP);
        }
        new WaitForUserDialog("Preview is done").show();
        int[] ids = WindowManager.getIDList();
        for (int id : ids) {
            ImagePlus image = WindowManager.getImage(id);
            image.changes = false;
            image.close();
        }
    }

    public void prepare(){
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (showImage){
            preprocessed.show();
        }
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed);
            if (directory!= null){
                IJ.save(thresholdIP,directory+"\\Results\\Images\\"+thresholdIP.getTitle());
            }
        }
        findMaximaIP = preprocessed.duplicate();
        detector.rename_image(findMaximaIP, "maxima");
        if (directory != null) {
            findMaximaIP.setRoi((Roi) null);
            PointRoi roiMaxima = findMaxima(findMaximaIP);
            findMaximaIP.setRoi(roiMaxima);
            boolean wasSaved = RoiEncoder.save(roiMaxima, directory+"\\Results\\ROI\\" + ImageToAnalyze.name_without_extension(image.getTitle()) + "_all_roi.roi");
            if (!wasSaved) {
                IJ.error("Could not save ROIs");
            }
            ImagePlus toSave = findMaximaIP.flatten();
            IJ.save(toSave, directory+"\\Results\\Images\\"+findMaximaIP.getTitle());
        }
    }
    public void run() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        if (showImage){
            preprocessed.show();
        }
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed);
        }
        ResultsTable test = new ResultsTable();
        int number_nuclei = nucleiROIs.length;
        findMaximaIP = preprocessed.duplicate();
        detector.rename_image(findMaximaIP, "maxima");
        for (int nucleus = 0; nucleus < number_nuclei; nucleus++) {
            analysisPerNucleus(nucleus, test);
            test.incrementCounter();
        }
//        test.show("Final proteins");
    }

    public void analysisPerNucleus(int nucleus, ResultsTable resultsTableFinal) {
        if (spotByfindMaxima) {
            findMaximaPerNucleus(nucleus, resultsTableFinal);
        }
        if (spotByThreshold) {
            findThresholdPerNucleus(nucleus, resultsTableFinal);
        }
    }

    private void findThresholdPerNucleus(int nucleus, ResultsTable resultsTableToAdd) {
        int number_spot;
        RoiManager roiManagerFoci;
        thresholdIP.setRoi(nucleiROIs[nucleus]);
        thresholdIP.getProcessor().invertLut();
        roiManagerFoci = detector.analyzeParticles(thresholdIP);
        roiManagerFoci.save(directory +"\\Results\\ROI\\"+ ImageToAnalyze.name_without_extension(image.getTitle()) + "_threshold_"+(nucleus+1)+"_roi.zip");
        number_spot = roiManagerFoci.getCount();
        ResultsTable resultsTable = new ResultsTable();
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable);
        resultsTableToAdd.addValue(protein_name + " threshold nr. spot", number_spot);
        if (number_spot > 0) {
            for (int spot = 0; spot < number_spot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();
            }
            detector.setSummarizedResults(resultsTable,resultsTableToAdd);
        } else {
            resultsTableToAdd.addValue(protein_name + " threshold Area (pixel)", Double.NaN);
            resultsTableToAdd.addValue(protein_name + " threshold Area (" + calibration.getUnit() + ")", Double.NaN);
            resultsTableToAdd.addValue(protein_name + " threshold Mean", Double.NaN);
            resultsTableToAdd.addValue(protein_name + " threshold Mean from Raw", Double.NaN);
            resultsTableToAdd.addValue(protein_name + " threshold RawIntDen", Double.NaN);
        }
    }

    private void findMaximaPerNucleus(int nucleus, ResultsTable resultsTableToAdd) {
        findMaximaIP.setRoi(nucleiROIs[nucleus]);
//                    Find maxima
        PointRoi roiMaxima = findMaxima(findMaximaIP);
//                    Get statistics
        if (directory != null) {
            boolean wasSaved = RoiEncoder.save(roiMaxima, directory +"\\Results\\ROI\\"+ ImageToAnalyze.name_without_extension(image.getTitle()) + "_" + (nucleus+1) + "_roi.roi");
            if (!wasSaved) {
                IJ.error("Could not save ROIs");
            }
        }
        int size = 0;
        float mean = 0;
        for (Point p : roiMaxima) {
            size++;
            mean += findMaximaIP.getProcessor().getPixelValue(p.x, p.y);
        }
        mean = mean / size;
        resultsTableToAdd.addValue(protein_name + " maxima nr. spots", size);
        resultsTableToAdd.addValue(protein_name + " maxima mean", mean);
    }

    private ImagePlus preprocessing() {
        IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
        if (zStack) {
            imageToMeasure = detector.getImage();
        } else {
            imageToMeasure = image.duplicate();
        }
        if (showImage){
            imageToMeasure.show(); /*show image that will be measured*/
        }
        if (useMacro){
            imageToMeasure.show();
            IJ.selectWindow(imageToMeasure.getID());
            IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false)");
            imageToMeasure = WindowManager.getCurrentImage();
//            imageToMeasure.show();
        }
        return getSubstractBackground();
    }

    private ImagePlus getSubstractBackground() {
        ImagePlus wobkg_IP = imageToMeasure.duplicate();
        detector.rename_image(wobkg_IP, "withoutBackground");
        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkg_IP.getProcessor(), rollingBallSize, false, false, false, true, false); /*Hmm correctCorners ?*/
        return wobkg_IP;
    }

    private PointRoi findMaxima(ImagePlus findMaxima_IP) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaxima_proc = findMaxima_IP.getProcessor();
        if (showImage) {
            findMaxima_IP.show();
        }
        Polygon maxima = maximumFinder.getMaxima(findMaxima_proc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaxima_IP.setRoi(roiMaxima);
        return roiMaxima;
//        findMaxima_IP.setRoi(roi_maxima);
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", /*false,*/ new Calibration(), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li", 1000, false, true, false);
        nucleiDetector.run();

        ImagePlus protein = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ProteinDetector proteinDetector = new ProteinDetector(protein, "CY5", "WT_HU_Ac-2re--cell003", 10, /*false,*/ new Calibration(), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        proteinDetector.setzStackParameters("Maximum projection");
        proteinDetector.setSpotByfindMaxima(1000);
        proteinDetector.setSpotByThreshold("Li", 10);
        proteinDetector.setNucleiROIs(nucleiDetector.getRoiArray());
        proteinDetector.run();
        new WindowOrganizer().run("tile");
    }
}
