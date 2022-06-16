package Detectors;

import GUI.PluginCellProt;
import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.io.File;


// TODO Cellpose gives entire cell, so for cytoplasm needs to remove nuclei
//TODO cellpose final validation to decide if to keep or not cell
public class CellDetector {
    private final ImagePlus image; /*Original image*/
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/

    //General parameters
    private final String name_experiment; /*Name of experiment (common with spot/nuclei images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showPreprocessedImage; /*Display or not the images (projection and binary)*/
    private NucleiDetector nucleiDetector; /*Object associated to nuclei images, */

    private boolean useMacro;
    private String macroText;
    //    Parameters for cellpose
    private int minSizeCell;
    private String cellposeModel;
    private boolean excludeOnEdges;

    private Analyzer analyzer;
    //    Results
    private ImagePlus cellposeOutput;
    private Roi[] cellRois;
    private ResultsTable rawMesures;
    private boolean saveMask;
    private boolean saveRois;
    private boolean showBinaryImage;

// CONSTRUCTOR
    /**
     * Constructor with basic parameters, the other are initialized only if needed
     *
     * @param image              image to analyze
     * @param name_experiment    name without channel
     * @param measureCalibration : calibration to use for ResultTable
     * @param resultsDir         : directory for saving results
     * @param showPreprocessedImage          : display or not of the images
     */
    public CellDetector(ImagePlus image, String name_experiment, MeasureCalibration measureCalibration, String resultsDir, boolean showPreprocessedImage) {
        this.image = image;
        this.resultsDirectory = resultsDir;

        this.showPreprocessedImage = showPreprocessedImage;
        if (name_experiment.endsWith("_")) {
            this.name_experiment = name_experiment.substring(0, name_experiment.length() - 1);
        } else {
            this.name_experiment = name_experiment;
        }
        detector = new Detector(image, "WholeCell", measureCalibration); /*TODO cell or cytoplasm ?*/
        nucleiDetector = null;
    }

//    SETTER
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
     * TODO
     *
     * @param minSizeDLNuclei
     * @param cellposeModel
     * @param excludeOnEdges
     */
    public void setDeeplearning(int minSizeDLNuclei, String cellposeModel, boolean excludeOnEdges, boolean showBinaryImage) {
        this.minSizeCell = minSizeDLNuclei;
        this.cellposeModel = cellposeModel;
        this.excludeOnEdges = excludeOnEdges;
        this.showBinaryImage = showBinaryImage;
    }

    public void preview() {
        int[] idsToKeep = WindowManager.getIDList();
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
                    composite.setTitle(name_experiment + "_composite");
                    composite.show();
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
            PluginCellProt.closeAllWindows("Preview is done.", idsToKeep);
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
                    if (showBinaryImage){
                        composite.setTitle(name_experiment + "_composite");
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
            cellposeOutput = cellpose.getCellposeOutput();
            detector.renameImage(cellposeOutput, "cellpose_WholeCell");
            if (showBinaryImage) {
                cellposeOutput.show();
                cellposeOutput.setDisplayRange(0, (cellpose.getCellposeRoiManager().getCount() + 10));
                cellposeOutput.updateAndDraw();
            }
            if (resultsDirectory != null && saveMask) {
                if (IJ.saveAsTiff(cellposeOutput, resultsDirectory + "\\Results\\Images\\" + cellposeOutput.getTitle())){
                    IJ.log("The binary mask " + cellpose.getCellposeOutput().getTitle() + " was saved in " + resultsDirectory + "\\Results\\Images\\");
                }else {
                    IJ.log("The binary mask " + cellpose.getCellposeOutput().getTitle() + " could not be saved in " + resultsDirectory + "\\Results\\Images\\");
                }
            }
            roiManagerCell = cellpose.getCellposeRoiManager();
            if (resultsDirectory != null && saveRois) {
                if (roiManagerCell.save(resultsDirectory + "\\Results\\ROI\\" + image.getShortTitle() + "_wholeCell_cellpose_roi.zip")){
                    IJ.log("The cell ROIs of " + image.getTitle() + " were saved in " + resultsDirectory + "\\Results\\ROI\\");
                } else {
                    IJ.log("The cell ROIs of " + image.getTitle() + " could not be saved in " + resultsDirectory + "\\Results\\ROI\\");
                }
            }
            this.rawMesures = new ResultsTable();
            cellRois = roiManagerCell.getRoisAsArray();
            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA + Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
            return true;
        } else return false;
    }

    /**
     *
     * @return
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
            if (useMacro) {
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
        return name_experiment;
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
     * @return labeled binary image
     */
    public ImagePlus getCellposeOutput() {
        return cellposeOutput;
    }

    /**
     *
     * @param cell : number of the cell in the array
     * @param resultsTableFinal : {@link ResultsTable} that will contain the results
     */
    public void measureEachCell(int cell,ResultsTable resultsTableFinal) {
        imageToMeasure.setRoi(cellRois[cell]);
        analyzer.measure();
        detector.setResultsAndRename(rawMesures,resultsTableFinal,cell);
    }

    /**
     * Tests
     * @param args
     */
    public static void main(String[] args) {
        ImagePlus cytoImage = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w21 FITC.TIF");
        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\Images").mkdirs();
        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\ROI").mkdirs();
        CellDetector cellDetector = new CellDetector(cytoImage, "test", new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", true);
        cellDetector.setzStackParameters("Maximum projection");
        cellDetector.setDeeplearning(200, "cyto2", true,true);
        ImagePlus DAPI = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "test", new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", true);
        nucleiDetector.setzStackParameters("Maximum projection");
        cellDetector.setNucleiDetector(nucleiDetector);
        cellDetector.prepare();
    }
}
