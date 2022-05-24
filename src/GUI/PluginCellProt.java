package GUI;

import Detectors.Experiment;
import Detectors.NucleiDetector;
import Detectors.ProteinDetector;
import Helpers.ImageToAnalyze;
import Helpers.MeasureCalibration;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;

//TODO progress bar (preview juste truc qui bouge sans longueur et launch avec nombre de tache)
// progress bar : xexp/nbexp
//TODO si stack pas coché, throw exception ? flag interrupted ?
//TODO if error message for extension, not allow launch or preview
public class PluginCellProt extends JFrame implements PlugIn {
    //    GUI
    private JLabel pluginTitle;
    private JPanel mainPanel;
    private JButton launchButton;
    private JButton cancelButton;

    //    GUI : TABS
    private JTabbedPane tabs;
    //    GUI : TABS : GENERAL PARAMETERS
    private JPanel general;
    //    Calibration
    private JLabel calibrationLabel;
    private JComboBox<MeasureCalibration> calibrationCombo;
    private JButton addNewCalibrationButton;

    //   Choice of new tabs to add
    private JLabel detectInLabel;
    private JCheckBox nucleiCheckBox;
    private JCheckBox cytoplasmCheckBox;

    private JLabel nrProteinsLabel;
    private JSpinner nrProteinsSpinner;
    // Tabs to add class
    private NucleiPanel detect_nuclei;
    private NucleiPanel detect_cytoplasm; /*TODO replace with detectCytoplasm class or similar*/
    private final ArrayList<ProteinQuantificationPanel> quantify_proteins = new ArrayList<>();

    private JButton validateMainConfigurationButton;

    //    Additional choice
    private JCheckBox showImagesResultsCheckBox;
    private JCheckBox saveResultsROIAndCheckBox;
    private JButton chooseResultsDirectoryButton;
    private JTextField resultsDirectoryField;
    private JPanel resultsDirectoryPanel;

    //    NON GUI
    private final ImageToAnalyze[] ip_list;
    private ArrayList<Experiment> experiments;
    private boolean interrupt = false;


