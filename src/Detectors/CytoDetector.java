package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

public class CytoDetector {
//    TODO Label si pas de noyau
//    TODO Si cytoplasme aire >1/4 de cellule (param pour IJ.prefs)

    private final boolean saveBinaryImage;
    private final ResultsTable rawMesures;
    private final Analyzer analyzer;
    private Roi[] nucleiRois;
    private final Detector detector;
    private final boolean saveRois;
    private final ImagePlus cellCytoImageToMeasure;
    private final double minNucleiCellOverlap;
    private final double minCytoSize;
    private final String resultsDirectory;
    private final boolean showBinaryImage;

    private int[] associationCell2Nuclei;
    private final Roi[] cellRois;
    private Roi[] trueCellRois;
    private Roi[] cytoplasmRois;
    private int[] numberOfNucleiPerCell;
    private Roi[] trueAssociatedRoi;

    public CytoDetector(ImagePlus cellCytoImageToMeasure, String imageName, Roi[] cellRois, String resultsDir, boolean showBinaryImage, boolean saveBinaryImage, boolean saveRois, double minNucleiCellOverlap, double minCytoSize) {
        this.cellRois = cellRois;
        this.cellCytoImageToMeasure = cellCytoImageToMeasure.duplicate();
        this.minNucleiCellOverlap = minNucleiCellOverlap;
        this.minCytoSize = minCytoSize;
        this.cellCytoImageToMeasure.setTitle(imageName);
        this.resultsDirectory = resultsDir;
        this.showBinaryImage = showBinaryImage;
        this.saveBinaryImage = saveBinaryImage;
        this.detector = new Detector(cellCytoImageToMeasure, "Cytoplasm");
        this.saveRois = saveRois;
        rawMesures = new ResultsTable();
        analyzer = new Analyzer(this.cellCytoImageToMeasure, Measurements.MEAN + Measurements.AREA + Measurements.INTEGRATED_DENSITY, rawMesures);
        /*precise mesures à faire et image sur laquelle faire*/
    }

    public void setNucleiRois(Roi[] nucleiRois) {
        this.nucleiRois = nucleiRois;
    }

    /**
     * @param measureCalibration Calibration value and unit to use for measurements
     */
    public void setMeasureCalibration(MeasureCalibration measureCalibration) {
        detector.setMeasureCalibration(measureCalibration);
    }

    /**
     * Get cytoplasm and associated nuclei ROIs
     *
     * @return
     */
    public boolean prepare() {
        associateNucleiCell();
        getCytoplasmRois();
        ImagePlus cytoplasmLabeledMask = detector.labeledImage(cytoplasmRois);
        if (showBinaryImage) {
            cytoplasmLabeledMask.show();
            cytoplasmLabeledMask.setDisplayRange(0, cytoplasmRois.length + 5);
        }
        if (resultsDirectory != null) {
            if (saveBinaryImage) {
                if (IJ.saveAsTiff(cytoplasmLabeledMask, resultsDirectory + "/Results/Images/" + cytoplasmLabeledMask.getTitle())) {
                    IJ.log("The binary mask " + cytoplasmLabeledMask.getTitle() + " was saved in " + resultsDirectory + "/Results/Images/");
                } else {
                    IJ.log("The binary mask " + cytoplasmLabeledMask.getTitle() + " could not be saved in " + resultsDirectory + "/Results/Images/");
                }
            }
            if (saveRois){
                RoiManager roiManager = RoiManager.getInstance();
                if (roiManager == null) { /*if no instance of roiManager, creates one*/
                    roiManager = new RoiManager();
                } else { /*if already exists, empties it*/
                    roiManager.reset();
                }
//                if (!showBinaryImage){
//                    cytoplasmLabeledMask.show();
//                }
                for (Roi roi : cytoplasmRois){
                    if (roi!=null) roiManager.addRoi(roi);
                }
//                if (!showBinaryImage){
//                    cytoplasmLabeledMask.close();
//                }
                if(roiManager.save(resultsDirectory +"/Results/ROI/"+ cellCytoImageToMeasure.getShortTitle()  +"_cytoplasm_roi.zip")){
                    IJ.log("The cytoplasm ROIs of "+cellCytoImageToMeasure.getTitle() + " were saved in "+ resultsDirectory+"/Results/ROI/");
                } else {
                    IJ.log("The cytoplasm ROIs of "+cellCytoImageToMeasure.getTitle() + " could not be saved in "+ resultsDirectory+"/Results/ROI/");
                }

            }
        }
        return true;
    }
public Roi[] getAssociatedNucleiRois(){
        return trueAssociatedRoi;
}
    public int[] getAssociationCell2Nuclei() {
        return associationCell2Nuclei;
    }

