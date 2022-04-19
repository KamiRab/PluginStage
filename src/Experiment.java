import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.WindowOrganizer;

import java.util.ArrayList;

public class Experiment {
    private final NucleiDetector nuclei;
    private final ArrayList <ProteinDetector>spots;
    private String experimentName;
    private ResultsTable finalResults;

    public Experiment(NucleiDetector nuclei, ArrayList<ProteinDetector> spots, ResultsTable finalResults) {
        this.nuclei = nuclei;
        this.spots = spots;
        this.experimentName=nuclei.getName_experiment();
        this.finalResults = finalResults;
    }

    public Experiment(NucleiDetector nuclei){
        this.nuclei = nuclei;
        this.spots = new ArrayList<>();
    }

    private void addSpot(ProteinDetector spot){
        this.spots.add(spot);
    }
    public void run(){
        nuclei.prepare();
//        this.finalResults = nuclei.getResultsTableNuclei();
//        this.finalResults = new ResultsTable();
//        LinkedHashMap<String, Variable[]> results;
        Roi[] nuclei_Roi = nuclei.getRoiArray();
        for (ProteinDetector spot: spots) {
            spot.setNucleiROIs(nuclei_Roi);
            spot.prepare();
//            spot.run();
//            results = spot.getResultTableMap();
//            for (String column: results.keySet()) {
//                this.finalResults.setColumn(column,results.get(column));
//            }
        }
        for (int nucleus =0; nucleus<nuclei_Roi.length;nucleus++) {
            finalResults.addValue("Name experiment",experimentName);
            finalResults.addValue("Cell name", "Cell nr. "+ (nucleus+1));
            nuclei.measureEachNuclei(nucleus,finalResults);
            for (ProteinDetector spot: spots) {
                spot.analysisPerNucleus(nucleus,finalResults);
            }
            if (nucleus< nuclei_Roi.length-1){
                finalResults.incrementCounter();
            }
        }
        this.finalResults.show("Results_final");
        this.finalResults.save("Essai.txt");
        new WindowOrganizer().run("tile");
    }

    public ResultsTable getFinalResults() {
        return finalResults;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector =  new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003"/*,false*/,new Calibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li",1000,false,true,false);
//        NucleiDetector nucleiDetector =  new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003",true,"Maximum projection",false,"Li",1000,false,true,false);
//        nucleiDetector.run();
        ArrayList<ProteinDetector> proteinDetectorArrayList = new ArrayList<>();
        ImagePlus protein1 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ProteinDetector proteinDetector1 = new ProteinDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", 20,/*false,*/new Calibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        proteinDetector1.setzStackParameters("Maximum projection");
        proteinDetector1.setSpotByThreshold("Li",10);
        proteinDetector1.setSpotByfindMaxima(1000);
//        ProteinDetector proteinDetector1 = new ProteinDetector(protein1, "CY5", "WT_HU_Ac-2re--cell003", true, "Maximum projection", 10, false, true, 1000, "Li", 10, false);
        proteinDetectorArrayList.add(proteinDetector1);
        ImagePlus protein2 = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        ProteinDetector proteinDetector2 = new ProteinDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", 100,/* false,*/new Calibration("idk","0.103","um²"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        proteinDetector2.setzStackParameters("Maximum projection");
        proteinDetector2.setSpotByThreshold("Li",100);
        proteinDetector2.setSpotByfindMaxima(500);
//        ProteinDetector proteinDetector2 = new ProteinDetector(protein2, "FITC", "WT_HU_Ac-2re--cell003", true, "Maximum projection", 100, false, true, 500, "Li", 100, false);
        proteinDetectorArrayList.add(proteinDetector2);
//        proteinDetector.setRoiManager_nuclei(nucleiDetector.getRoiManagerNuclei().getRoisAsArray());
//        proteinDetector.run();
        ResultsTable finalResults = new ResultsTable();
        new Experiment(nucleiDetector,proteinDetectorArrayList, finalResults).run();
    }
}
