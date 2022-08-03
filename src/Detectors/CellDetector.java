package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.File;

//TODO prefs saving/showing
//TODO selection empty and no nucleus
//TODO get nuclei prepare to not have two times
//TODO cellpose final validation to decide if to keep or not cell
//TODO preview cytoplasm ?
public class CellDetector {
    private final ImagePlus image; /*Original image*/
    private final ResultsTable rawMesures;
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
        rawMesures = new ResultsTable();
        /*precise mesures à faire et image sur laquelle faire*/
    }

    public CytoDetector getCytoDetector(){
        return new CytoDetector(imageToMeasure,image.getTitle(),cellRois,resultsDirectory,showBinaryImage, saveBinary,saveRois, minNucleiCellOverlap,minCytoSize);
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
     *
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
    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        detector.setzStackParameters(zStackProj, zStackFirstSlice, zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     *
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     *
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
     *
     *
     * @param minSizeDLCell : minimum size of cell to detect
     * @param cellposeModel : model used by cellpose to segment
     * @param excludeOnEdges : exclude cell on image edges
     */
    public void setDeeplearning(int minSizeDLCell, String cellposeModel, boolean excludeOnEdges, boolean showBinaryImage) {
        this.minSizeCell = minSizeDLCell;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
        this.showBinaryImage = showBinaryImage;
    }

    public void setCytoplasmParameters(double minOverlap, double minCytoSize){
        this.minNucleiCellOverlap = minOverlap/100;
        this.minCytoSize = minCytoSize/100;
    }

    /**
     * TODO
     * @param modifiedCellRois
     */
    public void setCellRois(Roi[]modifiedCellRois){
        cellRois=modifiedCellRois;
        if (cellRois.length>0){
            if (resultsDirectory!=null && saveRois) {
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
                for (int i = 0; i < modifiedCellRois.length; i++) {
                    Roi roi = modifiedCellRois[i];
                    if (roi!=null){
                        roi.setName("Cell_"+(i+1));
                        roiManager.addRoi(roi);
                    }
                }
                if(roiManager.save(resultsDirectory +"/Results/ROI/"+ image.getShortTitle()  +"_wholeCellWithNucleusROIs.zip")){
                    IJ.log("The cell ROIs containing a nucleus of "+image.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The cell ROIs containing a nucleus of "+image.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }
            }
            else if (resultsDirectory==null && saveRois){
                IJ.error("No directory given for the results");
            }
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

    public void preview() {
//        int[] idsToKeep = WindowManager.getIDList();
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            preprocessed.show();
//            launch cellpose command to obtain mask
            /*cyto channel : 0=grayscale, 1=red, 2=green, 3=blue*/
            /*nuclei channel : 0=None (will set to zero), 1=red, 2=green, 3=blue*/
            Cellpose cellpose;
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{imageToMeasure, nucleiDetector.getPreprocessing()}, true);
                    composite.setTitle(nameExperiment + "_composite");
                    if (showCompositeImage){
                        composite.show();
                    }
                    cellpose = new Cellpose(composite, minSizeCell, cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return;
                }
            } else {
                cellpose = new Cellpose(preprocessed, minSizeCell, cellposeModel, excludeOnEdges);
            }
            cellpose.analysis();
            ImagePlus cellposeOutput = cellpose.getCellposeOutput();
            cellposeOutput.show();
            cellposeOutput.setDisplayRange(0, (cellpose.getCellposeRoiManager().getCount() + 10));
            cellposeOutput.updateAndDraw();
//            PluginCellProt.closeAllWindows("Preview is done.", idsToKeep);
        }
    }

    public boolean prepare() {
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed != null) {
            if (showPreprocessedImage){
                preprocessed.show();
            }
            RoiManager roiManagerCell;
            Cellpose cellpose;
            if (nucleiDetector != null) {
                ImagePlus nucleiProcessedImage = nucleiDetector.getPreprocessing();
                if (nucleiProcessedImage != null) {
                    ImagePlus composite = RGBStackMerge.mergeChannels(new ImagePlus[]{imageToMeasure, nucleiDetector.getPreprocessing()}, true);
                    if (showCompositeImage){
                        composite.setTitle(nameExperiment + "_composite");
                        composite.show();
                    }
                    cellpose = new Cellpose(composite, minSizeCell, cellposeModel, 1, 2, excludeOnEdges);
                } else {
                    IJ.error("There is a problem with the nuclei preprocessing, please verify the parameters");
                    return false;
                }
            } else {
                cellpose = new Cellpose(preprocessed, minSizeCell, cellposeModel, excludeOnEdges);
            }cellpose.analysis();
            ImagePlus cellposeOutput = cellpose.getCellposeOutput();
            detector.renameImage(cellposeOutput, "cellpose_WholeCell");
            if (showBinaryImage) {
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0, (cellpose.getCellposeRoiManager().getCount() + 10));
                cellposeOutput.updateAndDraw();
            }
            if (resultsDirectory != null && saveBinary) {
                if (IJ.saveAsTiff(cellposeOutput, resultsDirectory + "/Results/Images/" + cellposeOutput.getTitle())){
                    IJ.log("The binary mask " + cellpose.getCellposeOutput().getTitle() + " was saved in " + resultsDirectory + "/Results/Images/");
                }else {
                    IJ.log("The binary mask " + cellpose.getCellposeOutput().getTitle() + " could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
            roiManagerCell = cellpose.getCellposeRoiManager();
            if (resultsDirectory != null && saveRois) {
                if (roiManagerCell.save(resultsDirectory + "/Results/ROI/" + image.getShortTitle() + "_wholeCell_cellpose_roi.zip")){
                    IJ.log("The cell ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "/Results/ROI/");
                } else {
                    IJ.log("The cell ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "/Results/ROI/");
                }
            }
            cellRois = roiManagerCell.getRoisAsArray();
//            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA + Measurements.INTEGRATED_DENSITY, rawMesures);
            analyzer = new Analyzer(imageToMeasure, measurements, rawMesures);
            return true;
        } else return false;
    }

    /**
     *
     * @return if processing worked
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
//         MEDIAN FILTER : reduce noise
            //        new RankFilters().rank(filteredProjection.getProcessor(),5,RankFilters.MEDIAN);
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
        if (cell==14){
            IJ.log("sus");
        }
        imageToMeasure.setRoi(cellRois[cell]);
        //    Results
        analyzer.measure();
        detector.setResultsAndRename(rawMesures,resultsTableFinal,cell,"Cell");
    }

    /**
     * Tests
     * @param args none
     */
    public static void main(String[] args) {
        ImagePlus cytoImage = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/Cell_02_w21 FITC.TIF");
        new File("C:/Users/Camille/Downloads/Camille_Stage2022/Results/Images").mkdirs();
        new File("C:/Users/Camille/Downloads/Camille_Stage2022/Results/ROI").mkdirs();
        CellDetector cellDetector = new CellDetector(cytoImage, "test", "C:/Users/Camille/Downloads/Camille_Stage2022", true,true,true);
        cellDetector.setzStackParameters("Maximum projection");
        cellDetector.setMeasureCalibration(new MeasureCalibration());
        cellDetector.setDeeplearning(200, "cyto2", true,true);
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/Cell_02_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "test", "C:/Users/Camille/Downloads/Camille_Stage2022", true);
        nucleiDetector.setMeasureCalibration(new MeasureCalibration());
        nucleiDetector.setzStackParameters("Maximum projection");
        cellDetector.setNucleiDetector(nucleiDetector);
        cellDetector.prepare();
    }

    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }
}
