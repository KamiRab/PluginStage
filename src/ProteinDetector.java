import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
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
    private String thresholdMethod;
    private double minSizeSpot;

    private Roi[] nucleiROIs;
//    private final boolean isPreview;
//    private ResultsTable resultsTablefinal;
    private final Detector detector;
    private boolean useMacro;
    private String macroText;
    private final ResultsTable resultsTable2=new ResultsTable();

    public ProteinDetector(ImagePlus image, String protein_name, String name_experiment, boolean zStack, String zStackProj, int zStackFirstSlice, int zStackLastSlice, double rollingBallSize, boolean spotByfindMaxima, boolean spotByThreshold, double prominence, String thresholdMethod, double minSizeSpot, Roi[] roiManager_nuclei, /*boolean isPreview,*/ Calibration calibration, String fromDir) {
        this(image, protein_name, name_experiment, rollingBallSize, /*isPreview,*/ calibration, fromDir);
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

    public ProteinDetector(ImagePlus image, String protein_name, String name_experiment, double rollingBallSize,/* boolean isPreview,*/ Calibration calibration, String directory) {
        detector = new Detector(image, name_experiment, protein_name, calibration);
        this.directory = directory;
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
        this.thresholdMethod = thresholdMethod + " dark";
        this.minSizeSpot = minSizeSpot;
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

//    public LinkedHashMap<String, Variable[]> getResultTableMap() {
//        LinkedHashMap<String, Variable[]> results = new LinkedHashMap<>();
//        for (String heading : this.resultsTablefinal.getHeadings()) {
//            results.put(heading, this.resultsTablefinal.getColumnAsVariables(heading));
//        }
//        return results;
//    }

    public void preview() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        preprocessed.show();
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed, thresholdMethod, minSizeSpot);
        }
        if (spotByfindMaxima) {
            findMaximaIP = preprocessed.duplicate();
            detector.rename_image(findMaximaIP, "maxima");
            /*Find maxima*/
            findMaxima(findMaximaIP);
        }
    }

    public void prepare(){
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        preprocessed.show();
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed, thresholdMethod, minSizeSpot);
        }
        findMaximaIP = preprocessed.duplicate();
        detector.rename_image(findMaximaIP, "maxima");
        if (directory != null) {
            findMaximaIP.setRoi((Roi) null);
            PointRoi roiMaxima = findMaxima(findMaximaIP);
            findMaximaIP.setRoi(roiMaxima);
            boolean wasSaved = RoiEncoder.save(roiMaxima, directory + protein_name + "_all_roi.roi");
            if (!wasSaved) {
                IJ.error("Could not save ROIs");
            }
            ImagePlus toSave = findMaximaIP.flatten();
            IJ.save(toSave, directory+"findMaxima" + protein_name + ".tif");
        }
    }
    public void run() {
//        PREPROCESSING : PROJECTION, PRE-TREATMENT (substractbkg, macro)....
        ImagePlus preprocessed;
        preprocessed = preprocessing();
        preprocessed.show();
        thresholdIP = preprocessed.duplicate();
        if (spotByThreshold) {
            thresholdIP = detector.getThresholdMask(preprocessed, thresholdMethod, minSizeSpot);
        }
        ResultsTable test = new ResultsTable();
        int number_nuclei = nucleiROIs.length;
        findMaximaIP = preprocessed.duplicate();
        detector.rename_image(findMaximaIP, "maxima");
        for (int nucleus = 0; nucleus < number_nuclei; nucleus++) {
            analysisPerNucleus(nucleus, test);
            test.incrementCounter();
        }
        test.show("Final proteins");
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
        roiManagerFoci = detector.analyzeParticles(thresholdIP, false, minSizeSpot);
        number_spot = roiManagerFoci.getCount();
        ResultsTable resultsTable = new ResultsTable();
        Analyzer analyzer = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable);
        Analyzer analyzer2 = new Analyzer(imageToMeasure, Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY, resultsTable2);
        double[] thresholdAreaArray;
        double[] thresholdMeanArray;
        double[] thresholdRawIntDenArray;
        double sum_area = 0;
        double sum_area_calibrated = 0;
        double mean_intensity = 0;
