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
    private JSpinner minSizeNucleus;
    private JLabel minSizeNucleus_label;
    private JLabel threshMethods_label;
    private JPanel zProj;
    private JCheckBox useWatershedCheckBox;
    private JPanel main;
    private JPanel parameters;
    private JList<ImageToAnalyze> imageList;
    private JPanel choiceByList;
    private JTextField imageEnding;
    private JLabel segmentationMethodsLabel;
    private JLabel endingLabel;
    private JRadioButton thresholdRadioButton;
    private JRadioButton deepLearningRadioButton;
    private JButton previewButton;
    private JScrollPane imageListScrolling;
    private JCheckBox excludeOnEdgesCheckBox;
    private JSpinner firstSlice;
    private JSpinner lastSlice;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JPanel zStackParameters;
    private JLabel errorImageEnding;
    private JCheckBox finalValidationCheckBox;
    private JTextArea macroArea;
    private JCheckBox useAMacroCheckBox;
    private JScrollPane macroAreaScroll;
    private JPanel macroPannel;
    private final Calibration calibration;
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> model = new DefaultListModel<>();
    private ImageToAnalyze[] selectedDAPI;
//    private boolean fromDirectory;

    //TODO update combo avec extension ==> enlever bouton
//    TODO mettre extension par defaut et selectionner toutes les images par defaut si rien de selectionné
//    TODO attention message d'erreur si mauvaise concordance
//    TODO get Last substring index
    //TODO preference
