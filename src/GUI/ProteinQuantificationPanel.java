package GUI;

import Detectors.ProteinDetector;
import Helpers.MeasureCalibration;
import Helpers.ImageToAnalyze;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.Prefs;
import ij.process.AutoThresholder;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;

public class ProteinQuantificationPanel {
    private final int id;
    //    GUI COMPONENTS
    private JPanel mainPanel;
    //    Image selection
    private JPanel chooseFilePanel;
    private JLabel proteinChannelLabel;
    private JTextField proteinChannelField;
    private JScrollPane imageListScroll;
    private JList<ImageToAnalyze> imageList;
    private JLabel imageEndingLabel;
    private JTextField imageEndingField;
    private JLabel errorImageEndingLabel;
    //    Preprocessing
//    ZStack
    private JPanel zStackParametersPanel;
    private JCheckBox isAZStackCheckBox;
    private JPanel zProjPanel;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;

    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;
    //    Macro
    private JPanel macroPannel;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;
    private JScrollPane macroAreaScroll;
    //    General parameters
    private JPanel generalParametersPanel;
    private JPanel choiceMethodPanel;
    private JButton previewButton;
    private JCheckBox findMaximaCheckBox;
    private JCheckBox thresholdCheckBox;
    private JSpinner rollingBallSizeSpinner;
    private JCheckBox useRollingBallCheckBox;
    //    FindMaxima
    private JPanel maximaPanel;
    private JSpinner prominenceSpinner;
    private JLabel prominenceLabel;
    //    Threshold
    private JPanel thresholdPanel;
    private JComboBox<String> threshMethodsCombo;
    private JSpinner minSizeSpotSpinner;
    private JLabel minSizeSpotLabel;
    private JLabel threshMethodsLabel;
    private JPanel slicesPanel;

    //    NON GUI
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> model = new DefaultListModel<>();
    private boolean filteredImages;
    private final MeasureCalibration measureCalibration;
    boolean showImages;

    //    TODO mean and integrated density always (even if no method)
    public ProteinQuantificationPanel(ImageToAnalyze[] ip_list, MeasureCalibration measureCalibration, int id, boolean showImages) {
        $$$setupUI$$$();
        this.measureCalibration = measureCalibration;
        imagesNames = ip_list;
        this.id = id;
        this.showImages = showImages;

//        GET PREFERENCES
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            model.removeElement(null);
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                model.addElement(imagePlusDisplay);
            }
            filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        }
