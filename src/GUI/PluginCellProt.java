package GUI;

import Detectors.CellDetector;
import Detectors.Experiment;
import Detectors.NucleiDetector;
import Detectors.SpotDetector;
import Helpers.ImageToAnalyze;
import Helpers.MeasureCalibration;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.WindowOrganizer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

//TODO progress bar (preview juste truc qui bouge sans longueur et launch avec nombre de tache)
// progress bar : xexp/nbexp
//TODO si stack pas coché, throw exception ? flag interrupted ?
//TODO verify prefs
//TODO verify saving directory
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
    private NucleiPanel nucleiPanel;
    private CytoCellPanel cytoplasmPanel;
    private final ArrayList<SpotPanel> spotPanels = new ArrayList<>();

    private JButton validateMainConfigurationButton;

    //    Additional choice
    private JCheckBox showImagesResultsCheckBox;
    private JCheckBox saveResultsROIAndCheckBox;
    private JButton chooseResultsDirectoryButton;
    private JTextArea resultsDirectoryField;
    private JPanel resultsDirectoryPanel;

    //    NON GUI
    private final ImageToAnalyze[] ipList;
    private ArrayList<Experiment> experiments;
    private boolean interrupt = false;
    private final int[] idsBefore;


    public PluginCellProt(ImageToAnalyze[] imagesToAnalyse, boolean fromDirectory) {
        idsBefore = WindowManager.getIDList();
        $$$setupUI$$$();
//        this.setAlwaysOnTop(true);
        launchButton.setVisible(false);
//        Get preferences
        /*Helpers.Calibration done in its own method*/
        nucleiCheckBox.setSelected(Prefs.get("PluginToName.analyseNucleusBoolean", true));
        cytoplasmCheckBox.setSelected(Prefs.get("PluginToName.analyseCytoplasmBoolean", false));
//        double nrproteinsDoubleValue = ;
        nrProteinsSpinner.setValue((int) Prefs.get("PluginToName.numberProteinsChannel", 2));
        resultsDirectoryPanel.setVisible(false);
        if (fromDirectory) {
            saveResultsROIAndCheckBox.setVisible(false);
            saveResultsROIAndCheckBox.setSelected(true);
            resultsDirectoryField.setText(imagesToAnalyse[0].getDirectory());
        } else {
            saveResultsROIAndCheckBox.setSelected(Prefs.get("PluginToName.saveResults", false));
            if (saveResultsROIAndCheckBox.isSelected()) {
                resultsDirectoryPanel.setVisible(true);
            }
        }
        showImagesResultsCheckBox.setSelected(Prefs.get("PluginToName.showResults", true));
//TODO item listener show image for panel

        ipList = imagesToAnalyse;

        addNewCalibrationButton.addActionListener(e -> new NewCalibrationPanel((DefaultComboBoxModel<MeasureCalibration>) calibrationCombo.getModel()).run());

        launchButton.addActionListener(e -> {
            interrupt = false;
            if (ipList[0].getDirectory() != null) {
//            Create parameter file
                createParametersFile();
            }

//            Set preferences
            setPreferences();

            String nameExperiment;
            ArrayList<SpotDetector> spots;
            experiments = new ArrayList<>();
            ResultsTable finalResults = new ResultsTable();
            if (nucleiPanel != null) {
                nucleiPanel.setPreferences();
            }
            if (cytoplasmPanel != null) {
                cytoplasmPanel.setPreferences();
            }
            if (nucleiPanel != null) {
                ArrayList<NucleiDetector> nucleiDetectorArrayList = nucleiPanel.getImages();
                if (nucleiDetectorArrayList != null) {
                    for (NucleiDetector nucleiDetector : nucleiDetectorArrayList) {
                        nameExperiment = nucleiDetector.getNameExperiment();
                        spots = new ArrayList<>();
                        for (SpotPanel spot_panel : spotPanels) {
                            spot_panel.setPreferences();
                            ArrayList<SpotDetector> spotPanelImages = spot_panel.getImages();
                            if (spotPanelImages != null) {
                                for (SpotDetector spotDetector : spotPanelImages) {
                                    if (spotDetector.getNameExperiment().equals(nameExperiment)) {
                                        spots.add(spotDetector);
                                    }
                                }
                            } else {
                                interrupt = true;
                            }
                        }
                        if (cytoplasmPanel != null) {
                            ArrayList<CellDetector> cellDetectorArrayList = cytoplasmPanel.getImages();
                            if (cellDetectorArrayList != null) {
                                for (CellDetector cellDetector : cellDetectorArrayList) {
                                    if (cellDetector.getNameExperiment().equals(nameExperiment)) {
//                                        IJ.log("Nuclei: " + nucleiDetector.getImageTitle() + " Cytoplasm/Cell :" + cellDetector.getImageTitle() + nameProteins);
                                        experiments.add(new Experiment(nucleiDetector, cellDetector, spots, finalResults, showImagesResultsCheckBox.isSelected()));
                                    }
                                }
                            }
                        } else {
//                            IJ.log("Nuclei: " + nucleiDetector.getImageTitle() + nameProteins);
                            experiments.add(new Experiment(nucleiDetector, null, spots, finalResults, showImagesResultsCheckBox.isSelected()));
                        }
                    }
                }
            } else if (cytoplasmPanel != null) {
                ArrayList<CellDetector> cellDetectorArrayList = cytoplasmPanel.getImages();
                if (cellDetectorArrayList != null) {
                    for (CellDetector cellDetector : cellDetectorArrayList) {
                        nameExperiment = cellDetector.getNameExperiment();
                        spots = new ArrayList<>();
                        for (SpotPanel spot_panel : spotPanels) {
                            spot_panel.setPreferences();
                            ArrayList<SpotDetector> spotPanelImages = spot_panel.getImages();
                            if (spotPanelImages != null) {
                                for (SpotDetector spotDetector : spotPanelImages) {
                                    if (spotDetector.getNameExperiment().equals(nameExperiment)) {
                                        spots.add(spotDetector);
                                    }
                                }
                            } else {
                                interrupt = true;
                            }
//                            IJ.log("Cell: " + cellDetector.getImageTitle() + nameProteins);
                        }
                        experiments.add(new Experiment(null, cellDetector, spots, finalResults, showImagesResultsCheckBox.isSelected()));
                    }
                }
            } else if (spotPanels.size() > 0) {
                ArrayList<SpotDetector> spotDetectorFirstPanel = spotPanels.get(0).getImages();
                for (SpotDetector spotDetectorFirst : spotDetectorFirstPanel) {
                    nameExperiment = spotDetectorFirst.getNameExperiment();
                    spots = new ArrayList<>();
                    for (SpotPanel spot_panel : spotPanels) {
                        spot_panel.setPreferences();
                        ArrayList<SpotDetector> spotPanelImages = spot_panel.getImages();
                        if (spotPanelImages != null) {
                            for (SpotDetector spotDetector : spotPanelImages) {
                                if (spotDetector.getNameExperiment().equals(nameExperiment)) {
                                    spots.add(spotDetector);
                                }
                            }
                        } else {
                            interrupt = true;
                        }
//                            IJ.log("Cell: " + cellDetector.getImageTitle() + nameProteins);
                    }
                    experiments.add(new Experiment(null, null, spots, finalResults, showImagesResultsCheckBox.isSelected()));
                }
            }
            if (!interrupt) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        int i = 0;
                        for (Experiment exp : experiments) {
//                            SHOW PROGRESS BAR
                            IJ.showProgress(i);
                            i++;
                            if (!interrupt) {
                                interrupt = !exp.run();
                                if (saveResultsROIAndCheckBox.isSelected() || fromDirectory) {
                                    finalResults.save(ipList[0].getDirectory() + "/Results/results.xls");
                                }
                            }
                        }
                        finalResults.deleteRow(finalResults.size() - 1);
                        if (saveResultsROIAndCheckBox.isSelected() || fromDirectory) {
                            finalResults.save(ipList[0].getDirectory() + "/Results/results.xls");
                        }
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
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        for (Experiment exp : experiments) {
                            exp.interruptProcess();
                            closeAllWindows("Process interrupted.", idsBefore);
                        }
                        return null;
                    }
                };
                worker.execute();
            } else {
                this.dispose(); /*TODO what to do with cancel button, when to dispose ?*/
            }
        });
        chooseResultsDirectoryButton.addActionListener(e -> {
            File directory = OpenImages.chooseDirectory(PluginCellProt.this);
            if (directory != null) {
                String path = directory.getAbsolutePath();
//                Shorten path to show only the 2 last subdirectories
                if (path.split("\\\\").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    resultsDirectoryField.setText("..." + path_shorten);
                } else {
                    resultsDirectoryField.setText(path);
                }
                for (ImageToAnalyze image : ipList) {
                    image.setDirectory(path);
                }
            }
        });
        saveResultsROIAndCheckBox.addItemListener(e -> resultsDirectoryPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
    }

    private void createParametersFile() {
        if (!new File(ipList[0].getDirectory()).exists()) {
            ImageToAnalyze.createResultsDirectory(ipList[0].getDirectory());
        }
        String parameterFilename = ipList[0].getDirectory() + "/Results/Parameters.txt";
        File parametersFile = new File(parameterFilename);
        try {
            if (parametersFile.createNewFile()) {
                IJ.log("Creation of parameters file in results directory");
            }
            FileWriter fileWriter = new FileWriter(parametersFile.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append("The configuration is :");
            bufferedWriter.close();
        } catch (IOException e) {
            IJ.log("The parametersFile could not be created in " + ipList[0].getDirectory());
            e.printStackTrace();
        }
    }

    private void setPreferences() {
        Prefs.set("PluginToName.CalibrationValue", calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()).getName());
        Prefs.set("PluginToName.analyseNucleusBoolean", nucleiCheckBox.isSelected());
        Prefs.set("PluginToName.analyseCytoplasmBoolean", cytoplasmCheckBox.isSelected());
        Prefs.set("PluginToName.numberProteinsChannel", (Integer) nrProteinsSpinner.getValue());
    }

    public static void closeAllWindows(String message, int[] idsBefore) {
        new WindowOrganizer().run("tile");
        new WaitForUserDialog(message).show();
        int[] idsAll = WindowManager.getIDList();
        if (idsAll != null) {
            boolean toclose;
            for (int idToClose : idsAll) {
                toclose = true;
                if (idsBefore != null) {
                    for (int idToKeep : idsBefore) {
                        if (idToClose == idToKeep) {
                            toclose = false;
                            break;
                        }
                    }
                    if (toclose) {
                        ImagePlus image = WindowManager.getImage(idToClose);
                        image.changes = false;
                        image.close();
                    }
                } else {
                    ImagePlus image = WindowManager.getImage(idToClose);
                    image.changes = false;
                    image.close();
                }
            }

        }
    }

    private void createConfig() {
        boolean wantTosave = (saveResultsROIAndCheckBox.isSelected() && !resultsDirectoryField.getText().equals("No directory choosen"));
        if (nucleiCheckBox.isSelected()) {
            if (nucleiPanel == null) {
                nucleiPanel = new NucleiPanel(ipList, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()));
                tabs.addTab("Detection of nuclei", nucleiPanel.getMainPanel());
            }
            nucleiPanel.setResultsCheckbox(wantTosave);
        }
        if (cytoplasmCheckBox.isSelected()) {
            if (cytoplasmPanel == null) {
                cytoplasmPanel = new CytoCellPanel(ipList, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()), showImagesResultsCheckBox.isSelected());
                tabs.addTab("Detect cytoplasm", cytoplasmPanel.getMainPanel());
            }
            cytoplasmPanel.setResultsCheckbox(wantTosave);
        }
        if (nucleiPanel != null && cytoplasmPanel != null) {
            cytoplasmPanel.setNucleiPanel(nucleiPanel);
        }
        if (!nucleiCheckBox.isSelected() && nucleiPanel != null) {
            tabs.remove(nucleiPanel.getMainPanel());
            nucleiPanel = null;
        }
        if (!cytoplasmCheckBox.isSelected() && cytoplasmPanel != null) {
            tabs.remove(cytoplasmPanel.getMainPanel());
            cytoplasmPanel = null;
        }
        Integer nrProteinTabs = (Integer) nrProteinsSpinner.getValue();
        int actualNrProteins = spotPanels.size();
        if (nrProteinTabs < actualNrProteins) {
            for (int i = 0; i < (actualNrProteins - nrProteinTabs); i++) {
                tabs.remove(spotPanels.get(spotPanels.size() - 1).getMainPanel());
                spotPanels.remove(spotPanels.size() - 1);
            }
        }
        for (int tabID = actualNrProteins; tabID < nrProteinTabs; tabID++) {
            spotPanels.add(new SpotPanel(ipList, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()), tabID, showImagesResultsCheckBox.isSelected()));
            tabs.addTab("Channel " + (tabID + 1) + " quantification", spotPanels.get(tabID).getMainPanel());
        }
        for (int tabIDAll = 0; tabIDAll < nrProteinTabs; tabIDAll++) {
            spotPanels.get(tabIDAll).setResultsCheckbox(wantTosave);
        }
    }

    public void run(String s) {
        setTitle("CellSpotMeasurer");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.mainPanel);
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
        cytoplasmCheckBox.setText("Whole cell (+ cytoplasm only if nuclei selected) ");
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
        showImagesResultsCheckBox = new JCheckBox();
        showImagesResultsCheckBox.setText("Ask to close image");
        general.add(showImagesResultsCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultsDirectoryPanel = new JPanel();
        resultsDirectoryPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        general.add(resultsDirectoryPanel, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resultsDirectoryField = new JTextArea();
        resultsDirectoryField.setText("No directory choosen");
        resultsDirectoryField.setWrapStyleWord(true);
        resultsDirectoryPanel.add(resultsDirectoryField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseResultsDirectoryButton = new JButton();
        chooseResultsDirectoryButton.setText("Choose results directory ");
        resultsDirectoryPanel.add(chooseResultsDirectoryButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        resultsDirectoryPanel.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        saveResultsROIAndCheckBox = new JCheckBox();
        saveResultsROIAndCheckBox.setText("Save results (ROI and images)");
        general.add(saveResultsROIAndCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    //TODO : centre spot = gaussian (remember)
//    TODO training spot (zerocostdl4mic) : crop image (validate data augmentation : prend image et fait translation/flip/bruit etc pour avoir plusieurs images à partir d'une)
    public static void main(String[] args) {
        ImageToAnalyze[] imagesToAnalyze = new ImageToAnalyze[6];
        imagesToAnalyze[0] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_02_w11 CY5.TIF");
        imagesToAnalyze[1] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_02_w21 FITC.TIF");
        imagesToAnalyze[2] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_02_w31 DAPI 405.TIF");
        imagesToAnalyze[3] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_23_w11 CY5.TIF");
        imagesToAnalyze[4] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_23_w21 FITC.TIF");
        imagesToAnalyze[5] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/", "Cell_23_w31 DAPI 405.TIF");
//        imagesToAnalyze[0] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
//        imagesToAnalyze[1] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell003_w11 CY5.TIF");
//        imagesToAnalyze[2] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell003_w21 FITC.TIF");
//        imagesToAnalyze[3] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell004_w31 DAPI 405.TIF");
//        imagesToAnalyze[4] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell004_w11 CY5.TIF");
//        imagesToAnalyze[5] = new ImageToAnalyze("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/", "WT_HU_Ac-2re--cell004_w21 FITC.TIF");
//        ImageToAnalyze[] imagesToAnalyze = new ImageToAnalyze[4];
//        imagesToAnalyze[0] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_2h_w21 DAPI 405.TIF");
//        imagesToAnalyze[1] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_2h_w31 FITC.TIF");
//        imagesToAnalyze[2] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_6h_w21 DAPI 405.TIF");
//        imagesToAnalyze[3] = new ImageToAnalyze("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\", "C6_1.5µM_IC5_1.1_6h_w31 FITC.TIF");
//        imagesToAnalyze[0] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_2h_w21 DAPI 405.TIF"));
//        imagesToAnalyze[1] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_2h_w31 FITC.TIF"));
//        imagesToAnalyze[2] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_6h_w21 DAPI 405.TIF"));
//        imagesToAnalyze[3] = new Helpers.ImageToAnalyze(IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 3_An Particles\\Images\\C6_1.5µM_IC5_1.1_6h_w31 FITC.TIF"));
        PluginCellProt plugin = new PluginCellProt(imagesToAnalyze, true);
        plugin.run(null);

    }
}
