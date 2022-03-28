import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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
    private JPanel main;
    private JPanel general;
    private JTabbedPane tabs;
    private JLabel calibrationLabel;
    private JLabel analyseProteinsInLabel;
    private JCheckBox nucleiCheckBox;
    private JCheckBox cytoplasmCheckBox;
    private JSpinner nrProteins;
    private JButton validateMainConfigurationButton;
    private JLabel nrProteinsLabel;
    Detect_Nuclei detect_nuclei;
    Detect_Nuclei detect_cytoplasm; /*TODO replace with detectCytoplasm class or similar*/
    ProteinQuantificationPanel[] quantify_proteins;
    ImagePlus[] ip_list;

    public Plugin_cellProt(ImagePlus[] imagesToAnalyse, boolean fromDirectory) {
        $$$setupUI$$$();
        for (ImagePlus image : imagesToAnalyse) {
            IJ.log(image.getTitle());
        }
        ip_list = imagesToAnalyse;
        /*TODO add tab with spinner*/
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launch_Plugin();
            }
        });
        validateMainConfigurationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (nucleiCheckBox.isSelected()) {
                    detect_nuclei = new Detect_Nuclei(ip_list);
                    tabs.addTab("Detection of nuclei", detect_nuclei.getMain());
                }
                if (cytoplasmCheckBox.isSelected()) {
                    detect_cytoplasm = new Detect_Nuclei(ip_list);
                    tabs.addTab("Detect cytoplasm", detect_cytoplasm.getMain());
                }
                Integer nrProteinTabs = (Integer) nrProteins.getValue();
                quantify_proteins = new ProteinQuantificationPanel[nrProteinTabs];
                for (int i = 0; i < nrProteinTabs; i++) {
                    quantify_proteins[i] = new ProteinQuantificationPanel(ip_list);
                    tabs.addTab("Protein " + (i + 1) + " quantification", quantify_proteins[i].getMain());
                }
                pack();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public void run(String s) {
        setTitle("Plugin2Name");
        setContentPane(this.main);
        main.setMaximumSize(new Dimension(800, 280));
        setPreferredSize(new Dimension(1000, 300));
//        setSize(1200, 200);
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
        nrProteins = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
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
        main.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        Plugin_title = new JLabel();
        Plugin_title.setText("PluginToName");
        main.add(Plugin_title, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        okButton = new JButton();
        okButton.setText("Launch");
        main.add(okButton, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        main.add(cancelButton, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        calibrationLabel = new JLabel();
        calibrationLabel.setText("Calibration");
        main.add(calibrationLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        calibrationValues = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("x63");
        defaultComboBoxModel1.addElement("x100");
        calibrationValues.setModel(defaultComboBoxModel1);
        main.add(calibrationValues, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabs = new JTabbedPane();
        tabs.setTabPlacement(1);
        main.add(tabs, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 422), new Dimension(800, 500), 0, false));
        general = new JPanel();
        general.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabs.addTab("General", general);
        analyseProteinsInLabel = new JLabel();
        analyseProteinsInLabel.setText("Analyse proteins in :");
        general.add(analyseProteinsInLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nucleiCheckBox = new JCheckBox();
        nucleiCheckBox.setText("Nuclei");
        general.add(nucleiCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(339, 24), null, 0, false));
        general.add(nrProteins, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(339, 31), null, 0, false));
        nrProteinsLabel = new JLabel();
        nrProteinsLabel.setText("Number of proteins to quantify");
        general.add(nrProteinsLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoplasmCheckBox = new JCheckBox();
        cytoplasmCheckBox.setText("Cytoplasm");
        general.add(cytoplasmCheckBox, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validateMainConfigurationButton = new JButton();
        validateMainConfigurationButton.setText("Validate main configuration");
        general.add(validateMainConfigurationButton, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(202, 75), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

    public static void main(String[] args) {
        ImagePlus[] imagesToAnalyze = new ImagePlus[3];
        imagesToAnalyze[0] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        imagesToAnalyze[1] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        imagesToAnalyze[2] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
        Plugin_cellProt plugin = new Plugin_cellProt(imagesToAnalyze, false);
        plugin.run(null);
    }
}

//TODO thread pour affichage en meme temps que calcul
/*2 classes : noyau et proteine : mesures + image + preprocessing*/
/* + classe set d'images*/
/*TODO créer form par Panel*/