    public Roi[] getCytoRois() {
        return cytoplasmRois;
    }

    /**
     * For each cell, find each nuclei associated
     * If multiple nucleis associated : combine them
     * Only the nuclei that are at minimum at 90% in the cell are considered
     * TODO : if null in nucleiDetector manage
     */
    private void associateNucleiCell() {
        associationCell2Nuclei = new int[nucleiRois.length];
        Arrays.fill(associationCell2Nuclei,-1);
        Roi[] associatedNucleiRois = new Roi[cellRois.length];
        numberOfNucleiPerCell = new int[cellRois.length];
        ArrayList<Integer> cellRoisToKeep = new ArrayList<>();
        ArrayList<Integer> associatedNucleiToKeep = new ArrayList<>();
        boolean contains;
        for (int cellID = 0; cellID < cellRois.length; cellID++) {
//            contains = false;
            Roi cellROI = cellRois[cellID];
            Rectangle cellBounding = cellROI.getBounds();
            for (int nucleusID = 0; nucleusID < nucleiRois.length; nucleusID++) {
                Roi nucleusROI = nucleiRois[nucleusID];
                Rectangle nucleiBounding = nucleusROI.getBounds();
                if (roisBoundingBoxOverlap(cellBounding,nucleiBounding)) {
                    double commonPoints = 0;
                    for (Point cellPoint : cellROI.getContainedPoints()) {
                        if (nucleusROI.contains(cellPoint.x, cellPoint.y)) {
//                            If multiple nuclei in cell, consider it is a cell in division. The nuclei are thus combined to be considered as only one
//                            To combine them : first we convert them to shape ROI, then use the function "or" and "tryToSimplify" of ShapeROI
                            commonPoints++;
//                            break;
                        }
                    }
//                    IJ.log("Nucleus :"+(nucleusID+1)+" Cell:"+(cellID+1)+" CommonPoints: "+ commonPoints + "Proportion:" + (commonPoints/nucleusROI.getContainedPoints().length));
                    if (commonPoints>minNucleiCellOverlap*nucleusROI.getContainedPoints().length){
                        numberOfNucleiPerCell[cellID]++;
                        if (associatedNucleiRois[cellID]!=null){
                            Roi roi1 = associatedNucleiRois[cellID];
                            ShapeRoi s1 = new ShapeRoi(roi1);
                            ShapeRoi s2 = new ShapeRoi(nucleusROI);
                            s1.or(s2);
                            Roi roi = s1.trySimplify();
                            associatedNucleiRois[cellID] = roi;
                        }else {
                            associatedNucleiRois[cellID]=nucleusROI;
                        }
                        if (associationCell2Nuclei[nucleusID] == -1) {
//                            int nucleussize = nucleusROI.size();
                            associationCell2Nuclei[nucleusID] = cellID+1;
                        } else {
                            IJ.error("The nucleus :"+nucleusID+" is associated to multiple cells.");
//                            associatedNucleiRois[cellID] = nuclei;
                        }
                    }
//                    }else {
//                        IJ.log(""+(commonPoints/nucleusROI.getContainedPoints().length));
//                    }
                }
            }
            if (numberOfNucleiPerCell[cellID]>0){
                cellRoisToKeep.add(cellID);
                associatedNucleiToKeep.add(cellID);
            }
        }
        if (cellRoisToKeep.size()!=cellRois.length){
            trueCellRois = new Roi[cellRoisToKeep.size()];
            trueAssociatedRoi = new Roi[cellRoisToKeep.size()];
            for (int trueCellID = 0; trueCellID < cellRoisToKeep.size() ; trueCellID++) {
                trueCellRois[trueCellID] = cellRois[cellRoisToKeep.get(trueCellID)];
                trueAssociatedRoi[trueCellID] = associatedNucleiRois[associatedNucleiToKeep.get(trueCellID)];
            }
        }else {
            trueCellRois = cellRois;
            trueAssociatedRoi = associatedNucleiRois;
        }
    }
//  TODO get cell area and cytoplasm area : if area cyto <1/4 area cell ==> NAN (another table)
    private void getCytoplasmRois() {
        cytoplasmRois = new Roi[trueCellRois.length];
        for (int cellID = 0; cellID < trueCellRois.length; cellID++) {

            Roi nuclei = trueAssociatedRoi[cellID];
            Roi cell = trueCellRois[cellID];
            Roi cytoplasm = (Roi) cell.clone();
            if (nuclei != null) {
                double common = (double) (cell.getContainedPoints().length - nuclei.getContainedPoints().length)/cell.getContainedPoints().length;
                if (common > minCytoSize){
                    ShapeRoi s0 = new ShapeRoi(cell);
                    ShapeRoi s1 = new ShapeRoi(cytoplasm);
                    ShapeRoi s2 = new ShapeRoi(nuclei);
                    s1.xor(s2);
                    s1.and(s0);
                    cytoplasm = s1.trySimplify();
//                    double cytoplasmSize = cytoplasm.getContainedPoints().length;
//                    double nucleiSize = nuclei.getContainedPoints().length;
//                    double cellSize = cell.getContainedPoints().length;
//                    IJ.log("cytoplasm "+ cellID + " size :"+cytoplasm.getContainedPoints().length);
                }else {
//                    IJ.log("Cytoplasm of cell "+ (cellID+1)+" is too little :"+common);
                    cytoplasm = null;
                }
            }
//            cytoplasm.setName("Cytoplasm"+cellID);
            cytoplasmRois[cellID] = cytoplasm;
        }
    }

