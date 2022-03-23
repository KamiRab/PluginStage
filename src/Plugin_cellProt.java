import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.macro.Variable;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.PlugIn;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.BackgroundSubtracter;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import ij.util.ArrayUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import static ij.plugin.frame.RoiManager.getInstance;
import static ij.process.AutoThresholder.Method.Default;
import static ij.process.AutoThresholder.Method.Li;


//TODO plutot jtab pour protein
//TODO si stack pas coché, message d'erreur
public class Plugin_cellProt extends JFrame implements PlugIn {
    private JLabel Plugin_title;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox calibrationValues;
    private JButton addProtein2;
    private JPanel main;
    private JPanel protein2Quantification;
    private JPanel protein3Quantification;
    private JPanel protein4Quantification;
    private JButton addProtein3;
    private JButton addProtein4;
    private JPanel general;
    private JPanel nucleiDetection;
    private JPanel cytoDetection;
    private JPanel prot1quanti;
    private JPanel prot2quanti;
    private JPanel prot3quanti;
    private JTabbedPane tabs;
    private JLabel calibrationLabel;
    private JLabel analyseProteinsInLabel;
    private JCheckBox nucleiCheckBox;
    private JCheckBox cytoplasmCheckBox;
    private JSpinner nrProteins;
    private JButton validateMainConfigurationButton;

    public Plugin_cellProt() {
        $$$setupUI$$$();
        /*TODO add tab with spinner*/
        prot2quanti.setVisible(false);
        prot3quanti.setVisible(false);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launch_Plugin();
            }
        });
        nrProteins.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
            }
        });
    }

    public Plugin_cellProt(ImagePlus[] imagesToAnalyse, boolean fromDirectory) {
        $$$setupUI$$$();
        for (ImagePlus image : imagesToAnalyse) {
            IJ.log(image.getTitle());
        }
        Plugin_cellProt plugin = new Plugin_cellProt();
    }

    public void run(String s) {
        setTitle("Plugin2Name");
        setContentPane(new Plugin_cellProt().main);
//        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public void launch_Plugin() {
//        OPEN DAPI IMAGE
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        DAPI.show();

        // PROJECTION
        ImagePlus projection_IP = ZProjector.run(DAPI, "max"); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        projection_IP.show(); /*image projetée*/
        ImagePlus threshold_IP = new Duplicator().run(projection_IP); /*Duplique image au cas où*/ /*==duplicate()*/

        // SEUILLAGE
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*recupere processor alias image*/
        threshold_proc.setAutoThreshold(Li, true);/*definit methode et bkg foncé*/
        threshold_proc.autoThreshold(); /*crée masque binaire avec thr*/
        threshold_IP.show(); /*Montre image binaire : correspond a la creation d'un masque*/

        // Analyse particule
        ResultsTable resultsNuclei = new ResultsTable(); /*creation d'une resultTable pour PA*/
        RoiManager roiManager_nuclei = getInstance(); /*recupere ROImanager actuel*/
        if (roiManager_nuclei == null) { /*si aucun ROiM trouvé, en crée un*/
            roiManager_nuclei = new RoiManager();/*creation si besoin est, d'un ROI manager pour l'avoir en variable*/
        } else {
            roiManager_nuclei.reset();
        }
        ParticleAnalyzer.setRoiManager(roiManager_nuclei); /*precise ROImanager à utiliser*/
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS + ParticleAnalyzer.ADD_TO_MANAGER + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 0, resultsNuclei, 1000, 999999);
        /*précise param a utiliser : actuellement créé overlay, ajouter ROI au ROIm, ne compte pas les particules partielles, ne mesure rien (je crois ?), taille minimale de particule = 1000*/

        threshold_proc.setAutoThreshold(Default, true); /*besoin de recreer threshold, vu que image est devenu binaire */
        particleAnalyzer.analyze(threshold_IP); /*recupere particules dans ROI + mesures*/

        resultsNuclei.reset();/*vide tableau*/

        int number_nuclei = roiManager_nuclei.getCount(); /*recupere nombre de particules*/
        Analyzer analyzer = new Analyzer(projection_IP, Measurements.MEAN + Measurements.AREA, resultsNuclei); /*precise mesures à faire et image sur laquelle faire*/
        //Iteration sur ROI manager pour mesure
        for (int i = 0; i < number_nuclei; i++) {
            roiManager_nuclei.select(projection_IP, i);
            analyzer.measure();
        }
        resultsNuclei.show("Results nuclei");