//    TODO faire finalValidation (selection noyau + coupe noyau)
    public NucleiPanel(ImageToAnalyze[] ip_list, Calibration calibration) {
        $$$setupUI$$$();
        this.calibration = calibration;
        imagesNames = ip_list;
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
        useAMacroCheckBox.addItemListener(e -> macroPannel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        thresholdRadioButton.addActionListener(e -> parameters.setVisible(true));
        deepLearningRadioButton.addActionListener(e -> parameters.setVisible(false));

        chooseSlicesToUseCheckBox.addItemListener(e -> {
            firstSlice.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            lastSlice.setVisible(e.getStateChange() == ItemEvent.SELECTED);
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
            nucleiDetector = new NucleiDetector(first_image.getImagePlus(), name_experiment, /*isPreview, */calibration, null);
        } else {
            nucleiDetector = new NucleiDetector(first_image.getImagePlus(), name_experiment, /*isPreview, */calibration, first_image.getDirectory());
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                nucleiDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()), (int) firstSlice.getValue(), (int) lastSlice.getValue());
            } else {
                nucleiDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
            }
        }
        if (deepLearningRadioButton.isSelected()) {
            IJ.error("TODO sorry");
        } else {
            nucleiDetector.setThresholdMethod(threshMethods.getItemAt(threshMethods.getSelectedIndex()), (Double) minSizeNucleus.getValue(), useWatershedCheckBox.isSelected(), excludeOnEdgesCheckBox.isSelected(), finalValidationCheckBox.isSelected());
        }
        if (useAMacroCheckBox.isSelected()) {
            nucleiDetector.setPreprocessingMacro(macroArea.getText());
        }
        return nucleiDetector;
    }

    public JPanel getMain() {
        return main;
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
        main = new JPanel();
        main.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        main.setBorder(BorderFactory.createTitledBorder(null, "Detect nuclei", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        parameters = new JPanel();
        parameters.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        main.add(parameters, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        parameters.setBorder(BorderFactory.createTitledBorder(null, "Parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        threshMethods_label = new JLabel();
        threshMethods_label.setText("Threshold method");
        parameters.add(threshMethods_label, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeNucleus_label = new JLabel();
        minSizeNucleus_label.setText("Minimum size of nucleus");
        parameters.add(minSizeNucleus_label, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parameters.add(minSizeNucleus, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        parameters.add(useWatershedCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        parameters.add(threshMethods, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        parameters.add(previewButton, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        excludeOnEdgesCheckBox = new JCheckBox();
        excludeOnEdgesCheckBox.setSelected(true);
        excludeOnEdgesCheckBox.setText("Exclude on edges");
        parameters.add(excludeOnEdgesCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        finalValidationCheckBox = new JCheckBox();
        finalValidationCheckBox.setText("Final validation");
        parameters.add(finalValidationCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceByList = new JPanel();
        choiceByList.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        main.add(choiceByList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        choiceByList.setBorder(BorderFactory.createTitledBorder(null, "Choice of image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        imageListScrolling = new JScrollPane();
        choiceByList.add(imageListScrolling, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageListScrolling.setViewportView(imageList);
        endingLabel = new JLabel();
        endingLabel.setText("Image ending without extension");
        choiceByList.add(endingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceByList.add(imageEnding, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEnding = new JLabel();
        errorImageEnding.setText("No image corresponding to ending");
        choiceByList.add(errorImageEnding, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        choiceByList.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        main.add(panel1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(null, "Method of segmentation", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        segmentationMethodsLabel = new JLabel();
        segmentationMethodsLabel.setText("Which method of segmentation do you want to use ?");
        panel1.add(segmentationMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdRadioButton = new JRadioButton();
        thresholdRadioButton.setSelected(true);
        thresholdRadioButton.setText("Threshold");
        panel1.add(thresholdRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deepLearningRadioButton = new JRadioButton();
        deepLearningRadioButton.setText("Deep learning");
        panel1.add(deepLearningRadioButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProj = new JPanel();
        zProj.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        main.add(zProj, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(487, 48), null, 0, false));
        zProj.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a Z-stack ?");
        zProj.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters = new JPanel();
        zStackParameters.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        zProj.add(zStackParameters, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        zStackParameters.add(zProjMethods, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters.add(lastSlice, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters.add(firstSlice, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParameters.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use a macro");
        zProj.add(useAMacroCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        macroPannel = new JPanel();
        macroPannel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        zProj.add(macroPannel, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPannel.add(macroAreaScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        final Spacer spacer2 = new Spacer();
        zProj.add(spacer2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(thresholdRadioButton);
        buttonGroup.add(deepLearningRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

    private void createUIComponents() {
        model.addElement(null);
        imageList = new JList<>(model);


// List of methods for threshold
        threshMethods = new JComboBox<>(AutoThresholder.getMethods());

// minSize spinner
        minSizeNucleus = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, 999999.0, 100.0));

//  Textfield to filter extension
        imageEnding = new JTextField(15);
        firstSlice = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        lastSlice = new JSpinner(new SpinnerNumberModel(33, 0, Integer.MAX_VALUE, 1));
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
            firstSlice.setVisible(false);
            lastSlice.setVisible(false);
        }
        firstSlice.setValue((int) Prefs.get("PluginToName.firstSliceNucleus", 1));
        lastSlice.setValue((int) Prefs.get("PluginToName.lastSliceNucleus", imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroNucleus", false));
        if (!useAMacroCheckBox.isSelected()) macroPannel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroNucleus", " ")); /*TODO default macro ?*/
        deepLearningRadioButton.setSelected(Prefs.get("PluginToName.useDeepLearningNucleus", false));
        threshMethods.setSelectedItem(Prefs.get("PluginToName.thresholdMethodNucleus", "Li"));
        minSizeNucleus.setValue(Prefs.get("PluginToName.minSizeNucleus", 1000));
        useWatershedCheckBox.setSelected(Prefs.get("PluginToName.useWaterShed", false));
        excludeOnEdgesCheckBox.setSelected(Prefs.get("PluginToName.excludeOnEdges", true));
        finalValidationCheckBox.setSelected(Prefs.get("PluginToName.finalValidation", false));
    }

    public void setPreferences() {
        Prefs.set("PluginToName.nucleusEnding", imageEnding.getText());
        Prefs.set("PluginToName.zStackNucleus", isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsNucleus", zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesNucleus", chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceNucleus", (double) (int) firstSlice.getValue());
        Prefs.set("PluginToName.lastSliceNucleus", (double) (int) lastSlice.getValue());
        Prefs.set("PluginToName.useMacroNucleus", useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroNucleus", macroArea.getText());
        Prefs.set("PluginToName.useDeepLearningNucleus", deepLearningRadioButton.isSelected());
        Prefs.set("PluginToName.thresholdMethodNucleus", threshMethods.getItemAt(threshMethods.getSelectedIndex()));
        Prefs.set("PluginToName.", (double) minSizeNucleus.getValue());
        Prefs.set("PluginToName.useWaterShed", useWatershedCheckBox.isSelected());
        Prefs.set("PluginToName.excludeOnEdges", excludeOnEdgesCheckBox.isSelected());
        Prefs.set("PluginToName.finalValidation", finalValidationCheckBox.isSelected());
    }
}
