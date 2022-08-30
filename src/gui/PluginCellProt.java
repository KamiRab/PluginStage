package gui;

import detectors.CellDetector;
import detectors.Experiment;
import detectors.NucleiDetector;
import detectors.SpotDetector;
import helpers.ImageToAnalyze;
import helpers.MeasureCalibration;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.Prefs;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 12/03/2022
 * GUI Class for
 * - choosing number of channels to analyze
 * - choice of calibration and measures
 * - containing all channel specific panels
 * - launching or canceling plugin
 */
public class PluginCellProt extends JFrame implements PlugIn {
    private final boolean fromDirectory;
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
    private JCheckBox saveResultsROIAndCheckBox;
    private JButton chooseResultsDirectoryButton;
    private JTextArea resultsDirectoryField;
    private JPanel resultsDirectoryPanel;
    private JPanel logo;
    private JButton setMeasurementsForNucleiButton;
    private JButton setMeasurementsForCellsButton;
    private JPanel detect;

    //    NON GUI
    private final ImageToAnalyze[] ipList;
    private ArrayList<Experiment> experiments;
    private boolean interrupt = false;
    private final MeasureValue measuresCells;
    private final MeasureValue measuresNuclei;


    public PluginCellProt(ImageToAnalyze[] imagesToAnalyse, boolean fromDirectory) {
        this.fromDirectory = fromDirectory;
        $$$setupUI$$$();
        launchButton.setVisible(false);
        for (ImageToAnalyze image : imagesToAnalyse) {
            image.setHideID(true);
        }
        ipList = imagesToAnalyse;
//        Get preferences
        nucleiCheckBox.setSelected(Prefs.get("MICMAQ.analyseNucleusBoolean", true));
        setMeasurementsForNucleiButton.setVisible(Prefs.get("MICMAQ.analyseNucleusBoolean", true));
        cytoplasmCheckBox.setSelected(Prefs.get("MICMAQ.analyseCytoplasmBoolean", false));
//        DETECTORS CONFIGURATION
        if (fromDirectory) {
            saveResultsROIAndCheckBox.setVisible(false);
            saveResultsROIAndCheckBox.setSelected(true);
            resultsDirectoryField.setText(imagesToAnalyse[0].getDirectory());
        } else {
            saveResultsROIAndCheckBox.setSelected(Prefs.get("MICMAQ.saveResults", false));
            if (saveResultsROIAndCheckBox.isSelected()) {
                resultsDirectoryPanel.setVisible(true);
            }
        }
        nrProteinsSpinner.setValue((int) Prefs.get("MICMAQ.numberProteinsChannel", 2));
        resultsDirectoryPanel.setVisible(false);
        validateMainConfigurationButton.addActionListener(e -> {
            createConfig();
            if (tabs.getTabCount() > 1) {
                tabs.setSelectedIndex(1);
            }
            launchButton.setVisible(true);
            pack();
        });
        nucleiCheckBox.addItemListener(e -> setMeasurementsForNucleiButton.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        cytoplasmCheckBox.addItemListener(e -> setMeasurementsForCellsButton.setVisible(e.getStateChange() == ItemEvent.SELECTED));

//        CALIBRATION
        addNewCalibrationButton.addActionListener(e -> new NewCalibrationPanel((DefaultComboBoxModel<MeasureCalibration>) calibrationCombo.getModel()).run());
//        CHOOSE MEASUREMENTS
        setMeasurementsForCellsButton.setVisible(Prefs.get("MICMAQ.analyseCytoplasmBoolean", false));
        measuresNuclei = new MeasureValue();
        measuresNuclei.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        measuresCells = new MeasureValue();
        measuresCells.setMeasure(Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY); /*Default measurements*/
        setMeasurementsForCellsButton.addActionListener(e -> {
            Measures cellMeas = new Measures("Cells", measuresCells);
            cellMeas.run();
        });
        setMeasurementsForNucleiButton.addActionListener(e -> {
            Measures nucleiMeas = new Measures("Nuclei", measuresNuclei);
            nucleiMeas.run();
        });
//        LAUNCH
        launchButton.addActionListener(e -> {
            interrupt = false;
            String directory = ipList[0].getDirectory();
            if (directory != null) {
//            Create parameter file
                if (!new File(directory + "/Results/Images").exists() || !new File(directory + "/Results/ROI").exists()) {
                    ImageToAnalyze.createResultsDirectory(directory);
                }
                createParametersFile();
            }

//            Set preferences
            setPreferences();
//            CREATE EXPERIMENTS
            String nameExperiment;
            ArrayList<SpotDetector> spots;
            experiments = new ArrayList<>();
            ResultsTable finalResultsNuclei = new ResultsTable();
            ResultsTable finalResultsCellSpot = new ResultsTable();
            ArrayList<CellDetector> cellDetectorArrayList = null;
            if (nucleiPanel != null) {
                nucleiPanel.setPreferences();
            }
            if (cytoplasmPanel != null) {
                cytoplasmPanel.setPreferences();
            }
            if (cytoplasmPanel != null) {
                cellDetectorArrayList = cytoplasmPanel.getImages();
            }
            if (nucleiPanel != null) {
                ArrayList<NucleiDetector> nucleiDetectorArrayList = nucleiPanel.getImages(true);
                if (nucleiDetectorArrayList != null) {
                    for (NucleiDetector nucleiDetector : nucleiDetectorArrayList) {
                        nameExperiment = nucleiDetector.getNameExperiment();
                        spots = new ArrayList<>();
                        for (SpotPanel spotPanel : spotPanels) {
                            ArrayList<SpotDetector> spotPanelImages = spotPanel.getImages();
                            spotPanel.setPreferences();
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
                        if (spots.size() == 0 && spotPanels.size() > 0) {
                            IJ.log("No spots images were associated to nuclei images. Please verify the image ending field.");
                        }
                        if (cellDetectorArrayList != null) {
                            for (CellDetector cellDetector : cellDetectorArrayList) {
                                if (cellDetector.getNameExperiment().equals(nameExperiment)) {
                                    experiments.add(new Experiment(nucleiDetector, cellDetector, spots, finalResultsCellSpot, finalResultsNuclei, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex())));
                                }
                            }
                        } else {
                            experiments.add(new Experiment(nucleiDetector, null, spots, finalResultsCellSpot, null, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex())));
                        }
                    }
                }
            } else if (cytoplasmPanel != null) {
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
                        }
                        if (spots.size() == 0 && spotPanels.size() > 0) {
                            IJ.log("No spots images were associated to nuclei images. Please verify the image ending field.");
                        }
                        experiments.add(new Experiment(null, cellDetector, spots, finalResultsCellSpot, null, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex())));
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
                    }
                    experiments.add(new Experiment(null, null, spots, finalResultsCellSpot, null, calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex())));
                }
            }
            for (SpotPanel spotPanel : spotPanels) {
                spotPanel.addParametersToFile();
            }
            if (!interrupt) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        int i = 1;

                        Instant dateBegin = Instant.now();
                        for (Experiment exp : experiments) {
//                            SHOW PROGRESS BAR
                            if (!interrupt) {
                                IJ.showProgress(i, experiments.size());
                                IJ.log("Experiment " + i + "/" + experiments.size() + " is launching.");
                                IJ.log("The name of the experiment is :" + exp.getExperimentName());
                                interrupt = !exp.run();
                                if (saveResultsROIAndCheckBox.isSelected() || fromDirectory) {
                                    if (nucleiPanel != null && cytoplasmPanel != null) {
                                        finalResultsNuclei.save(directory + "/Results/ResultsNuclei.xls");
                                    }
                                    finalResultsCellSpot.save(directory + "/Results/Results.xls");
                                }
                                IJ.log("Experiment " + i + "/" + experiments.size() + " is done.");
                            }
                            i++;
                        }
                        if (nucleiPanel != null & cytoplasmPanel != null) {
                            finalResultsNuclei.deleteRow(finalResultsNuclei.size() - 1);
                            if (saveResultsROIAndCheckBox.isSelected() || fromDirectory) {
                                finalResultsNuclei.save(directory + "/Results/ResultsNuclei.xls");
                            }
                        }
                        finalResultsCellSpot.deleteRow(finalResultsCellSpot.size() - 1);
                        if (saveResultsROIAndCheckBox.isSelected() || fromDirectory) {
                            finalResultsCellSpot.save(directory + "/Results/Results.xls");
                        }
                        Instant dateEnd = Instant.now();
                        long duration = Duration.between(dateBegin, dateEnd).toMillis();
                        IJ.log("Analysis is done. It took " + duration / 1000 + " seconds");
                        if (nucleiPanel != null && cytoplasmPanel != null) {
                            finalResultsNuclei.show("Final Results nuclei");
                        }
                        finalResultsCellSpot.show("Final Results");
                        return null;
                    }
                };
                worker.execute();
                Prefs.savePreferences();
            }
        });

        cancelButton.addActionListener(e -> {
            interrupt = true;
            if (experiments != null) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        IJ.log("Process interrupted");
                        for (Experiment exp : experiments) {
                            exp.interruptProcess();
                        }
                        return null;
                    }
                };
                worker.execute();
            } else {
                this.dispose();
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
            FileWriter fileWriter = new FileWriter(parametersFile.getAbsoluteFile(), false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.append("The configuration is :");
            bufferedWriter.close();
        } catch (IOException e) {
            IJ.log("The parametersFile could not be created in " + ipList[0].getDirectory());
            e.printStackTrace();
        }
    }

    private void setPreferences() {
        Prefs.set("MICMAQ.CalibrationValue", calibrationCombo.getItemAt(calibrationCombo.getSelectedIndex()).getName());
        Prefs.set("MICMAQ.analyseNucleusBoolean", nucleiCheckBox.isSelected());
        Prefs.set("MICMAQ.analyseCytoplasmBoolean", cytoplasmCheckBox.isSelected());
        Prefs.set("MICMAQ.numberProteinsChannel", (Integer) nrProteinsSpinner.getValue());
    }


    private void createConfig() {
        boolean wantToSave = (saveResultsROIAndCheckBox.isSelected() && !resultsDirectoryField.getText().equals("No directory chosen"));
        if (nucleiCheckBox.isSelected()) {
            if (nucleiPanel == null) {
                nucleiPanel = new NucleiPanel(ipList, fromDirectory);
                tabs.addTab("Detection of nuclei", nucleiPanel.getMainPanel());
            }
            nucleiPanel.setMeasurements(measuresNuclei.measure);
            nucleiPanel.saveResultsCheckbox(wantToSave);
        }
        if (!nucleiCheckBox.isSelected() && nucleiPanel != null) {
            tabs.remove(nucleiPanel.getMainPanel());
            nucleiPanel = null;
        }
        if (cytoplasmCheckBox.isSelected()) {
            if (cytoplasmPanel == null) {
                cytoplasmPanel = new CytoCellPanel(ipList, fromDirectory);
                tabs.addTab("Detect cell", cytoplasmPanel.getMainPanel());
            }
            cytoplasmPanel.setMeasurements(measuresCells.measure);
            cytoplasmPanel.setNucleiPanel(nucleiPanel);
            cytoplasmPanel.saveResultsCheckBox(wantToSave);
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
            spotPanels.add(new SpotPanel(ipList, tabID, fromDirectory));
            tabs.addTab("Channel " + (tabID + 1) + " quantification", spotPanels.get(tabID).getMainPanel());
        }
        for (int tabIDAll = 0; tabIDAll < nrProteinTabs; tabIDAll++) {
            spotPanels.get(tabIDAll).saveResultsCheckbox(wantToSave);
        }
    }

    public void run(String s) {
        setTitle("MIC-MAQ");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.mainPanel);
        pack();
        setVisible(true);
    }


    private void createUIComponents() {
        nrProteinsSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 4, 1));
        setCalibrationComboBox();
    }

    private void setCalibrationComboBox() {
//        Parse calibration file
        MeasureCalibration[] calibrationsArray = MeasureCalibration.getCalibrationArrayFromFile();
//        Add calibrations found to ComboBox
        DefaultComboBoxModel<MeasureCalibration> calibrationDefaultComboBoxModel = new DefaultComboBoxModel<>(calibrationsArray);
        calibrationCombo = new JComboBox<>(calibrationDefaultComboBoxModel);

//        Get preferred calibration
        MeasureCalibration measureCalibrationSelected = MeasureCalibration.findCalibrationFromName(calibrationsArray, Prefs.get("MICMAQ.CalibrationValue", "No calibration"));
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
        mainPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBackground(new Color(-16777216));
        launchButton = new JButton();
        launchButton.setText("Launch");
        mainPanel.add(launchButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        mainPanel.add(cancelButton, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabs = new JTabbedPane();
        tabs.setTabPlacement(1);
        mainPanel.add(tabs, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        general = new JPanel();
        general.setLayout(new GridLayoutManager(5, 3, new Insets(0, 0, 0, 0), -1, -1));
        tabs.addTab("General", general);
        detectInLabel = new JLabel();
        detectInLabel.setText("Detect in");
        general.add(detectInLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        general.add(nrProteinsSpinner, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nrProteinsLabel = new JLabel();
        nrProteinsLabel.setText("Number of channels to quantify");
        general.add(nrProteinsLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validateMainConfigurationButton = new JButton();
        validateMainConfigurationButton.setText("Validate main configuration");
        general.add(validateMainConfigurationButton, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(202, 75), null, 0, false));
        general.add(calibrationCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        calibrationLabel = new JLabel();
        calibrationLabel.setText("Calibration");
        general.add(calibrationLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addNewCalibrationButton = new JButton();
        addNewCalibrationButton.setText("Add new measure calibration");
        general.add(addNewCalibrationButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resultsDirectoryPanel = new JPanel();
        resultsDirectoryPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        general.add(resultsDirectoryPanel, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        detect = new JPanel();
        detect.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        general.add(detect, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        cytoplasmCheckBox = new JCheckBox();
        cytoplasmCheckBox.setText("Whole cell (+ cytoplasm only if nuclei selected) ");
        detect.add(cytoplasmCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        setMeasurementsForCellsButton = new JButton();
        setMeasurementsForCellsButton.setText("Set measurements for cells");
        detect.add(setMeasurementsForCellsButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nucleiCheckBox = new JCheckBox();
        nucleiCheckBox.setSelected(true);
        nucleiCheckBox.setText("Nuclei");
        detect.add(nucleiCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        setMeasurementsForNucleiButton = new JButton();
        setMeasurementsForNucleiButton.setText("Set measurements for nuclei");
        detect.add(setMeasurementsForNucleiButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logo = new JPanel();
        logo.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        logo.setBackground(new Color(-16777216));
        mainPanel.add(logo, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        pluginTitle = new JLabel();
        pluginTitle.setBackground(new Color(-16777216));
        pluginTitle.setForeground(new Color(-16777216));
        pluginTitle.setHorizontalAlignment(0);
        pluginTitle.setHorizontalTextPosition(0);
        pluginTitle.setIcon(new ImageIcon(getClass().getResource("/logo_bandeau.png")));
        pluginTitle.setText("");
        logo.add(pluginTitle, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nrProteinsLabel.setLabelFor(nrProteinsSpinner);
        calibrationLabel.setLabelFor(calibrationCombo);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    /**
     * Helper class that allows modification of int in measure class
     */
    static class MeasureValue {
        public int measure;

        public MeasureValue() {
        }

        public void setMeasure(int measure) {
            this.measure = measure;
        }
    }
}
