package GUI;

import Detectors.NucleiDetector;
import Helpers.Calibration;
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

public class NucleiPanel extends JPanel {
    private JCheckBox isAZStackCheckBox;
    private JComboBox<String> zProjMethods;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> threshMethods;
    private JSpinner minSizeNucleusSpinner;
    private JLabel minSizeNucleusLabel;
    private JLabel threshMethodsLabel;
    private JPanel zProjPanel;
    private JCheckBox useWatershedCheckBox;
    private JPanel mainPanel;
    private JPanel parametersPanel;
    private JList<ImageToAnalyze> imageList;
    private JPanel chooseFilePanel;
    private JTextField imageEnding;
    private JLabel segmentationMethodsLabel;
    private JLabel endingLabel;
    private JRadioButton thresholdRadioButton;
    private JRadioButton deepLearningRadioButton;
    private JButton previewButton;
    private JScrollPane imageListScrolling;
    private JCheckBox excludeOnEdgesCheckBox;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JPanel zStackParameters;
    private JLabel errorImageEnding;
    private JCheckBox finalValidationCheckBox;
    private JTextArea macroArea;
    private JCheckBox useAMacroCheckBox;
    private JScrollPane macroAreaScroll;
    private JPanel macroPanel;
    private JPanel methodPanel;
    private JPanel slicesPannel;
    private final Calibration calibration;
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> model = new DefaultListModel<>();
    private ImageToAnalyze[] selectedDAPI;
    private boolean showImage;
//    private boolean fromDirectory;

    //TODO update combo avec extension ==> enlever bouton
//    TODO mettre extension par defaut et selectionner toutes les images par defaut si rien de selectionné
//    TODO attention message d'erreur si mauvaise concordance
//    TODO get Last substring index
    //TODO preference
//    TODO faire finalValidation (selection noyau + coupe noyau)
    public NucleiPanel(ImageToAnalyze[] ip_list, Calibration calibration, boolean showImage) {
        $$$setupUI$$$();
        this.calibration = calibration;
        imagesNames = ip_list;
        this.showImage = showImage;
        //        List of images
        getPreferences();
        if (imagesNames != null) {
            for (ImageToAnalyze ip : imagesNames) {
                model.removeElement(null);
                model.addElement(ip); /*TODO images avec meme nom ?*/
            }
        }
        filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEnding.getText());

//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        thresholdRadioButton.addActionListener(e -> parametersPanel.setVisible(true));
        deepLearningRadioButton.addActionListener(e -> parametersPanel.setVisible(false));

