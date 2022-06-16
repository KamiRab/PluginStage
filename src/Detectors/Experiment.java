package Detectors;

import GUI.PluginCellProt;
import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

//TODO log of experiments
//TODO remove calibration ?
//TODO spot : donne ROI en arraylist de tableau

public class Experiment {
    private final NucleiDetector nuclei;
    private final CellDetector cytoplasm;
    private final ArrayList<SpotDetector> spots;
    private final String experimentName;
    private final ResultsTable finalResults;
    private final boolean showImages;

    private boolean interrupt = false;

    public Experiment(NucleiDetector nuclei, CellDetector cytoplasm, ArrayList<SpotDetector> spots, ResultsTable finalResults, boolean showImages) {
        this.nuclei = nuclei;
        this.cytoplasm = cytoplasm;
        this.spots = spots;
        if (nuclei!=null) this.experimentName = nuclei.getNameExperiment();
        else if (cytoplasm!=null) this.experimentName = cytoplasm.getNameExperiment();
        else if (spots.size()>0) this.experimentName = spots.get(0).getNameExperiment();
        else this.experimentName = null;
        this.finalResults = finalResults;
        this.showImages = showImages;
    }

    //TODO renvoyer booleen pour arreter tout
//    TODO log for association
    public boolean run() {
        Instant dateBegin = Instant.now();
        int[] idsToKeep = WindowManager.getIDList();
        if (nuclei != null) {
            IJ.log("Nuclei image: " + nuclei.getImageTitle());
            for (int i = 0; i < spots.size(); i++) {
                IJ.log("Spot channel " + (i+1) + " image:" + spots.get(i).getImageTitle());
            }
            if (nuclei.prepare() && !interrupt) {
                Roi[] nuclei_Roi = nuclei.getRoiArray();
                IJ.log("For " + nuclei.getImageTitle() + ": " + nuclei_Roi.length + " nuclei were found.");
                for (SpotDetector spot : spots) {
                    spot.setNucleiROIs(nuclei_Roi);
                    if (!spot.prepare()) {
                        PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                        return false;
                    }
                }
                for (int nucleus = 0; nucleus < nuclei_Roi.length; nucleus++) {
                    finalResults.addValue("Name experiment", experimentName);
                    finalResults.addValue("Cell nr.", "" + (nucleus + 1));
                    nuclei.measureEachNuclei(nucleus, finalResults);
                    for (SpotDetector spot : spots) {
                        spot.analysisPerRegion(nucleus, finalResults, "Nucleus");
                    }
                    finalResults.incrementCounter();
                }
            } else {
                PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                return false;
            }
        }
        if (cytoplasm!=null){
            IJ.log("Cell image: " + cytoplasm.getImageTitle());
            for (int i = 0; i < spots.size(); i++) {
                IJ.log("Spot channel " + (i+1) + " image:" + spots.get(i).getImageTitle());
            }
            if (cytoplasm.prepare() && !interrupt) {
                Roi[] nuclei_Roi = cytoplasm.getRoiArray();
                IJ.log("For " + cytoplasm.getImageTitle() + ": " + nuclei_Roi.length + " cells were found.");
                for (SpotDetector spot : spots) {
                    spot.setNucleiROIs(nuclei_Roi);
                    if (!spot.prepare()) {
                        PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                        return false;
                    }
                }
                for (int cell = 0; cell < nuclei_Roi.length; cell++) {
                    finalResults.addValue("Name experiment", experimentName);
                    finalResults.addValue("Cell nr.", "" + (cell + 1));
                    cytoplasm.measureEachCell(cell, finalResults);
                    for (SpotDetector spot : spots) {
                        spot.analysisPerRegion(cell, finalResults,"Cell");
                    }
                    finalResults.incrementCounter();
                }
            } else {
                PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                return false;
            }
        }
        if (cytoplasm == null && nuclei == null && spots.size()>0) {
            for(SpotDetector spot : spots){
                spot.setNucleiROIs(new Roi[]{null});
                if (!spot.prepare()) {
                    PluginCellProt.closeAllWindows("The process for " + experimentName + " has been interrupted.", idsToKeep);
                    return false;
                }
            }
            finalResults.addValue("Name experiment", experimentName);
            for (SpotDetector spot : spots) {
                spot.analysisPerRegion(0, finalResults,"Image");
            }
            finalResults.incrementCounter();
            }
        if (showImages) {
            PluginCellProt.closeAllWindows(experimentName + " analysis is done", idsToKeep);
        }
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
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003"/*,false*/, new MeasureCalibration("idk", "0.103", "um²"), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li", 1000, false, true, false,true);
        ArrayList<SpotDetector> spotDetectorArrayList = new ArrayList<>();
        ImagePlus protein1 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        SpotDetector spotDetector1 = new SpotDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", new MeasureCalibration("idk", "0.103", "um²"), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        spotDetector1.setRollingBallSize(20);
        spotDetector1.setzStackParameters("Maximum projection");
        spotDetector1.setSpotByThreshold("Li", 10, false,true);
        spotDetector1.setSpotByfindMaxima(1000,true);
        spotDetectorArrayList.add(spotDetector1);
        ImagePlus protein2 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        SpotDetector spotDetector2 = new SpotDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", new MeasureCalibration("idk", "0.103", "um²"), "C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        spotDetector1.setRollingBallSize(100);
        spotDetector2.setzStackParameters("Maximum projection");
        spotDetector2.setSpotByThreshold("Li", 100, false,true);
        spotDetector2.setSpotByfindMaxima(500,true);
        spotDetectorArrayList.add(spotDetector2);
        ResultsTable finalResults = new ResultsTable();
        new Experiment(nucleiDetector, null, spotDetectorArrayList, finalResults, false).run();
    }
}
