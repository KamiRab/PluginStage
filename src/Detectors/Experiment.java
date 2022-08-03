package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

public class Experiment {
    private final NucleiDetector nuclei;
    private final CellDetector cell;
    private final ArrayList<SpotDetector> spots;
    private final String experimentName;
    private final ResultsTable finalResultsNuclei;
    private final ResultsTable finalResultsCellSpot;

    private boolean interrupt = false;

    private final MeasureCalibration measureCalibration;

    public Experiment(NucleiDetector nuclei, CellDetector cytoplasm, ArrayList<SpotDetector> spots, ResultsTable finalResultsCellSpot, ResultsTable finalResultsNuclei, MeasureCalibration measureCalibration) {
        this.nuclei = nuclei;
        this.cell = cytoplasm;
        this.spots = spots;
        if (nuclei!=null) this.experimentName = nuclei.getNameExperiment();
        else if (cytoplasm!=null) this.experimentName = cytoplasm.getNameExperiment();
        else if (spots.size()>0) this.experimentName = spots.get(0).getNameExperiment();
        else this.experimentName = null;
        this.finalResultsNuclei = finalResultsNuclei;
        this.finalResultsCellSpot = finalResultsCellSpot;
        this.measureCalibration=measureCalibration;
    }

    public boolean run() {
        Instant dateBegin = Instant.now();
//        int[] idsToKeep = WindowManager.getIDList();
        if (cell !=null && !interrupt){
            IJ.log("Cell/Cytoplasm image: "+ cell.getImageTitle());
            cell.setMeasureCalibration(measureCalibration);
            if (!cell.prepare()) {
                interrupt=true;
//                PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                return false;
            }
        }
        if (nuclei!=null && !interrupt){
            IJ.log("Nuclei image: "+nuclei.getImageTitle());
            nuclei.setMeasureCalibration(measureCalibration);
            if (!nuclei.prepare()) {
                interrupt=true;
//                PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                return false;
            }
        }
        for (int i = 0; i < spots.size(); i++) {
            if (!interrupt){
                IJ.log("Spot channel "+(i+1)+ " image:" + spots.get(i).getImageTitle());
                SpotDetector spot = spots.get(i);
                spot.setMeasureCalibration(measureCalibration);
                if (!spot.prepare()) {
                    interrupt=true;
//                    PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                    return false;
                }
            }
        }
        Roi[] cellRois = null;
        Roi[] nucleiRois = null;
        Roi[] cytoplasmRois = null;
        boolean onlySpot = false;
        int numberOfObject;
        CytoDetector cytoDetector = null;
        if (cell!=null && !interrupt){
            cellRois = cell.getRoiArray();
            if (nuclei!=null){
                cytoDetector = cell.getCytoDetector();
                cytoDetector.setNucleiRois(nuclei.getRoiArray());
                cytoDetector.setMeasureCalibration(measureCalibration);
                cytoDetector.prepare();
                nucleiRois = cytoDetector.getAssociatedNucleiRois();
                cytoplasmRois = cytoDetector.getCytoRois();
                cellRois = cytoDetector.getCellRois();
                cell.setCellRois(cellRois);
//                nuclei.setNucleiRois(nucleiRois);
            }
            numberOfObject = cellRois.length;
        }else {
            if (nuclei!=null && !interrupt){
                nucleiRois = nuclei.getRoiArray();
                numberOfObject = nucleiRois.length;
            } else {
                numberOfObject = 1;
                onlySpot = true;
            }
        }
        if (cell!=null && nuclei!=null && !interrupt){
            Roi[] allNuclei = nuclei.getRoiArray();
            int[] association2Cell = cytoDetector.getAssociationCell2Nuclei();
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                finalResultsNuclei.addValue("Name experiment", experimentName);
                finalResultsNuclei.addValue("Nucleus nr.", "" + (nucleusID + 1));
                finalResultsNuclei.addValue("Cell associated",""+association2Cell[nucleusID]);
                nuclei.measureEachNuclei(nucleusID, finalResultsNuclei,allNuclei[nucleusID]);
                for (SpotDetector spot : spots) {
                    spot.analysisPerRegion(nucleusID,allNuclei[nucleusID], finalResultsNuclei,"Nuclei");
                }
                finalResultsNuclei.incrementCounter();
            }
            nuclei.setNucleiAssociatedRois(nucleiRois);
        }
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
            if (!interrupt){
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell nr.", "" + (cellID + 1));
                if (cell!=null){
                    cell.measureEachCell(cellID, finalResultsCellSpot);
                }
                if (nuclei!=null && cell==null){
                    nuclei.measureEachNuclei(cellID, finalResultsCellSpot,nucleiRois[cellID]);
                }
                if (cytoDetector!=null){
                    cytoDetector.measureEachCytoplasm(cellID, finalResultsCellSpot);
                }
                for (SpotDetector spot : spots) {
                    if (cellRois!=null)spot.analysisPerRegion(cellID,cellRois[cellID], finalResultsCellSpot,"Cell");
                    if (nucleiRois!=null && cellRois==null)spot.analysisPerRegion(cellID,nucleiRois[cellID], finalResultsCellSpot,"Nuclei");
                    if (cytoplasmRois!=null)spot.analysisPerRegion(cellID, cytoplasmRois[cellID], finalResultsCellSpot,"Cytoplasm");
                    if (onlySpot) spot.analysisPerRegion(cellID,null, finalResultsCellSpot,"Image");
                    IJ.log("spot done");
                }
                finalResultsCellSpot.incrementCounter();
            }
        }
//        if (idsToKeep!=null&&WindowManager.getIDList()!=null&&idsToKeep.length!=WindowManager.getIDList().length) {
//            PluginCellProt.closeAllWindows(experimentName + " analysis is done", idsToKeep);
//        }
        Instant dateEnd = Instant.now();
        long duration = Duration.between(dateBegin,dateEnd).toMillis();
        IJ.log("Experiment "+experimentName+" is done in :" +duration/1000+" seconds");
        return true;
    }

    public void interruptProcess() {
        interrupt = true;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003"/*,false*/,"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setMeasureCalibration(new MeasureCalibration());
        nucleiDetector.setSegmentation(false,true);
        nucleiDetector.setThresholdMethod("Li", 1000, false, true);
        ArrayList<SpotDetector> spotDetectorArrayList = new ArrayList<>();
        ImagePlus protein1 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        SpotDetector spotDetector1 = new SpotDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        spotDetector1.setRollingBallSize(20);
        spotDetector1.setzStackParameters("Maximum projection");
        spotDetector1.setSpotByThreshold("Li", 10, false,true);
        spotDetector1.setSpotByfindMaxima(1000,true);
        spotDetectorArrayList.add(spotDetector1);
        ImagePlus protein2 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        SpotDetector spotDetector2 = new SpotDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        spotDetector1.setRollingBallSize(100);
        spotDetector2.setzStackParameters("Maximum projection");
        spotDetector2.setSpotByThreshold("Li", 100, false,true);
        spotDetector2.setSpotByfindMaxima(500,true);
        spotDetectorArrayList.add(spotDetector2);
        ResultsTable finalResults = new ResultsTable();
        new Experiment(nucleiDetector, null, spotDetectorArrayList, null,finalResults,new MeasureCalibration()).run();
    }
}