//        double mean_intensity2 = 0;
        double sumRawIntDen = 0;
        resultsTableToAdd.addValue(protein_name + " threshold nr. spot", number_spot);
        if (number_spot > 0) {
//            roiManagerFoci.runCommand(imageToMeasure,"Select all");
//            roiManagerFoci.runCommand(imageToMeasure,"Combine");
//            analyzer2.measure();

            for (int spot = 0; spot < number_spot; spot++) {
                roiManagerFoci.select(imageToMeasure, spot);
                analyzer.measure();
            }
//            thresholdAreaArray = resultsTable.getColumn("Area");
//            thresholdMeanArray = resultsTable.getColumn("Mean");
//            thresholdRawIntDenArray = resultsTable.getColumn("RawIntDen");
//
//            for (int spot = 0; spot < number_spot; spot++) {
//                sum_area += thresholdAreaArray[spot];
//                sum_area_calibrated += thresholdAreaArray[spot] * this.calibration.getValue();
//                mean_intensity += thresholdMeanArray[spot];
//                sumRawIntDen += thresholdRawIntDenArray[spot];
////                mean_intensity2 += thresholdRawIntDenArray[spot]/thresholdAreaArray[spot];
//            }
////            mean_intensity = sumRawIntDen/sum_area;
//            mean_intensity = mean_intensity / number_spot;
////            mean_intensity2 = mean_intensity2 / number_spot;
//            resultsTableToAdd.addValue(protein_name + " threshold Area (pixel)", sum_area);
//            resultsTableToAdd.addValue(protein_name + " threshold Area (" + calibration.getUnit() + ")", sum_area_calibrated);
//            resultsTableToAdd.addValue(protein_name + " threshold Mean of Mean", mean_intensity);
//            resultsTableToAdd.addValue(protein_name + " threshold General Mean", sumRawIntDen/sum_area);
////            resultsTableToAdd.addValue(protein_name + " threshold Mean from me", mean_intensity2);
//            resultsTableToAdd.addValue(protein_name + " threshold RawIntDen", sumRawIntDen);
            detector.setResultsThresholdSpot(resultsTable,resultsTableToAdd);
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
            boolean wasSaved = RoiEncoder.save(roiMaxima, directory + protein_name + "_" + nucleus + "_roi.roi");
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
            this.imageToMeasure = detector.getImage();
        } else {
            this.imageToMeasure = image.duplicate();
        }
        this.imageToMeasure.show(); /*show image that will be measured*/
        if (useMacro){
            IJ.selectWindow(imageToMeasure.getID());
            IJ.runMacro(macroText);
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

//    private void findThreshold(ImagePlus threshold_IP) {
//        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*recupere processor alias image*/
//        threshold_proc.setAutoThreshold(thresholdMethod);
//        threshold_proc.threshold((int) threshold_proc.getMinThreshold());
//        threshold_IP.show();/*crée masque binaire avec thr*/
//    }

    private PointRoi findMaxima(ImagePlus findMaxima_IP) {
        MaximumFinder maximumFinder = new MaximumFinder();
        ImageProcessor findMaxima_proc = findMaxima_IP.getProcessor();
//                ImageProcessor wobkg_proc = wobkg_IP.getProcessor();
//                ImageProcessor findMaxima_proc = wobkg_proc.duplicate();
//                ImagePlus findMaxima_IP= new ImagePlus("Maxima",findMaxima_proc);
        findMaxima_IP.show();
        Polygon maxima = maximumFinder.getMaxima(findMaxima_proc, prominence, true);
        PointRoi roiMaxima = new PointRoi(maxima);
        findMaxima_IP.setRoi(roiMaxima);
        return roiMaxima;
//        findMaxima_IP.setRoi(roi_maxima);
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", /*false,*/ new Calibration("No calibration", 1.0, "pix"), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li", 1000, false, true, false);
        nucleiDetector.run();

        ImagePlus protein = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ProteinDetector proteinDetector = new ProteinDetector(protein, "CY5", "WT_HU_Ac-2re--cell003", 10, /*false,*/ new Calibration("None", 1.0, "pix"), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        proteinDetector.setzStackParameters("Maximum projection");
        proteinDetector.setSpotByfindMaxima(1000);
        proteinDetector.setSpotByThreshold("Li", 10);
        proteinDetector.setNucleiROIs(nucleiDetector.getRoiArray());
        proteinDetector.run();
        new WindowOrganizer().run("tile");
    }
}
