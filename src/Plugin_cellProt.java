import ij.IJ;
import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static ij.plugin.frame.RoiManager.getInstance;
import static ij.process.AutoThresholder.Method.Default;
import static ij.process.AutoThresholder.Method.Li;

public class Plugin_cellProt extends JFrame implements PlugIn {
    private JPanel Nuclei;
    private JCheckBox nucleiProteinCheckBox;
    private JCheckBox cytoplasmicProteinCheckBox;
    private JPanel Cytoplasm;
    private JLabel Plugin_title;
    private JPanel NucleiOrCytoplasm;
    private JPanel ProteinQuantification;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField textField1;
    private JComboBox comboBox1;
    private JComboBox comboBox2;
    private JComboBox comboBox3;
    private JCheckBox isAZStackCheckBox;
    private JComboBox comboBox4;
    private JComboBox comboBox5;
    private JSpinner spinner1;
    private JCheckBox previewCheckBox;
    private JCheckBox useAutomaticWatershedCheckBox;
    private JButton addProteinChannelButton;
    private JPanel main;

    public Plugin_cellProt() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }

    public void run(String s) {
        JFrame frame = new JFrame("Test");
        frame.setContentPane(new Plugin_cellProt().main);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public void launch_Plugin(){
//        OPEN DAPI IMAGE
        ImagePlus test = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        test.show();

        // PROJECTION
        ImagePlus projection_IP = ZProjector.run(test,"max"); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        projection_IP.show(); /*image projetée*/
        ImagePlus threshold_IP = new Duplicator().run(projection_IP); /*Duplique image au cas où*/ /*==duplicate()*/

        // SEUILLAGE
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*recupere processor alias image*/
        threshold_proc.setAutoThreshold(Li,true);/*definit methode et bkg foncé*/
        threshold_proc.autoThreshold(); /*crée masque binaire avec thr*/
        threshold_IP.show(); /*Montre image binaire : correspond a la creation d'un masque*/

        // Analyse particule
        ResultsTable resultsTable = new ResultsTable(); /*creation d'une resultTable pour PA*/
        RoiManager roiManager_nuclei = getInstance(); /*recupere ROImanager actuel*/
        if (roiManager_nuclei == null){ /*si aucun ROiM trouvé, en crée un*/
            roiManager_nuclei = new RoiManager();/*creation si besoin est, d'un ROI manager pour l'avoir en variable*/
        } else {
            roiManager_nuclei.reset();
        }
        ParticleAnalyzer.setRoiManager(roiManager_nuclei); /*precise ROImanager à utiliser*/
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,0,resultsTable,1000,999999);
        /*précise param a utiliser : actuellement créé overlay, ajouter ROI au ROIm, ne compte pas les particules partielles, ne mesure rien (je crois ?), taille minimale de particule = 1000*/

        threshold_proc.setAutoThreshold(Default,true); /*besoin de recreer threshold, vu que image est devenu binaire */
        particleAnalyzer.analyze(threshold_IP); /*recupere particules dans ROI + mesures*/

        resultsTable.reset();/*vide tableau*/

        int number_nuclei = roiManager_nuclei.getCount(); /*recupere nombre de particules*/
        Analyzer analyzer = new Analyzer(projection_IP, Measurements.MEAN+Measurements.AREA,resultsTable); /*precise mesures à faire et image sur laquelle faire*/
        //Iteration sur ROI manager pour mesure
        for (int i = 0; i < number_nuclei; i++) {
            roiManager_nuclei.select(projection_IP,i);
            analyzer.measure();
        }
        resultsTable.show("Results nuclei");







    }
}