        chooseSlicesToUseCheckBox.addItemListener(e -> {
            slicesPannel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            mainPanel.revalidate();
        });
        previewButton.addActionListener(e -> {
            selectedDAPI = new ImageToAnalyze[model.getSize()];
            for (int i = 0; i < model.getSize(); i++) {
                selectedDAPI[i] = model.getElementAt(i);
            }
            if (selectedDAPI != null) {
                ImageToAnalyze first_image = selectedDAPI[0];
                String name_experiment = first_image.getImagePlus().getTitle().split(imageEnding.getText())[0];
                NucleiDetector previewND = getNucleiDetector(first_image, name_experiment, true);
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {

                        previewND.preview();
                        return null;
                    }
                };
                worker.execute();
            } else {
                IJ.error("There is no image to be used to do a preview.");
            }
        });
        imageEnding.addActionListener(e -> filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEnding.getText()));
    }

    private NucleiDetector getNucleiDetector(ImageToAnalyze first_image, String name_experiment, boolean isPreview) {
        NucleiDetector nucleiDetector;
        if (isPreview) {
            nucleiDetector = new NucleiDetector(first_image.getImagePlus(), name_experiment, /*isPreview, */calibration, null, true);
        } else {
            nucleiDetector = new NucleiDetector(first_image.getImagePlus(), name_experiment, /*isPreview, */calibration, first_image.getDirectory(), showImage);
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                nucleiDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                nucleiDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
            }
        }
        if (deepLearningRadioButton.isSelected()) {
            IJ.error("TODO sorry");
        } else {
            nucleiDetector.setThresholdMethod(threshMethods.getItemAt(threshMethods.getSelectedIndex()), (Double) minSizeNucleusSpinner.getValue(), useWatershedCheckBox.isSelected(), excludeOnEdgesCheckBox.isSelected(), finalValidationCheckBox.isSelected());
        }
        if (useAMacroCheckBox.isSelected()) {
            nucleiDetector.setPreprocessingMacro(macroArea.getText());
        }
        return nucleiDetector;
    }

    public JPanel getMain() {
        return mainPanel;
    }

    public ArrayList<NucleiDetector> getImages() {
        ArrayList<NucleiDetector> nuclei = new ArrayList<>();
        ImageToAnalyze image;
        String name_experiment;
        for (int i = 0; i < model.getSize(); i++) {
            image = model.getElementAt(i);
            name_experiment = image.getImagePlus().getTitle().split(imageEnding.getText())[0];
            nuclei.add(getNucleiDetector(image, name_experiment, false));
        }
        return nuclei;
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
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Detect nuclei", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        parametersPanel = new JPanel();
        parametersPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(parametersPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        parametersPanel.setBorder(BorderFactory.createTitledBorder(null, "Parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold method");
        parametersPanel.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeNucleusLabel = new JLabel();
        minSizeNucleusLabel.setText("Minimum size of nucleus");
        parametersPanel.add(minSizeNucleusLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parametersPanel.add(minSizeNucleusSpinner, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        parametersPanel.add(useWatershedCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parametersPanel.add(threshMethods, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        parametersPanel.add(previewButton, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        excludeOnEdgesCheckBox = new JCheckBox();
        excludeOnEdgesCheckBox.setSelected(true);
        excludeOnEdgesCheckBox.setText("Exclude on edges");
        parametersPanel.add(excludeOnEdgesCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        finalValidationCheckBox = new JCheckBox();
        finalValidationCheckBox.setText("Final validation");
        parametersPanel.add(finalValidationCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFilePanel = new JPanel();
        chooseFilePanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(chooseFilePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        chooseFilePanel.setBorder(BorderFactory.createTitledBorder(null, "Choice of image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        imageListScrolling = new JScrollPane();
        chooseFilePanel.add(imageListScrolling, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageListScrolling.setViewportView(imageList);
        endingLabel = new JLabel();
        endingLabel.setText("Image ending without extension");
        chooseFilePanel.add(endingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFilePanel.add(imageEnding, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEnding = new JLabel();
        errorImageEnding.setText("No image corresponding to ending");
        chooseFilePanel.add(errorImageEnding, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        chooseFilePanel.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        methodPanel = new JPanel();
        methodPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(methodPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        methodPanel.setBorder(BorderFactory.createTitledBorder(null, "Method of segmentation", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        segmentationMethodsLabel = new JLabel();
        segmentationMethodsLabel.setText("Which method of segmentation do you want to use ?");
        methodPanel.add(segmentationMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdRadioButton = new JRadioButton();
        thresholdRadioButton.setSelected(true);
        thresholdRadioButton.setText("Threshold");
        methodPanel.add(thresholdRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deepLearningRadioButton = new JRadioButton();
        deepLearningRadioButton.setText("Deep learning");
        methodPanel.add(deepLearningRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjPanel = new JPanel();
        zProjPanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(zProjPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(487, 48), null, 0, false));
        zProjPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a Z-stack ?");
        zProjPanel.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters = new JPanel();
        zStackParameters.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(zStackParameters, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjMethodsLabel = new JLabel();
        zProjMethodsLabel.setEnabled(true);
        zProjMethodsLabel.setText("Method of projection");
        zStackParameters.add(zProjMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjMethods = new JComboBox();
        zProjMethods.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethods.setModel(defaultComboBoxModel1);
        zStackParameters.add(zProjMethods, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParameters.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPannel = new JPanel();
        slicesPannel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zStackParameters.add(slicesPannel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        slicesPannel.add(firstSliceSpinner, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPannel.add(lastSliceSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use a macro");
        zProjPanel.add(useAMacroCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPanel = new JPanel();
        macroPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(macroPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPanel.add(macroAreaScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        final Spacer spacer2 = new Spacer();
        zProjPanel.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(thresholdRadioButton);
        buttonGroup.add(deepLearningRadioButton);
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


// List of methods for threshold
        threshMethods = new JComboBox<>(AutoThresholder.getMethods());

// minSize spinner
        minSizeNucleusSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, Integer.MAX_VALUE, 100.0));

//  Textfield to filter extension
        imageEnding = new JTextField(15);
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
//        firstSliceSpinner
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
    }

    public void filterModel(DefaultListModel<ImageToAnalyze> model, String filter) {
        for (ImageToAnalyze image : imagesNames) {
            String title = image.getImageName();
            int lastPoint = title.lastIndexOf(".");
            String title_wo_ext = title.substring(0, lastPoint);
            if (!title.endsWith(filter) && !title_wo_ext.endsWith(filter)) {
                if (model.contains(image)) {
                    model.removeElement(image);
                }
            } else {
                if (!model.contains(image)) {
                    model.addElement(image);
                }
            }
        }
        if (model.isEmpty()) { /*TODO*/
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                model.addElement(imagePlusDisplay);
            }
            errorImageEnding.setVisible(true);
        } else {
            errorImageEnding.setVisible(false);
        }
    }

    private void getPreferences() {
        imageEnding.setText(Prefs.get("PluginToName.nucleusEnding", "_w31 DAPI 405"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackNucleus", true));
        zProjMethods.setSelectedItem(Prefs.get("PluginToName.ProjMethodsNucleus", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSlidesNucleus", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPannel.setVisible(false);
        }
        firstSliceSpinner.setValue((int) Prefs.get("PluginToName.firstSliceNucleus", 1));
        lastSliceSpinner.setValue((int) Prefs.get("PluginToName.lastSliceNucleus", imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroNucleus", false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroNucleus", " ")); /*TODO default macro ?*/
        deepLearningRadioButton.setSelected(Prefs.get("PluginToName.useDeepLearningNucleus", false));
        threshMethods.setSelectedItem(Prefs.get("PluginToName.thresholdMethodNucleus", "Li"));
        minSizeNucleusSpinner.setValue(Prefs.get("PluginToName.minSizeNucleus", 1000));
        useWatershedCheckBox.setSelected(Prefs.get("PluginToName.useWaterShed", false));
        excludeOnEdgesCheckBox.setSelected(Prefs.get("PluginToName.excludeOnEdges", true));
        finalValidationCheckBox.setSelected(Prefs.get("PluginToName.finalValidation", false));
    }

    public void setPreferences() {
        Prefs.set("PluginToName.nucleusEnding", imageEnding.getText());
        Prefs.set("PluginToName.zStackNucleus", isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsNucleus", zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesNucleus", chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceNucleus", (double) (int) firstSliceSpinner.getValue());
        Prefs.set("PluginToName.lastSliceNucleus", (double) (int) lastSliceSpinner.getValue());
        Prefs.set("PluginToName.useMacroNucleus", useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroNucleus", macroArea.getText());
        Prefs.set("PluginToName.useDeepLearningNucleus", deepLearningRadioButton.isSelected());
        Prefs.set("PluginToName.thresholdMethodNucleus", threshMethods.getItemAt(threshMethods.getSelectedIndex()));
        Prefs.set("PluginToName.", (double) minSizeNucleusSpinner.getValue());
        Prefs.set("PluginToName.useWaterShed", useWatershedCheckBox.isSelected());
        Prefs.set("PluginToName.excludeOnEdges", excludeOnEdgesCheckBox.isSelected());
        Prefs.set("PluginToName.finalValidation", finalValidationCheckBox.isSelected());
    }
}