    /**
     *
     * @return cell ROIs, with the cell without nucleus = null
     */
    public Roi[] getCellRois() {
        return trueCellRois;
    }

    public boolean roisBoundingBoxOverlap(Rectangle cellRec, Rectangle nucleusRec){
        return (cellRec.contains(nucleusRec.getCenterX(),nucleusRec.getCenterY()));
    }
    /**
     * Do the measurement for each cell and add them to result table
     *
     * @param cellID            : index of the cell to analyze
     * @param resultsTableFinal : resultTable to fill
     */
    public void measureEachCytoplasm(int cellID, ResultsTable resultsTableFinal) {
        if (cytoplasmRois[cellID]!=null){
            Roi cytoRoi = cytoplasmRois[cellID];
            cellCytoImageToMeasure.setRoi(cytoRoi);
            Roi test =cellCytoImageToMeasure.getRoi();
            analyzer.measure();
            resultsTableFinal.addValue("Number of nuclei", numberOfNucleiPerCell[cellID]);
            detector.setResultsAndRename(rawMesures, resultsTableFinal, cellID, "Cytoplasm");
        }else {
            analyzer.measure();
            resultsTableFinal.addValue("Number of nuclei", numberOfNucleiPerCell[cellID]);
            detector.setResultsAndRename(rawMesures, resultsTableFinal, -1, "Cytoplasm");
        }
    }

    public static void main(String[] args) {
//        ImagePlus cytoImage = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w21 FITC.TIF");
//        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\Images").mkdirs();
//        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\ROI").mkdirs();
//        CellDetector cellDetector = new CellDetector(cytoImage, "test", "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", false, false, false);
//        cellDetector.setMeasureCalibration(new MeasureCalibration());
//        cellDetector.setzStackParameters("Maximum projection");
//        cellDetector.setDeeplearning(200, "cyto2", true, false);
//        ImagePlus DAPI = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w31 DAPI 405.TIF");
//        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", false);
//        nucleiDetector.setzStackParameters("Maximum projection");
//        nucleiDetector.setMeasureCalibration(new MeasureCalibration());
//        nucleiDetector.setSegmentation(false, false);
////        nucleiDetector.setThresholdMethod("Li",100,false,true);
//        nucleiDetector.setDeeplearning(100, "cyto2", true);
//        nucleiDetector.prepare();
//        cellDetector.setNucleiDetector(nucleiDetector);
//        cellDetector.prepare();
        ImagePlus cyto = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Results\\Images\\Cell_23_w31 DAPI 405_cellpose_labeledMask.tif");
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) { /*if no instance of roiManager, creates one*/
            roiManager = new RoiManager();
        } else { /*if already exists, empties it*/
            roiManager.reset();
        }
        roiManager.open("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Results\\ROI\\Cell_23_w21FITC_wholeCell_cellpose_roi.zip");
        Roi[]cellROIS = roiManager.getRoisAsArray();
        CytoDetector cytoDetector = new CytoDetector(cyto,"Test",cellROIS,null,false,false,false, 50,25);
        cytoDetector.setMeasureCalibration(new MeasureCalibration());
        roiManager.reset();
        roiManager.open("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Results\\ROI\\Cell_23_w31DAPI405cellpose_nucleus_threshold_roi.zip");
        cytoDetector.setNucleiRois(roiManager.getRoisAsArray());
        cytoDetector.prepare();
        IJ.log("Finished");
    }
}
