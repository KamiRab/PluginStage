package Detectors;

import Helpers.MeasureCalibration;
import Helpers.ImageToAnalyze;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.EDM;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

//Final validation cellpose TODO
//TODO cellpose exclude on edge
//TODO attention ordre rolling ball + checkbox
public class NucleiDetector {
    private final ImagePlus image; /*Original image*/
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/
    private ImagePlus imageToAnalyze; /*Binary mask that particle analyzer use to count and return the ROIs*/
    private final String name_experiment; /*Name of experiment (common with spot images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showImage; /*Display or not the images (projection and binary)*/
    private boolean deeplearning; /*use deeplearning, if false use thresholding*/
    private int minSizeDLNuclei;
    private String dLMethod;
    private boolean useWatershed; /*TODO*/
    private Roi[] nucleiRois;
    private Analyzer analyzer;
    private ResultsTable rawMesures;
    private boolean finalValidation;
    private boolean useMacro;
    private String macroText;
    private boolean excludeOnEdges;

    //TODO measures
    /**
     * Constructor with basic parameters, the other are initialized only if needed
     * @param image image to analyze
     * @param name_experiment name without channel
     * @param measureCalibration : calibration to use for ResultTable
     * @param resultsDir : directory for saving results
     * @param showImage : display or not of the images
     */
    public NucleiDetector(ImagePlus image, String name_experiment, MeasureCalibration measureCalibration, String resultsDir, boolean showImage) {
        this.image = image;
        this.resultsDirectory =resultsDir;
        this.showImage=showImage;
        if (name_experiment.endsWith("_")){
            this.name_experiment = name_experiment.substring(0,name_experiment.length()-1);
        }else {
            this.name_experiment=name_experiment;
        }
        detector = new Detector(image, "Nucleus", measureCalibration);
    }

    /**
     * Set all parameters for projection if necessary
     * @param zStackProj Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice,int zStackLastSlice){
        detector.setzStackParameters(zStackProj,zStackFirstSlice,zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText){
        this.useMacro = true;
        this.macroText = macroText;
    }


    /**
     * Set parameters for thresholding and
     * @param thresholdMethod method of thresholding
     * @param minSizeNucleus minimum size of particle to consider
     * @param useWatershed if true, use watershed method in addition to thresholding
     * @param excludeOnEdges if true, exclude particles on edge of image
     */
    public void setThresholdMethod(String thresholdMethod,double minSizeNucleus,boolean useWatershed,boolean excludeOnEdges,boolean finalValidation) {
        this.deeplearning = false;
        detector.setThresholdParameters(thresholdMethod,excludeOnEdges,minSizeNucleus);
        this.useWatershed = useWatershed;
        this.finalValidation = finalValidation;
    }

    /**
     * TODO
     * @param minSizeDLNuclei
     * @param dlMethod
     * @param excludeOnEdges
     */
    public void setDeeplearning(int minSizeDLNuclei, String dlMethod,boolean excludeOnEdges) {
        this.deeplearning = true;
        this.minSizeDLNuclei = minSizeDLNuclei;
        this.dLMethod = dlMethod;
        this.excludeOnEdges = excludeOnEdges;
    }

    /**
     *
     * @return Array of the nuclei ROIs
     */
    public Roi[] getRoiArray() {
        return nucleiRois;
    }