//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParametersPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPannel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        chooseSlicesToUseCheckBox.addItemListener(e -> slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        findMaximaCheckBox.addItemListener(e -> maximaPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        thresholdCheckBox.addItemListener(e -> thresholdPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        previewButton.addActionListener(e -> {
            if (model.getSize() > 0) {
                ImageToAnalyze first_image = model.firstElement();
                String name_experiment = first_image.getImagePlus().getTitle().split(imageEndingField.getText())[0];
                String protein_name = proteinChannelField.getText();
                ProteinDetector previewPD = getProteinDetector(first_image, name_experiment, protein_name, true);
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        previewPD.preview();
                        return null;
                    }
                };
                worker.execute();
            } else {
                IJ.error("There is no image to be used to do a preview.");
            }
        });
        imageEndingField.addActionListener(e -> filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel));
    }

    private void getPreferences() {
        proteinChannelField.setText(Prefs.get("PluginToName.proteinName_" + id, "CY5"));
        imageEndingField.setText(Prefs.get("PluginToName.proteinEnding_" + id, "_w11 CY5"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackProtein_" + id, true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("PluginToName.ProjMethodsProtein_" + id, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSlidesProtein_" + id, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        firstSliceSpinner.setValue((int) Prefs.get("PluginToName.firstSliceProtein_" + id, 1));
        lastSliceSpinner.setValue((int) Prefs.get("PluginToName.lastSliceProtein_" + id, imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroProtein_" + id, false));
        if (!useAMacroCheckBox.isSelected()) macroPannel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroProtein_" + id, " ")); /*TODO default macro ?*/
        useRollingBallCheckBox.setSelected(Prefs.get("PluginToName.rollingballCheckProtein_" + id, true));
        rollingBallSizeSpinner.setValue(Prefs.get("PluginToName.rollingballSizeProtein_" + id, 10));
        findMaximaCheckBox.setSelected(Prefs.get("PluginToName.findMaximaSelectedProtein_" + id, true));
        maximaPanel.setEnabled(findMaximaCheckBox.isSelected());
        thresholdCheckBox.setSelected(Prefs.get("PluginToName.thresholdSelectedProtein_" + id, false));
        thresholdPanel.setEnabled(thresholdCheckBox.isSelected());
        prominenceSpinner.setValue(Prefs.get("PluginToName.prominence_" + id, 500));
        threshMethodsCombo.setSelectedItem(Prefs.get("PluginToName.thresholdMethodProtein_" + id, "Li"));
        minSizeSpotSpinner.setValue(Prefs.get("PluginToName.minSizeSpot_" + id, 10));
    }

    private ProteinDetector getProteinDetector(ImageToAnalyze first_image, String name_experiment, String protein_name, boolean isPreview) {
        ProteinDetector proteinDetector;
        if (isPreview) {
            proteinDetector = new ProteinDetector(first_image.getImagePlus(), protein_name, name_experiment, (double) rollingBallSizeSpinner.getValue()/*, isPreview*/, measureCalibration, null, true);
        } else {
            proteinDetector = new ProteinDetector(first_image.getImagePlus(), protein_name, name_experiment, (double) rollingBallSizeSpinner.getValue()/*, isPreview*/, measureCalibration, first_image.getDirectory(), showImages);
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                proteinDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                proteinDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        if (findMaximaCheckBox.isSelected()) {
            proteinDetector.setSpotByfindMaxima((double) prominenceSpinner.getValue());
        }
        if (thresholdCheckBox.isSelected()) {
            proteinDetector.setSpotByThreshold(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()), (double) minSizeSpotSpinner.getValue());
        }
        if (useAMacroCheckBox.isSelected()) {
            proteinDetector.setPreprocessingMacro(macroArea.getText());
        }
        return proteinDetector;
    }

    public JPanel getMain() {
        return mainPanel;
    }

    public ArrayList<ProteinDetector> getImages() {
        ArrayList<ProteinDetector> selectedProteins = new ArrayList<>();
        ImageToAnalyze image;
        String name_experiment;
        String protein_name = proteinChannelField.getText();
        /*if no image selected*/
        if (!filteredImages) {
            IJ.error("No images given for protein " + (id + 1) + ". Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            for (int i = 0; i < model.getSize(); i++) {
                image = model.getElementAt(i);
                name_experiment = image.getImagePlus().getTitle().split(imageEndingField.getText())[0];
                selectedProteins.add(getProteinDetector(image, name_experiment, protein_name, false));
            }
            return selectedProteins;
        }
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
        mainPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        chooseFilePanel = new JPanel();
        chooseFilePanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(chooseFilePanel, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        imageListScroll = new JScrollPane();
        chooseFilePanel.add(imageListScroll, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageList.setEnabled(false);
        imageList.setSelectionMode(0);
        imageListScroll.setViewportView(imageList);
        imageEndingLabel = new JLabel();
        imageEndingLabel.setText("Image ending without extension");
        chooseFilePanel.add(imageEndingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFilePanel.add(imageEndingField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEndingLabel = new JLabel();
        errorImageEndingLabel.setText("No image corresponding to ending");
        chooseFilePanel.add(errorImageEndingLabel, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        chooseFilePanel.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        zProjPanel = new JPanel();
        zProjPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(zProjPanel, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a z-stack ?");
        zProjPanel.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use macro code");
        zProjPanel.add(useAMacroCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParametersPanel = new JPanel();
        zStackParametersPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(zStackParametersPanel, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjMethodsLabel = new JLabel();
        zProjMethodsLabel.setText("Method of projection");
        zStackParametersPanel.add(zProjMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjMethodsCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethodsCombo.setModel(defaultComboBoxModel1);
        zStackParametersPanel.add(zProjMethodsCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel = new JPanel();
        slicesPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zStackParametersPanel.add(slicesPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        slicesPanel.add(firstSliceSpinner, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel.add(lastSliceSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParametersPanel.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPannel = new JPanel();
        macroPannel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(macroPannel, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPannel.add(macroAreaScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        final Spacer spacer2 = new Spacer();
        zProjPanel.add(spacer2, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        useRollingBallCheckBox = new JCheckBox();
        useRollingBallCheckBox.setText("Substract background");
        zProjPanel.add(useRollingBallCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel1, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Size of rolling ball");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel1.add(rollingBallSizeSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generalParametersPanel = new JPanel();
        generalParametersPanel.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(generalParametersPanel, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        generalParametersPanel.add(previewButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceMethodPanel = new JPanel();
        choiceMethodPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        generalParametersPanel.add(choiceMethodPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        choiceMethodPanel.setBorder(BorderFactory.createTitledBorder(null, "Spot detection method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        maximaPanel = new JPanel();
        maximaPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(maximaPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("Prominence");
        maximaPanel.add(prominenceLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(thresholdPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        minSizeSpotLabel = new JLabel();
        minSizeSpotLabel.setText("Minimum size of spot");
        thresholdPanel.add(minSizeSpotLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(minSizeSpotSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(threshMethodsCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold Method");
        thresholdPanel.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findMaximaCheckBox = new JCheckBox();
        findMaximaCheckBox.setSelected(true);
        findMaximaCheckBox.setText("Find maxima");
        choiceMethodPanel.add(findMaximaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdCheckBox = new JCheckBox();
        thresholdCheckBox.setSelected(true);
        thresholdCheckBox.setText("Threshold");
        choiceMethodPanel.add(thresholdCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proteinChannelField = new JTextField();
        proteinChannelField.setText("");
        mainPanel.add(proteinChannelField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        proteinChannelLabel = new JLabel();
        proteinChannelLabel.setText("Name of protein");
        mainPanel.add(proteinChannelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        model.addElement(null);
        imageList = new JList<>(model);


// List of methods for thresholdPanel
        threshMethodsCombo = new JComboBox<>(AutoThresholder.getMethods());

//  Textfield to filter extension
        imageEndingField = new JTextField(15);
//        minSizeThr spinner
        minSizeSpotSpinner = new JSpinner(new SpinnerNumberModel(10, 0.0, Integer.MAX_VALUE, 1));

//        prominence spinner
        prominenceSpinner = new JSpinner(new SpinnerNumberModel(500.0, 0.0, Integer.MAX_VALUE, 0.5));
//rolling ball size
        rollingBallSizeSpinner = new JSpinner(new SpinnerNumberModel(10.0, 0.0, Integer.MAX_VALUE, 1.0));
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
    }

    public void setPreferences() {
        Prefs.set("PluginToName.proteinName_" + id, proteinChannelField.getText());
        Prefs.set("PluginToName.proteinEnding_" + id, imageEndingField.getText());
        Prefs.set("PluginToName.zStackProtein_" + id, isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsProtein_" + id, zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesProtein_" + id, chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceProtein_" + id, (double) (int) firstSliceSpinner.getValue());
        Prefs.set("PluginToName.lastSliceProtein_" + id, (double) (int) lastSliceSpinner.getValue());
        Prefs.set("PluginToName.useMacroProtein_" + id, useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroProtein_" + id, macroArea.getText());
        Prefs.set("PluginToName.rollingballCheckProtein_" + id, useRollingBallCheckBox.isSelected());
        Prefs.set("PluginToName.rollingballSizeProtein_" + id, (double) rollingBallSizeSpinner.getValue());
        Prefs.set("PluginToName.findMaximaSelectedProtein_" + id, findMaximaCheckBox.isSelected());
        Prefs.set("PluginToName.thresholdSelectedProtein_" + id, thresholdCheckBox.isSelected());
        Prefs.set("PluginToName.prominence_" + id, (double) prominenceSpinner.getValue());
        Prefs.set("PluginToName.thresholdMethodProtein_" + id, threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.minSizeSpot_" + id, (double) minSizeSpotSpinner.getValue());
    }
}


//seuillage : surface et nombre de spots (mais nombre moins performant)
// prominence : nb de spots