    public PluginCellProt(ImageToAnalyze[] imagesToAnalyse, boolean fromDirectory) {
        $$$setupUI$$$();
//        this.setAlwaysOnTop(true);
        launchButton.setVisible(false);
//        Get preferences
        /*Helpers.Calibration done in its own method*/
        nucleiCheckBox.setSelected(Prefs.get("PluginToName.analyseNucleusBoolean", true));
        cytoplasmCheckBox.setSelected(Prefs.get("PluginToName.analyseCytoplasmBoolean", false));
//        double nrproteinsDoubleValue = ;
        nrProteinsSpinner.setValue((int) Prefs.get("PluginToName.numberProteinsChannel", 2));
        if (fromDirectory) {
            saveResultsROIAndCheckBox.setVisible(false);
            resultsDirectoryPanel.setVisible(false);
        } else {
            saveResultsROIAndCheckBox.setSelected(Prefs.get("PluginToName.saveResults", false));
            if (saveResultsROIAndCheckBox.isSelected()) {
                resultsDirectoryPanel.setVisible(true);
            }
        }
        showImagesResultsCheckBox.setSelected(Prefs.get("PluginToName.showResults", true));


        ip_list = imagesToAnalyse;

        addNewCalibrationButton.addActionListener(e -> new NewCalibrationPanel((DefaultComboBoxModel<MeasureCalibration>) calibrationCombo.getModel()).run());

        launchButton.addActionListener(e -> {
//            Set preferences
            Prefs.set("PluginToName.CalibrationValue", calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()).getName());
            Prefs.set("PluginToName.analyseNucleusBoolean", nucleiCheckBox.isSelected());
            Prefs.set("PluginToName.analyseCytoplasmBoolean", cytoplasmCheckBox.isSelected());
            Prefs.set("PluginToName.numberProteinsChannel", (Integer) nrProteinsSpinner.getValue());
            String name_experiment;
            ArrayList<ProteinDetector> spots;
            experiments = new ArrayList<>();
            ResultsTable finalResults = new ResultsTable();
            detect_nuclei.setPreferences();
            boolean correctConfig = true;
            if (detect_nuclei.getImages() != null) {
                for (NucleiDetector nucleiDetector : detect_nuclei.getImages()) {
                    name_experiment = nucleiDetector.getName_experiment();
                    spots = new ArrayList<>();
                    StringBuilder test = new StringBuilder();
                    for (ProteinQuantificationPanel spot_panel : quantify_proteins) {
                        spot_panel.setPreferences();
                        if (spot_panel.getImages() != null) {
                            for (ProteinDetector proteinDetector : spot_panel.getImages()) {
                                if (proteinDetector.getName_experiment().equals(name_experiment)) {
                                    spots.add(proteinDetector);
                                    test.append("\n").append(proteinDetector.getImageTitle());
                                }
                            }
                        } else {
                            correctConfig = false;
                        }
                    }
                    if (correctConfig) {
                        IJ.log("Nuclei: " + nucleiDetector.getImageTitle() + " Proteins: " + test);
                        experiments.add(new Experiment(nucleiDetector, spots, finalResults, showImagesResultsCheckBox.isSelected()));
                    }
                }
            }
            if (correctConfig) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        for (Experiment exp : experiments) {
                            if (!interrupt) {
                                exp.run(); /*TODO test if bien sauvegardé*/
                                finalResults.save(ip_list[0].getDirectory() + "\\Results\\results.xls");
                            }
                        }
                        finalResults.deleteRow(finalResults.size() - 1);
                        finalResults.save(ip_list[0].getDirectory() + "\\Results\\results.xls");
                        finalResults.show("Final Results");
                        return null;
                    }
                };
                worker.execute();
                Prefs.savePreferences();
            }
        });
        validateMainConfigurationButton.addActionListener(e -> {
            createConfig();
            if (tabs.getTabCount() > 1) {
                tabs.setSelectedIndex(1);
            }
            launchButton.setVisible(true);
            pack();
        });
        cancelButton.addActionListener(e -> {
            /*this.dispose()*/
            interrupt = true;
            if (experiments != null) {
                for (Experiment exp : experiments
                ) {
                    exp.interruptProcess();
                    closeAllWindows("Process interrupted.");
                }
            } else {
                this.dispose(); /*TODO what to do with cancel button, when to dispose ?*/
            }
        });
        chooseResultsDirectoryButton.addActionListener(e -> {
            resultDirectoryChoice();
        });
        saveResultsROIAndCheckBox.addItemListener(e -> resultsDirectoryPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
    }

    public static void closeAllWindows(String message) {
        new WaitForUserDialog(message + "\n WARNING : all the windows will be closed without asking to save changes.").show();
        int[] ids = WindowManager.getIDList();
        for (int id : ids) {
            ImagePlus image = WindowManager.getImage(id);
            image.changes = false;
            image.close();
        }
    }

    private void resultDirectoryChoice() {
        JFileChooser directoryChooser = new JFileChooser(IJ.getDirectory("current"));
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (directoryChooser.showOpenDialog(PluginCellProt.this) == JFileChooser.APPROVE_OPTION) {
            File directory = directoryChooser.getSelectedFile();
            if (!directory.exists()) {
                IJ.error("The directory does not exists");
            } else if (!directory.isDirectory()) {
                IJ.error("It needs to be a directory not a file");
            } else {
                String path = directory.getAbsolutePath();
                if (path.split("\\\\").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    resultsDirectoryField.setText("..." + path_shorten);
                } else {
                    resultsDirectoryField.setText(path);
                }
                for (ImageToAnalyze image : ip_list) {
                    image.setDirectory(resultsDirectoryField.getText());
                }
            }
        }
    }

    private void createConfig() {
        if (nucleiCheckBox.isSelected() && detect_nuclei == null) {
            detect_nuclei = new NucleiPanel(ip_list, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()), showImagesResultsCheckBox.isSelected());
            tabs.addTab("Detection of nuclei", detect_nuclei.getMain());
        }
        if (cytoplasmCheckBox.isSelected() && detect_cytoplasm == null) {
            detect_cytoplasm = new NucleiPanel(ip_list, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()), showImagesResultsCheckBox.isSelected());
            tabs.addTab("Detect cytoplasm", detect_cytoplasm.getMain());
        }
        Integer nrProteinTabs = (Integer) nrProteinsSpinner.getValue();
        int actualNrProteins = quantify_proteins.size();
