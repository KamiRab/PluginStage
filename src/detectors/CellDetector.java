package detectors;

import helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

/**
 * Author : Camille RABIER
 * Date : 25/03/2022
 * Class for
 * - segmenting and measuring cell
 */
public class CellDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMeasures;
    private Analyzer analyzer;
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/

    //General parameters
    private final String nameExperiment; /*Name of experiment (common with spot/nuclei images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private String macroText; /*Macro text if the user wants to treat image*/
    private Roi[] cellRois;
    //    Showing/Saving
    private final boolean showPreprocessedImage; /*Display or not the images (projection and binary)*/
    private final boolean showCompositeImage;
    private boolean saveBinary;
    private boolean saveRois;
    private boolean showBinaryImage;

    //    Parameters for cellpose
    private int minSizeCell; /*minimum size of the cell*/
    private String cellposeModel; /*model to be used by cellpose*/
    private boolean excludeOnEdges; /*exclude the cells on the edge*/

    //    Cytoplasm parameters
    private NucleiDetector nucleiDetector; /*Object associated to nuclei images */
    private double minNucleiCellOverlap;
    private double minCytoSize;
    private int measurements;
    private boolean finalValidation;

// CONSTRUCTOR
    /**
     * Constructor with basic parameters, the other are initialized only if needed
     *
     * @param image              image to analyze
     * @param nameExperiment    name without channel
     * @param resultsDir         : directory for saving results
     * @param showPreprocessedImage          : display or not of the images
     */
    public CellDetector(ImagePlus image, String nameExperiment, String resultsDir, boolean showPreprocessedImage,boolean showBinaryImage, boolean showCompositeImage) {
        this.image = image;
        this.resultsDirectory = resultsDir;
        this.showPreprocessedImage = showPreprocessedImage;
        this.showBinaryImage = showBinaryImage;
        this.showCompositeImage = showCompositeImage;
        if (nameExperiment.endsWith("_")) {
            this.nameExperiment = nameExperiment.substring(0, nameExperiment.length() - 1);
        } else {
            this.nameExperiment = nameExperiment;
        }
        detector = new Detector(image, "Cell");
        nucleiDetector = null;
        rawMeasures = new ResultsTable();
    }


