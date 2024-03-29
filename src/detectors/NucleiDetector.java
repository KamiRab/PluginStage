package detectors;

import helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
/**
 * Author : Camille RABIER
 * Date : 25/03/2022
 * Class for
 * - segmenting and measuring nuclei
 */
public class NucleiDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMeasures;
    private Analyzer analyzer;
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/
    private ImagePlus imageToParticleAnalyze; /*Binary mask that particle analyzer use to count and return the ROIs*/
    private final String nameExperiment; /*Name of experiment (common with spot images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showPreprocessingImage; /*Display or not the images (projection and binary)*/
    private boolean deepLearning; /*use deep learning, if false use thresholding*/
    private int minSizeDLNuclei;
    private String cellposeModel;
    private boolean useWatershed;
    private Roi[] nucleiRois;
    private boolean finalValidation;
    private boolean useMacro;
    private String macroText;
    private boolean excludeOnEdges;
    private boolean saveMask;
    private boolean saveRois;
    private boolean showBinaryImage;
    private int measurements;

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
        rawMeasures = new ResultsTable();
    }

    public void setNucleiAssociatedRois(Roi[] associatedToCellNucleiRois){
        this.nucleiRois=associatedToCellNucleiRois;
        if (associatedToCellNucleiRois.length>0){
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                for (int i = 0; i < associatedToCellNucleiRois.length; i++) {
                    Roi roi = associatedToCellNucleiRois[i];
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
                ImagePlus nucleiLabeledMask = detector.labeledImage(associatedToCellNucleiRois);
                if (IJ.saveAsTiff(nucleiLabeledMask, resultsDirectory + "/Results/Images/AssociatedToCell" + nucleiLabeledMask.getTitle())) {
                    IJ.log("The binary mask " + nucleiLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Results/Images/");
                } else {
                    IJ.log("The binary mask " + nucleiLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
        }else {
            IJ.log("No nuclei were associated to a cell");
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
     * Set parameters for thresholding
     * @param thresholdMethod : method of thresholding
     * @param minSizeNucleus : minimum size of particle to consider
     * @param useWatershed : if true, use watershed method in addition to thresholding
     * @param excludeOnEdges : if true, exclude particles on edge of image
     */
    public void setThresholdMethod(String thresholdMethod,double minSizeNucleus,boolean useWatershed,boolean excludeOnEdges) {
        this.deepLearning = false;
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
    public void setDeepLearning(int minSizeDLNuclei, String cellposeModel, boolean excludeOnEdges) {
        this.deepLearning = true;
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

    /**
     * Preview of segmentation
     * - does the preprocessing:
     * - segmentation either by threshold or by cellpose
     * - show result labeled image
     */
    public void preview(){
//        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            preprocessed.show();
            if (deepLearning){
//            launch cellpose command to obtain mask
                /*cyto channel = 0 for gray*/
                /*nuclei channel = 0 for none*/
                CellposeLauncher cellposeLauncher = new CellposeLauncher(preprocessed,minSizeDLNuclei, cellposeModel, excludeOnEdges);
                cellposeLauncher.analysis();
                ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0,(cellposeLauncher.getCellposeRoiManager().getCount()+10));
                cellposeOutput.updateAndDraw();
            } else {
                thresholding(preprocessed);
            }
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
            if (deepLearning){
                analysisType = "cellpose";
                CellposeLauncher cellposeLauncher = new CellposeLauncher(preprocessed,minSizeDLNuclei, cellposeModel, excludeOnEdges);
                cellposeLauncher.analysis();
                labeledImage = cellposeLauncher.getCellposeMask();
                detector.renameImage(labeledImage,"cellpose");
                roiManagerNuclei = cellposeLauncher.getCellposeRoiManager();
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
                roiManagerNuclei.toFront();
                ImagePlus tempImage = imageToMeasure.duplicate(); /*Need to duplicate, as closing the image nullify the ImageProcessor*/
                if (showBinaryImage){
                    IJ.selectWindow(imageToMeasure.getID());
                }else {
                    tempImage.show();
                }
                roiManagerNuclei.runCommand("Show All");
                new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
                if (!showBinaryImage){
                    tempImage.close();
                }
                labeledImage = detector.labeledImage(roiManagerNuclei.getRoisAsArray());
            }
//            SAVING
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
                for (int i = 0; i < roiManagerNuclei.getCount(); i++) {
                    roiManagerNuclei.rename(i,"Nucleus_"+(i+1));
                }
                if(roiManagerNuclei.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle() + analysisType +"_nucleus_threshold_roi.zip")){
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The nuclei ROIs of "+image.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
            analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures); /*set measurements and image to analyze*/
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
        if (nucleusRoi!=null){ /*TODO verify if necessary. Possible it was only done for precedent version of plugin where all cells were considered (thus those without nucleus) and the nucleus measurements were in the cell ResultsTable*/
            imageToMeasure.setRoi(nucleusRoi);
            analyzer.measure();
            detector.setResultsAndRename(rawMeasures,resultsTableFinal,nucleus,"Nuclei");
        }else {
            imageToMeasure.setRoi((Roi) null);
            analyzer.measure();
            detector.setNullResultsAndRename(rawMeasures,resultsTableFinal,"Nuclei");
        }
    }

    /**
     * If useThreshold, prepare threshold image
     * @param imagePlus : image to segment
     */
    public void thresholding(ImagePlus imagePlus){
//        OBTAIN BINARY MASK OF THRESHOLD IMAGE
        ImagePlus mask_IP = detector.getThresholdMask(imagePlus);

        if (showBinaryImage){
            mask_IP.show();
        }

        if (useWatershed){
            ImagePlus watershed_mask = detector.getWatershed(mask_IP.getProcessor());
            detector.renameImage(watershed_mask,"watershed");
            if (showBinaryImage){
                watershed_mask.show();
            }
            imageToParticleAnalyze = watershed_mask;
        }else {
            imageToParticleAnalyze = mask_IP;
        }
    }

    /**
     * Preprocess image for better segmentation
     * - Projection
     * - Macro
     * @return ImagePlus
     */
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
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);"); /*accelerates the treatment by displaying only the last image*/
                temp = WindowManager.getCurrentImage();
                imageToReturn = temp.duplicate();
                temp.changes=false;
                temp.close();
            }
            return imageToReturn;
        }else return null;
    }

    /**
     *
     * @return name without channel specific information
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    /**
     *
     * @param measurements integer corresponding to addition of measurements to do for nuclei
     */
    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }
}