//        quantify_proteins = new ProteinQuantificationPanel[nrProteinTabs];
        if (nrProteinTabs < actualNrProteins) {
            int number_tabs = 1 + actualNrProteins;
            if (detect_nuclei != null) number_tabs++;
            if (detect_cytoplasm != null) number_tabs++;
            for (int i = 0; i < (actualNrProteins - nrProteinTabs); i++) {
                quantify_proteins.remove(actualNrProteins - i - 1);
                tabs.remove(number_tabs - i - 1);
            }
        }
        for (int tabID = actualNrProteins; tabID < nrProteinTabs; tabID++) {
//                    quantify_proteins[i] = new ProteinQuantificationPanel(ip_list);
            quantify_proteins.add(new ProteinQuantificationPanel(ip_list, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()), tabID, showImagesResultsCheckBox.isSelected()));
            tabs.addTab("Channel " + (tabID + 1) + " quantification", quantify_proteins.get(tabID).getMain());
        }
    }

    public void run(String s) {
        setTitle("Plugin2Name");
        setContentPane(this.mainPanel);
//        setSize(new Dimension(500, 500));
//        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }


    private void createUIComponents() {
        nrProteinsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        setCalibrationComboBox();
        // TODO: place custom component creation code here
    }

    private void setCalibrationComboBox() {
//        Parse calibration file
        MeasureCalibration[] calibrationsArray = MeasureCalibration.getCalibrationArrayFromFile();
//        Add calibrations found to ComboBox
        DefaultComboBoxModel<MeasureCalibration> calibrationDefaultComboBoxModel = new DefaultComboBoxModel<>(calibrationsArray);
        calibrationCombo = new JComboBox<>(calibrationDefaultComboBoxModel);

//        Get prefered calibration
        MeasureCalibration measureCalibrationSelected = MeasureCalibration.findCalibrationFromName(calibrationsArray, Prefs.get("PluginToName.CalibrationValue", "No calibration"));
        if (measureCalibrationSelected == null) {
            measureCalibrationSelected = new MeasureCalibration();
            calibrationDefaultComboBoxModel.addElement(measureCalibrationSelected);
        }
        calibrationCombo.setSelectedItem(measureCalibrationSelected);
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        pluginTitle = new JLabel();
        pluginTitle.setText("PluginToName");
        mainPanel.add(pluginTitle, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        launchButton = new JButton();
        launchButton.setText("Launch");
        mainPanel.add(launchButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        mainPanel.add(cancelButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabs = new JTabbedPane();
        tabs.setTabPlacement(1);
        mainPanel.add(tabs, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 422), null, 0, false));
        general = new JPanel();
        general.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabs.addTab("General", general);
        detectInLabel = new JLabel();
        detectInLabel.setText("Detect in");
        general.add(detectInLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nucleiCheckBox = new JCheckBox();
        nucleiCheckBox.setSelected(true);
        nucleiCheckBox.setText("Nuclei");
        general.add(nucleiCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(339, 24), null, 0, false));
        general.add(nrProteinsSpinner, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(339, 31), null, 0, false));
        nrProteinsLabel = new JLabel();
        nrProteinsLabel.setText("Number of channels to quantify");
        general.add(nrProteinsLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoplasmCheckBox = new JCheckBox();
        cytoplasmCheckBox.setText("Cytoplasm");
        general.add(cytoplasmCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validateMainConfigurationButton = new JButton();
        validateMainConfigurationButton.setText("Validate main configuration");
        general.add(validateMainConfigurationButton, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(202, 75), null, 0, false));
        general.add(calibrationCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        calibrationLabel = new JLabel();
        calibrationLabel.setText("Calibration");
        general.add(calibrationLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addNewCalibrationButton = new JButton();
        addNewCalibrationButton.setText("Add new measureCalibration");
        general.add(addNewCalibrationButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveResultsROIAndCheckBox = new JCheckBox();
        saveResultsROIAndCheckBox.setText("Save results (ROI and images)");
        general.add(saveResultsROIAndCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultsDirectoryPanel = new JPanel();
        resultsDirectoryPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        general.add(resultsDirectoryPanel, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultsDirectoryField = new JTextField();
        resultsDirectoryPanel.add(resultsDirectoryField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseResultsDirectoryButton = new JButton();
        chooseResultsDirectoryButton.setText("Choose results directory ");
        resultsDirectoryPanel.add(chooseResultsDirectoryButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showImagesResultsCheckBox = new JCheckBox();
        showImagesResultsCheckBox.setText("Show images during analysis");
        general.add(showImagesResultsCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }


    public static void main(String[] args) {
//        Helpers.ImageToAnalyze[] imagesToAnalyze = new Helpers.ImageToAnalyze[6];
//        imagesToAnalyze[0] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF"));
//        imagesToAnalyze[1] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF"));
//        imagesToAnalyze[2] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF"));
//        imagesToAnalyze[3] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell004_w31 DAPI 405.TIF"));
//        imagesToAnalyze[4] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell004_w11 CY5.TIF"));
//        imagesToAnalyze[5] = new Helpers.ImageToAnalyze(IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell004_w21 FITC.TIF"));
        ImageToAnalyze[] imagesToAnalyze = new ImageToAnalyze[4];
        imagesToAnalyze[0] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_2h_w21 DAPI 405.TIF");
        imagesToAnalyze[1] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_2h_w31 FITC.TIF");
        imagesToAnalyze[2] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_6h_w21 DAPI 405.TIF");
        imagesToAnalyze[3] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_6h_w31 FITC.TIF");
//        imagesToAnalyze[0] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_2h_w21 DAPI 405.TIF"));
//        imagesToAnalyze[1] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_2h_w31 FITC.TIF"));
//        imagesToAnalyze[2] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_6h_w21 DAPI 405.TIF"));
//        imagesToAnalyze[3] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_6h_w31 FITC.TIF"));
        PluginCellProt plugin = new PluginCellProt(imagesToAnalyze, true);
        plugin.run(null);
    }
}

//TODO thread pour affichage en meme temps que calcul
/*2 classes : noyau et proteine : mesures + image + preprocessing*/
/* + classe set d'images*/
/*TODO créer form par Panel*/