//        OUVERTURE IMAGE CY5
        ImagePlus protein_IP = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ImagePlus projection2_IP = ZProjector.run(protein_IP, "max");
        projection2_IP.show();
        ImagePlus wobkg_IP = projection2_IP.duplicate();

        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkg_IP.getProcessor(), 10, false, false, false, true, false); /*Hmm correctCorners ?*/
        wobkg_IP.show();

        /*Find maxima*/
        MaximumFinder maximumFinder = new MaximumFinder();
        ResultsTable resultsTable_foci = new ResultsTable();
        Variable[] cell_name = new Variable[number_nuclei];
        Variable[] fociMean = new Variable[number_nuclei];
        Variable[] fociMin = new Variable[number_nuclei];
        Variable[] fociMax = new Variable[number_nuclei];
        Variable[] fociRawInt = new Variable[number_nuclei];
        Variable[] fociNr = new Variable[number_nuclei];
        ResultsTable rt = new ResultsTable();
        for (int i = 0; i < number_nuclei; i++) {
            cell_name[i] = new Variable("Cell nr." + i);
            roiManager_nuclei.select(wobkg_IP, i);
            ImageProcessor wobkg_proc = wobkg_IP.getProcessor();
            Polygon maxima = maximumFinder.getMaxima(wobkg_proc, 1000, true);
            int nb_maxima = maxima.npoints;
            PointRoi roi_maxima = new PointRoi(maxima);
            Point[] foci = roi_maxima.getContainedPoints();
            ArrayList<Float> pixelValue = new ArrayList<>();
            for (Point p : roi_maxima) {
                pixelValue.add(wobkg_proc.getPixelValue(p.x, p.y));
            }
            float[] roiValue;
            ArrayUtil roiArray;
            if (pixelValue.size() == 0) {
                roiValue = new float[]{0, 0};
                roiArray = new ArrayUtil(roiValue);
            } else {
                roiValue = new float[pixelValue.size()];
                for (int index = 0; index < pixelValue.size(); index++) {
                    roiValue[index] = pixelValue.get(index);
                }
                roiArray = new ArrayUtil(roiValue);
            }
            fociNr[i] = new Variable(pixelValue.size());
            fociMean[i] = new Variable(roiArray.getMean());
            fociMax[i] = new Variable(roiArray.getMaximum());
            fociMin[i] = new Variable(roiArray.getMinimum());
            fociRawInt[i] = new Variable(roiArray.getMean() * roiValue.length);
//            double mean = roiArray.getMean();
//            double max = roiArray.getMaximum();
//            double min = roiArray.getMinimum();
//            double rawInt = roiArray.getMean() * roiValue.length;

//            rt.incrementCounter();
//            rt.addValue("Mean",mean);
//            rt.addValue("Min",min);

            IJ.log("Pour le noyau :" + i + " mean :" + fociMean[i] + " max:" + fociMax[i] + "min:" + fociMin[i] + " Integrated density:" + fociRawInt[i]);
//            IJ.log("Pour le noyau :" + i + " mean :" + mean + " max:" + max + "min:" + min + " Integrated density:" + rawInt);
        }
        Variable[] image_name = new Variable[number_nuclei];
        Arrays.fill(image_name, new Variable(DAPI.getTitle().split(" ")[0]));

        String protein_name = protein_IP.getTitle().split("\\.")[0].split(" ")[1];
        rt.setColumn("Image name", image_name);
        rt.setColumn("Cell name", cell_name);
        rt.setColumn("Nucleus Area", resultsNuclei.getColumnAsVariables("Area"));
        rt.setColumn("Mean DAPI", resultsNuclei.getColumnAsVariables("Mean"));
        /*TODO density noyau*/
        rt.setColumn(protein_name + " Foci number", fociNr);
        rt.setColumn(protein_name + " Foci mean", fociMean);
        rt.setColumn(protein_name + " Foci min", fociMin);
        rt.setColumn(protein_name + " Foci max", fociMax);
        rt.setColumn(protein_name + " Foci Integrated density", fociRawInt); /*TODO remplacé par noyau entier*/
        /* pour approche par seuillage, il faut aussi integrated density par spot*/
        rt.show("Foci");
        rt.save("essai.txt");

    }

    public ResultsTable setFinalResults() {
        ResultsTable final_rt = new ResultsTable();
        return final_rt;
    }

    private void createUIComponents() {
        nucleiDetection = new Detect_Nuclei().getMain();
        nrProteins = new JSpinner();
        // TODO: place custom component creation code here
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        main = new JPanel();
        main.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(9, 4, new Insets(0, 0, 0, 0), -1, -1));
        Plugin_title = new JLabel();
        Plugin_title.setText("PluginToName");
        main.add(Plugin_title, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));


        final JLabel label1 = new JLabel();
        label1.setText("Threshold Method");
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Default");
        defaultComboBoxModel1.addElement("Huang");
        defaultComboBoxModel1.addElement("Li");
        defaultComboBoxModel1.addElement("...");
        final JLabel label2 = new JLabel();
        label2.setText("Minimum size of nucleus ?");
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Maximum projection");
        defaultComboBoxModel2.addElement("Standard Deviation projection");
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        defaultComboBoxModel3.addElement("Segmentation");
        defaultComboBoxModel3.addElement("Deep Learning");
        final JComboBox comboBox1 = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        defaultComboBoxModel4.addElement("Segmentation");
        defaultComboBoxModel4.addElement("Deep Learning");
        comboBox1.setModel(defaultComboBoxModel4);
        final JLabel label3 = new JLabel();
        label3.setText("Lot of things");
        addProtein2 = new JButton();
        addProtein2.setText("Add protein channel");
        protein2Quantification = new JPanel();
        protein2Quantification.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        main.add(protein2Quantification, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        protein2Quantification.setBorder(BorderFactory.createTitledBorder(null, "Protein 2 quantification", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Lot of things");
        protein2Quantification.add(label4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addProtein3 = new JButton();
        addProtein3.setText("Add protein channel");
        protein2Quantification.add(addProtein3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        protein3Quantification = new JPanel();
        protein3Quantification.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        main.add(protein3Quantification, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        protein3Quantification.setBorder(BorderFactory.createTitledBorder(null, "Protein 3 quantification", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label5 = new JLabel();
        label5.setText("Lot of things");
        protein3Quantification.add(label5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addProtein4 = new JButton();
        addProtein4.setText("Add protein channel");
        protein3Quantification.add(addProtein4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        protein4Quantification = new JPanel();
        protein4Quantification.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        main.add(protein4Quantification, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        protein4Quantification.setBorder(BorderFactory.createTitledBorder(null, "Protein 4 quantification", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label6 = new JLabel();
        label6.setText("Lot of things");
        protein4Quantification.add(label6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        okButton = new JButton();
        okButton.setText("Ok");
        main.add(okButton, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        main.add(cancelButton, new com.intellij.uiDesigner.core.GridConstraints(8, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Calibration");
        main.add(label7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        calibrationValues = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        defaultComboBoxModel5.addElement("x63");
        defaultComboBoxModel5.addElement("x100");
        calibrationValues.setModel(defaultComboBoxModel5);
        main.add(calibrationValues, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }
    public static void main(String[] args) {

        Plugin_cellProt plugin = new Plugin_cellProt();
        plugin.run(null);
    }
}

//TODO thread pour affichage en meme temps que calcul
/*2 classes : noyau et proteine : mesures + image + preprocessing*/
/* + classe set d'images*/
/*TODO créer form par Panel*/
