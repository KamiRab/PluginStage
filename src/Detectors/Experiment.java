package Detectors;

import GUI.PluginCellProt;
import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.WindowOrganizer;

import java.util.ArrayList;

//TODO log of experiments
//TODO remove calibration ?

public class Experiment {
    private final NucleiDetector nuclei;
    private final ArrayList <ProteinDetector>spots;
    private final String experimentName;
    private final ResultsTable finalResults;
    private final boolean showImages;

    private boolean interrupt=false;

    public Experiment(NucleiDetector nuclei, ArrayList<ProteinDetector> spots, ResultsTable finalResults, boolean showImages) {
        this.nuclei = nuclei;
        this.spots = spots;
        this.experimentName=nuclei.getName_experiment();
        this.finalResults = finalResults;
        this.showImages = showImages;
    }
//TODO renvoyer booleen pour arreter tout
    public void run(){
        if(nuclei.prepare() && !interrupt){
            Roi[] nuclei_Roi = nuclei.getRoiArray();
            IJ.log("For "+ nuclei.getImageTitle()+": "+nuclei_Roi.length+" nuclei were found.");
            for (ProteinDetector spot: spots) {
                spot.setNucleiROIs(nuclei_Roi);
                if(!spot.prepare()) {
                    PluginCellProt.closeAllWindows("The process for "+ experimentName +" has been interrupted.");
                    return;
                }
            }
            for (int nucleus =0; nucleus<nuclei_Roi.length;nucleus++) {
                finalResults.addValue("Name experiment",experimentName);
                finalResults.addValue("Cell nr.", ""+ (nucleus+1));
                nuclei.measureEachNuclei(nucleus,finalResults);
                for (ProteinDetector spot: spots) {
                    spot.analysisPerNucleus(nucleus,finalResults);
                }
                finalResults.incrementCounter();
            }
            if (showImages) {
                new WindowOrganizer().run("tile");
                PluginCellProt.closeAllWindows(experimentName+" analysis is done");
//                new WaitForUserDialog(experimentName+" analysis is done").show();
//                int[] ids = WindowManager.getIDList();
//                for (int id : ids) {
//                    ImagePlus image = WindowManager.getImage(id);
//                    image.changes = false;
//                    image.close();
//                }
            }
//            finalResults.save() TODO
        }else{
            PluginCellProt.closeAllWindows("The process for "+experimentName+" has been interrupted.");
        }
    }

    public void interruptProcess(){
        interrupt=true;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector =  new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003"/*,false*/,new MeasureCalibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li",1000,false,true,false);
//        Detectors.NucleiDetector nucleiDetector =  new Detectors.NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003",true,"Maximum projection",false,"Li",1000,false,true,false);
//        nucleiDetector.run();
        ArrayList<ProteinDetector> proteinDetectorArrayList = new ArrayList<>();
        ImagePlus protein1 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ProteinDetector proteinDetector1 = new ProteinDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", 20,/*false,*/new MeasureCalibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        proteinDetector1.setzStackParameters("Maximum projection");
        proteinDetector1.setSpotByThreshold("Li",10);
        proteinDetector1.setSpotByfindMaxima(1000);
//        Detectors.ProteinDetector proteinDetector1 = new Detectors.ProteinDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", true, "Maximum projection", 10, false, true, 1000, "Li", 10, false);
        proteinDetectorArrayList.add(proteinDetector1);
        ImagePlus protein2 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        ProteinDetector proteinDetector2 = new ProteinDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", 100,/* false,*/new MeasureCalibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/",true);
        proteinDetector2.setzStackParameters("Maximum projection");
        proteinDetector2.setSpotByThreshold("Li",100);
        proteinDetector2.setSpotByfindMaxima(500);
//        Detectors.ProteinDetector proteinDetector2 = new Detectors.ProteinDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", true, "Maximum projection", 100, false, true, 500, "Li", 100, false);
        proteinDetectorArrayList.add(proteinDetector2);
//        proteinDetector.setRoiManager_nuclei(nucleiDetector.getRoiManagerNuclei().getRoisAsArray());
//        proteinDetector.run();
        ResultsTable finalResults = new ResultsTable();
        new Experiment(nucleiDetector,proteinDetectorArrayList, finalResults,false).run();
    }
}