    /**
     *
     * @return image
     */
    public String getImageTitle() {
        return image.getTitle();
    }

//    TODO commentaire avec différentes étapes de l'algo
    public void preview(){
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            preprocessed.show();
            if (deeplearning){
//            launch cellpose command to obtain mask
                /*cyto channel = 0 for gray*/
                /*nuclei channel = 0 for none*/
                Cellpose cellpose = new Cellpose(preprocessed,minSizeDLNuclei,dLMethod,excludeOnEdges);
                cellpose.analysis();
                cellpose.getCellposeOutput().show();
                IJ.log("finished");
            } else {
                thresholding(preprocessed);
            }
            new WaitForUserDialog("Preview is done").show();
        }
        int[] ids = WindowManager.getIDList();
        for (int id : ids) {
            ImagePlus image = WindowManager.getImage(id);
            image.changes = false;
            image.close();
        }
    }

    public boolean prepare(){
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            RoiManager roiManagerNuclei;
            if (deeplearning){
                Cellpose cellpose = new Cellpose(preprocessed,minSizeDLNuclei,dLMethod,excludeOnEdges);
                cellpose.analysis();
                if (showImage)  cellpose.getCellposeOutput().show();
                if (resultsDirectory!=null){
                    IJ.save(cellpose.getCellposeOutput(), resultsDirectory +"\\Results\\Images\\"+cellpose.getCellposeOutput().getTitle());
                    IJ.log("The binary mask "+cellpose.getCellposeOutput().getTitle() + " was saved in "+ resultsDirectory+"\\Results\\Images\\");
                }
                roiManagerNuclei =cellpose.label2Roi();
                if (resultsDirectory!=null) {
                    roiManagerNuclei.save(resultsDirectory +"\\Results\\ROI\\"+ ImageToAnalyze.name_without_extension(image.getTitle()) + "_nucleus_cellpose_roi.zip");
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " were saved in "+ resultsDirectory+"\\Results\\ROI\\");
                }
            } else {
                thresholding(preprocessed);
                // Analyse particule
//            this.resultsTableNuclei = new ResultsTable(); /*creation d'une resultTable pour PA*/
                roiManagerNuclei =detector.analyzeParticles(imageToAnalyze);
                if (resultsDirectory!=null) {
                    roiManagerNuclei.save(resultsDirectory +"\\Results\\ROI\\"+ ImageToAnalyze.name_without_extension(image.getTitle()) + "_nucleus_threshold_roi.zip");
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " were saved in "+ resultsDirectory+"\\Results\\ROI\\");
                }
                if (finalValidation){
//                roiManagerNuclei.show(); /*TODO*/
                    roiManagerNuclei.toFront(); /*TODO to verify*/
                    IJ.selectWindow(imageToMeasure.getID());
                    roiManagerNuclei.runCommand("Show All");
                    new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
                }
            }
            this.rawMesures = new ResultsTable();
            nucleiRois = roiManagerNuclei.getRoisAsArray();
            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA+Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
            return true;
        }else return false;
    }

    public void measureEachNuclei(int nucleus,ResultsTable resultsTableFinal) {
        imageToMeasure.setRoi(nucleiRois[nucleus]);
        analyzer.measure();
        detector.setResultsAndRename(rawMesures,resultsTableFinal,nucleus);
    }


//    TODO mesureNuclei par ROI ==> besoin de globaliser analyser et results table
//    TODO Mesure noyau et prot dans meme boucle de Roi
//    TODO preprocess puis boucle ROI


    /**
     *
     * @param binary_threshold_proc : processor of mask, already ByteProcessor
     * @return TODO
     */
    private ImagePlus getWatershed(ImageProcessor binary_threshold_proc) {
//        ByteProcessor binary_threshold_proc = threshold_proc.convertToByteProcessor();
        EDM edm = new EDM();
        edm.toWatershed(binary_threshold_proc);
        return new ImagePlus(image.getTitle()+"_watershed",binary_threshold_proc);
    }

    public void thresholding(ImagePlus imagePlus){
//        OBTAIN BINARY MASK OF THRESHOLDED IMAGE
        ImagePlus mask_IP = detector.getThresholdMask(imagePlus);

        if (mask_IP.isInvertedLut()){
            IJ.log("mask ip");
        }
        if (showImage){
            mask_IP.show();
//            openedImagesID.add(mask_IP.getID());
        }
        if (resultsDirectory !=null){
            ImagePlus labeledImage = detector.labeledImage();
            IJ.save(labeledImage, resultsDirectory +"\\Results\\Images\\"+labeledImage.getTitle());
            IJ.log("The binary mask "+labeledImage.getTitle() + " was saved in "+ resultsDirectory+"\\Results\\Images\\");
        }
//        mask_IP.getProcessor().invertLut();
        IJ.run(mask_IP, "Fill Holes","");
        if (useWatershed){
            ImagePlus watersheded_mask = getWatershed(mask_IP.getProcessor());
            if (showImage){
                watersheded_mask.show();
//                openedImagesID.add(watersheded_mask.getID());
            }
            if (resultsDirectory !=null){
                IJ.save(watersheded_mask, resultsDirectory +"\\Results\\Images\\"+watersheded_mask.getTitle());
                IJ.log("The watershed mask "+watersheded_mask.getTitle() + " was saved in "+ resultsDirectory+"\\Results\\Images\\");
            }
            imageToAnalyze = watersheded_mask;
        }else {
            imageToAnalyze = mask_IP.duplicate();
        }
    }

    private ImagePlus getPreprocessing() {
        if (detector.getImage()!=null){
            IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            this.imageToMeasure = detector.getImage(); /*detector class does the projection if needed*/
            if (showImage){
                this.imageToMeasure.show(); /*show image that will be measured*/
            }
//      MACRO : apply custom commands of user
            if (useMacro){
                IJ.selectWindow(imageToMeasure.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                imageToMeasure = WindowManager.getCurrentImage();
//            imageToMeasure.show();
            }
//         MEDIAN FILTER : reduce noise
            //        new RankFilters().rank(filteredProjection.getProcessor(),5,RankFilters.MEDIAN);
            return imageToMeasure.duplicate();
        }else return null;
    }

    /**
     *
     * @return TODO
     */
    public String getName_experiment() {
        return name_experiment;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003",new MeasureCalibration(),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setDeeplearning(200,"cyto",true);
//        nucleiDetector.setThresholdMethod("Li",1000,false,true,false);
        nucleiDetector.prepare();
    }
}