//    SETTER
    /**
     *
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration){
        detector.setMeasureCalibration(measureCalibration);
    }
    /**
     * @param nucleiDetector : Nuclei object that improve the cell detection with cellpose
     */
    public void setNucleiDetector(NucleiDetector nucleiDetector) {
        this.nucleiDetector = nucleiDetector;
    }
    /**
     * Set all parameters for projection if necessary
     *
     * @param zStackProj       Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice  Last slice of stack to use
     */
    public void setZStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     *
     * @param zStackProj Method of projection
     */
    public void setZStackParameters(String zStackProj) {
        setZStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText) {
        this.macroText = macroText;
    }

    /**
     *
     * @param saveMask : boolean to save the labeled mask or not
     * @param saveROIs : boolean to save the ROI obtained after segmentation or not
     */
    public void setSavings(boolean saveMask,boolean saveROIs){
        this.saveBinary = saveMask;
        this.saveRois = saveROIs;
    }

    /**
     * @param minSizeDLCell : minimum size of cell to detect
     * @param cellposeModel : model used by cellpose to segment
     * @param excludeOnEdges : exclude cell on image edges
     */
    public void setDeepLearning(int minSizeDLCell, String cellposeModel, boolean excludeOnEdges, boolean finalValidation, boolean showBinaryImage) {
        this.minSizeCell = minSizeDLCell;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
        this.finalValidation = finalValidation;
        this.showBinaryImage = showBinaryImage;
    }
    public void setCytoplasmParameters(double minOverlap, double minCytoSize){
        this.minNucleiCellOverlap = minOverlap/100;
        this.minCytoSize = minCytoSize/100;
    }

    /**
     * The cell without nuclei are considered as errors, so they are not analyzed
     * --> saves cellRoi
     * --> save new binary mask associated to new Rois
     * @param modifiedCellRois : CellRois that are associated to a nuclei
     */
    public void setCellRois(Roi[]modifiedCellRois){
        cellRois=modifiedCellRois;
        if (cellRois.length>0){
//            Save Rois
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                /*Add to roiManager*/
                for (int i = 0; i < modifiedCellRois.length; i++) {
                    Roi roi = modifiedCellRois[i];
                    if (roi!=null){
                        roi.setName("Cell_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                /*Save RoiManager*/
                if(roiManager.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle()  +"_wholeCellWithNucleusROIs.zip")){
                    IJ.log("The cell ROIs containing a nucleus of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The cell ROIs containing a nucleus of "+image.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
//            Save binary mask
            if (resultsDirectory!=null && saveBinary){
                ImagePlus cellLabeledMask = detector.labeledImage(modifiedCellRois);
                if (IJ.saveAsTiff(cellLabeledMask, resultsDirectory + "/Results/Images/validated" + cellLabeledMask.getTitle())) {
                    IJ.log("The binary mask validated" + cellLabeledMask.getTitle() + " with only cells containing a nucleus was saved in " + resultsDirectory + "/Results/Images/");
                } else {
                    IJ.log("The binary mask validated" + cellLabeledMask.getTitle() + " with only cells containing a nucleus could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
        }else {
            IJ.log("No cells with a nucleus were detected");
        }

    }

//    GETTER

    /**
     * @return CytoDetector associated to CellDetector (cellRois are given)
     */
    public CytoDetector getCytoDetector(){
        return new CytoDetector(imageToMeasure,image.getTitle(),cellRois,resultsDirectory,showBinaryImage, saveBinary,saveRois, minNucleiCellOverlap,minCytoSize);
    }

    /**
     * Segment cell to obtain ROIs for further analysis
     * @return true if no problem occurred
     */
    public boolean prepare() {
//        PREPROCESSING
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            if (showPreprocessedImage){
                preprocessed.show();
            }

//            SEGMENTATION : launch cellpose command to obtain mask
            /*cyto channel : 0=grayscale, 1=red, 2=green, 3=blue*/
            /*nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue*/
            RoiManager roiManagerCell;
            CellposeLauncher cellposeLauncher;
            /*Use nuclei image, if exists, to improve segmentation*/
            /*Cellpose needs in this case a composite image that contains the nuclei and cytoplasm channels*/
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
//                    Create composite
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{imageToMeasure, nucleiDetector.getPreprocessing()}, true);
                    if (showCompositeImage){
                        composite.setTitle(nameExperiment + "_composite");
                        composite.show();
                    }
//                    Create CellposeLauncher objet
                    cellposeLauncher = new CellposeLauncher(composite, minSizeCell, cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return false;
                }
            } else {/*No nuclei channel*/
                cellposeLauncher = new CellposeLauncher(preprocessed, minSizeCell, cellposeModel, excludeOnEdges);
            }
//            Launch Cellpose
            cellposeLauncher.analysis();
//            Get cellpose mask and roiManager
            ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
            detector.renameImage(cellposeOutput, "cellpose_WholeCell");
            roiManagerCell = cellposeLauncher.getCellposeRoiManager();
            if (showBinaryImage) {
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0, (cellposeLauncher.getCellposeRoiManager().getCount() + 10));
                cellposeOutput.updateAndDraw();
            }
//            Allow user to redefine the regions of interest
            if (finalValidation){
                roiManagerCell.toFront();
                ImagePlus tempImage = imageToMeasure.duplicate(); /*Need to duplicate, as closing the image nullify the ImageProcessor*/
                if (showBinaryImage){
                    IJ.selectWindow(imageToMeasure.getID());
                }else {
                    tempImage.show();
                }
                roiManagerCell.runCommand("Show All");
                new WaitForUserDialog("Cell selection","Delete cells : select the ROIs + delete").show();
                if (!showBinaryImage){
                    tempImage.close();
                }
                /*Obtain new ROIs*/
                cellposeOutput = detector.labeledImage(roiManagerCell.getRoisAsArray());
            }
//            SAVINGS
            if (resultsDirectory != null && saveBinary) {
                if (IJ.saveAsTiff(cellposeOutput, resultsDirectory + "/Results/Images/" + cellposeOutput.getTitle())){
                    IJ.log("The binary mask " + cellposeLauncher.getCellposeMask().getTitle() + " was saved in " + resultsDirectory + "/Results/Images/");
                }else {
                    IJ.log("The binary mask " + cellposeLauncher.getCellposeMask().getTitle() + " could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
            if (resultsDirectory != null && saveRois) {
                for (int i = 0; i < roiManagerCell.getCount(); i++) {
                    roiManagerCell.rename(i,"Cell_"+(i+1));
                }
                if (roiManagerCell.save(resultsDirectory + "/Results/ROI/" + image.getShortTitle() + "_wholeCell_cellpose_roi.zip")){
                    IJ.log("The cell ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/Results/ROI/");
                } else {
                    IJ.log("The cell ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/Results/ROI/");
                }
            }
            cellRois = roiManagerCell.getRoisAsArray();
//            Create analyzer for future measurements
            analyzer = new Analyzer(imageToMeasure, measurements, rawMeasures);
            return true;
        } else return false;
    }

    /**
     * Preview of segmentation (similar to prepare without final validation and RoiManager)
     * - does the preprocessing:
     * - does the segmentation by cellpose
     * - show result labeled image
     */
    public void preview() {
//        PREPROCESSING
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            preprocessed.show();
//            SEGMENTATION : launch cellpose command to obtain mask
            /*cyto channel : 0=grayscale, 1=red, 2=green, 3=blue*/
            /*nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue*/
            CellposeLauncher cellposeLauncher;
            /*Use nuclei image, if exists, to improve segmentation*/
            /*Cellpose needs in this case a composite image that contains the nuclei and cytoplasm channels*/
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
                    /*Create composite*/
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{imageToMeasure, nucleiDetector.getPreprocessing()}, true);
                    composite.setTitle(nameExperiment + "_composite");
                    if (showCompositeImage){
                        composite.show();
                    }
//                    Create CellposeLauncher object
                    cellposeLauncher = new CellposeLauncher(composite, minSizeCell, cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return;
                }
            } else { /*no nuclei channel*/
                cellposeLauncher = new CellposeLauncher(preprocessed, minSizeCell, cellposeModel, excludeOnEdges);
            }
//            Launch Cellpose
            cellposeLauncher.analysis();
//            Get Cellpose mask
            ImagePlus cellposeOutput = cellposeLauncher.getCellposeMask();
//            Show cellpose mask
            cellposeOutput.show();
            cellposeOutput.setDisplayRange(0, (cellposeLauncher.getCellposeRoiManager().getCount() + 10));
            cellposeOutput.updateAndDraw();
        }
    }

    /**
     * Preprocess image for segmentation
     * @return ImagePlus or null if preprocessing did not work
     */
    private ImagePlus getPreprocessing() {
        if (detector.getImage() != null) {
            IJ.run("Options...", "iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            this.imageToMeasure = detector.getImage(); /*detector class does the projection if needed*/
            if (showPreprocessedImage) {
                this.imageToMeasure.show(); /*show image that will be measured*/
            }
//      MACRO : apply custom commands of user
            if (macroText!=null) {
                imageToMeasure.show();
                IJ.selectWindow(imageToMeasure.getID());
                IJ.runMacro("setBatchMode(true);" + macroText + "setBatchMode(false);");
                imageToMeasure = WindowManager.getCurrentImage();
                imageToMeasure.changes=false;
                imageToMeasure.close();
            }
            return imageToMeasure.duplicate();
        } else return null;

    }

    /**
     *
     * @return name of experiment
     */
    public String getNameExperiment() {
        return nameExperiment;
    }

    /**
     * @return image title
     */
    public String getImageTitle() {
        return image.getTitle();
    }

    /**
     *
     * @return Array of the cell ROIs
     */
    public Roi[] getRoiArray() {
        return cellRois;
    }

    /**
     *
     * @param cell : number of the cell in the array
     * @param resultsTableFinal : {@link ResultsTable} that will contain the results
     */
    public void measureEachCell(int cell,ResultsTable resultsTableFinal) {
        imageToMeasure.setRoi(cellRois[cell]);
        //    Results
        analyzer.measure();
        detector.setResultsAndRename(rawMeasures,resultsTableFinal,cell,"Cell");
    }

    /**
     *
     * @param measurements : integer corresponding to measurements to do on cells
     */
    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }
}
