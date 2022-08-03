package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
//TODO process interrupted appears too much
//Final results appears after interruption ? TODO
// TODO garder paramétres utilisé dans fichier : aire,moyenne intensité et somme intensité (+ options mesures morphologiques)
//Final validation cellpose TODO
//TODO cellpose exclude on edge
//TODO attention ordre rolling ball + checkbox
public class NucleiDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMesures;
    private Analyzer analyzer;
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/
    private ImagePlus imageToParticleAnalyze; /*Binary mask that particle analyzer use to count and return the ROIs*/
    private final String nameExperiment; /*Name of experiment (common with spot images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showPreprocessingImage; /*Display or not the images (projection and binary)*/
    private boolean deeplearning; /*use deeplearning, if false use thresholding*/
    private int minSizeDLNuclei;
    private String cellposeModel;
    private boolean useWatershed;
    private Roi[] nucleiRois;
    //    private ResultsTable rawMesures;
    private boolean finalValidation;
    private boolean useMacro;
    private String macroText;
    private boolean excludeOnEdges;
    private boolean saveMask;
    private boolean saveRois;
    private boolean showBinaryImage;
    private int measurements;

    //TODO measures
    /**
     * Constructor with basic parameters, the other are initialized only if needed
     * @param image image to analyze
     * @param nameExperiment name without channel
     * @param resultsDir : directory for saving results
     * @param showPreprocessingImage : display or not of the images
     */
    public NucleiDetector(ImagePlus image, String nameExperiment, String resultsDir, boolean showPreprocessingImage) {
        this.image = image;
        this.resultsDirectory =resultsDir;
        this.showPreprocessingImage =showPreprocessingImage;
        if (nameExperiment.endsWith("_")){
            this.nameExperiment = nameExperiment.substring(0,nameExperiment.length()-1);
        }else {
            this.nameExperiment =nameExperiment;
        }
        detector = new Detector(image, "Nucleus");
        rawMesures = new ResultsTable();
        /*precise mesures à faire et image sur laquelle faire*/

    }

    public void setNucleiAssociatedRois(Roi[] associatedToCellNucleisRois){
        this.nucleiRois=associatedToCellNucleisRois;
        if (associatedToCellNucleisRois.length>0){
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                for (int i = 0; i < associatedToCellNucleisRois.length; i++) {
                    Roi roi = associatedToCellNucleisRois[i];
                    if (roi!=null){
                        roi.setName("Nuclei_Cell_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                if(roiManager.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle()  +"_nucleus_associatedToCell_roi.zip")){
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
            if (resultsDirectory!=null && saveMask){
                ImagePlus nucleiLabeledMask = detector.labeledImage(associatedToCellNucleisRois);
                if (IJ.saveAsTiff(nucleiLabeledMask, resultsDirectory + "/Results/Images/AssociatedToCell" + nucleiLabeledMask.getTitle())) {
                    IJ.log("The binary mask " + nucleiLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Results/Images/");
                } else {
                    IJ.log("The binary mask " + nucleiLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
        }else {
            IJ.log("No nucleis were associated to a cell");
        }
    }

    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
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
     *
     * @param saveMask : boolean to save the labeled mask or not
     * @param saveROIs : boolean to save the ROI obtained after segmentation or not
     */
    public void setSavings(boolean saveMask,boolean saveROIs){
        this.saveMask = saveMask;
        this.saveRois = saveROIs;
    }

    /**
     * Set parameters for thresholding and
     * @param thresholdMethod : method of thresholding
     * @param minSizeNucleus : minimum size of particle to consider
     * @param useWatershed : if true, use watershed method in addition to thresholding
     * @param excludeOnEdges : if true, exclude particles on edge of image

     */
    public void setThresholdMethod(String thresholdMethod,double minSizeNucleus,boolean useWatershed,boolean excludeOnEdges) {
        this.deeplearning = false;
        detector.setThresholdParameters(thresholdMethod,excludeOnEdges,minSizeNucleus);
        this.useWatershed = useWatershed;
        this.excludeOnEdges = excludeOnEdges;
    }

    /**
     *
     * @param minSizeDLNuclei : minimum size of nucleus to detect
     * @param cellposeModel : model used by cellpose to segment
     * @param excludeOnEdges : exclude nuclei on image edges
     */
    public void setDeeplearning(int minSizeDLNuclei, String cellposeModel,boolean excludeOnEdges) {
        this.deeplearning = true;
        this.minSizeDLNuclei = minSizeDLNuclei;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
    }

    /**
     * Set common parameters for cellpose and threshold method
     * @param finalValidation : let the user redefine the ROI found automatically
     * @param showBinaryImage : show result image if true
     */
    public void setSegmentation(boolean finalValidation, boolean showBinaryImage){
        this.finalValidation = finalValidation;
        this.showBinaryImage = showBinaryImage;
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

    /**
     * Preview of segmentation
     * - does the preprocessing:
     * - either by threshold or by cellpose
     * - show result labeled image
     */
    public void preview(){
//        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            preprocessed.show();
            if (deeplearning){
//            launch cellpose command to obtain mask
                /*cyto channel = 0 for gray*/
                /*nuclei channel = 0 for none*/
                Cellpose cellpose = new Cellpose(preprocessed,minSizeDLNuclei, cellposeModel, excludeOnEdges);
                cellpose.analysis();
                ImagePlus cellposeOutput = cellpose.getCellposeOutput();
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0,(cellpose.getCellposeRoiManager().getCount()+10));
                cellposeOutput.updateAndDraw();
            } else {
                thresholding(preprocessed);
            }
//            PluginCellProt.closeAllWindows("Preview is done",idsToKeep);
        }
    }

    /**
     * Prepare for measurement
     * - does the preprocessing
     * - if selected by user show preprocessing image
     * - segment image
     * - if selected by user, the user can delete/modify ROIs
     * - if selected by user show segmentation image
     * - if selected by user save segmentation image and ROIs
     * @return true if no error
     */
    public boolean prepare(){
//        PREPROCESSING

        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){ /*if no error during preprocessing*/
            if (showPreprocessingImage){
                preprocessed.show();
            }
            RoiManager roiManagerNuclei;
            ImagePlus labeledImage;
            String analysisType;
//            SEGMENTATION
            if (deeplearning){
                analysisType = "cellpose";
                Cellpose cellpose = new Cellpose(preprocessed,minSizeDLNuclei, cellposeModel, excludeOnEdges);
                cellpose.analysis();
                labeledImage = cellpose.getCellposeOutput();
                detector.renameImage(labeledImage,"cellpose");
                roiManagerNuclei =cellpose.getCellposeRoiManager();
            } else {
                analysisType = "threshold";
                thresholding(preprocessed);
                // Analyse particle
                roiManagerNuclei =detector.analyzeParticles(imageToParticleAnalyze);
                labeledImage = detector.labeledImage(roiManagerNuclei.getRoisAsArray());
            }
            if (showBinaryImage){
                labeledImage.show();
                labeledImage.setDisplayRange(0,roiManagerNuclei.getCount()+5);
                labeledImage.updateAndDraw();
            }
//            User can redefine ROIs if option selected
            if (finalValidation){
                roiManagerNuclei.toFront(); /*TODO to verify*/
                if (showBinaryImage){
                    IJ.selectWindow(imageToMeasure.getID());
                }else {
                    imageToMeasure.show();
                }
                roiManagerNuclei.runCommand("Show All");
                new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
                if (!showBinaryImage){
                    imageToMeasure.close();
                }
                labeledImage = detector.labeledImage(roiManagerNuclei.getRoisAsArray());
            }
            if (resultsDirectory !=null && saveMask){
                detector.renameImage(labeledImage,analysisType+"_labeledMask");
                if(IJ.saveAsTiff(labeledImage, resultsDirectory +"/Results/Images/"+labeledImage.getTitle())){
                    IJ.log("The segmentation mask "+labeledImage.getTitle() + " was saved in "+ resultsDirectory+"/Results/Images/");
                } else {
                    IJ.log("The segmentation mask "+labeledImage.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/Images/");
                }
            }else if (resultsDirectory==null && saveMask){
                IJ.error("No directory given for the results");
            }

            if (resultsDirectory!=null && saveRois) {
                if(roiManagerNuclei.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle() + analysisType +"_nucleus_threshold_roi.zip")){
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
//            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA + Measurements.INTEGRATED_DENSITY, rawMesures);
            analyzer = new Analyzer(imageToMeasure, measurements, rawMesures);
            nucleiRois = roiManagerNuclei.getRoisAsArray();
            return true;
        }else return false;
    }

    /**
     * Do the measurement for each nucleus and add them to result table
     * @param nucleus : index of the nucleus to analyze
     * @param resultsTableFinal : resultTable to fill
     */
    public void measureEachNuclei(int nucleus,ResultsTable resultsTableFinal, Roi nucleusRoi) {
//        if (nucleiRois[nucleus]!=null){
        if (nucleusRoi!=null){
//            imageToMeasure.setRoi(nucleiRois[nucleus]);
            imageToMeasure.setRoi(nucleusRoi);
            analyzer.measure();
            detector.setResultsAndRename(rawMesures,resultsTableFinal,nucleus,"Nuclei");
        }else {
            imageToMeasure.setRoi((Roi) null);
            analyzer.measure();
            detector.setNullResultsAndRename(rawMesures,resultsTableFinal,"Nuclei");
        }
    }

    /**
     * If useThreshold, prepare threshold image
     * @param imagePlus
     */
    public void thresholding(ImagePlus imagePlus){
//        OBTAIN BINARY MASK OF THRESHOLDED IMAGE
        ImagePlus mask_IP = detector.getThresholdMask(imagePlus);

        if (showBinaryImage){
            mask_IP.show();
        }

//        IJ.run(mask_IP, "Fill Holes","");
        if (useWatershed){
            ImagePlus watersheded_mask = detector.getWatershed(mask_IP.getProcessor());
            detector.renameImage(watersheded_mask,"watershed");
            if (showBinaryImage){
                watersheded_mask.show();
            }
            imageToParticleAnalyze = watersheded_mask;
        }else {
            imageToParticleAnalyze = mask_IP;
        }
    }

    protected ImagePlus getPreprocessing() {
        if (detector.getImage()!=null){
            IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            imageToMeasure = detector.getImage();
            ImagePlus imageToReturn = detector.getImage().duplicate(); /*detector class does the projection if needed*/
            ImagePlus temp;
//      MACRO : apply custom commands of user
            if (useMacro){
                imageToReturn.show();
                IJ.selectWindow(imageToReturn.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
//         MEDIAN FILTER : reduce noise
            //        new RankFilters().rank(filteredProjection.getProcessor(),5,RankFilters.MEDIAN);
            return imageToReturn;
        }else return null;
    }

    /**
     *
     * @return TODO
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003","C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setMeasureCalibration(new MeasureCalibration());
        nucleiDetector.setSegmentation(false,true);
        nucleiDetector.setDeeplearning(200,"cyto",true);
//        nucleiDetector.setThresholdMethod("Li",1000,false,true,false);
        nucleiDetector.prepare();
    }

    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }
}
