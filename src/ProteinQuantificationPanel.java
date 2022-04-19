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
    private JPanel main;
    //    Image selection
    private JPanel choiceByExtension;
    private JLabel proteinChannelLabel;
    private JTextField proteinChannel;
    private JScrollPane imageListScroll;
    private JList<ImageToAnalyze> imageList;
    private JLabel endingLabel;
    private JTextField imageEnding;
    private JLabel errorImageEnding;
    //    Preprocessing
//    ZStack
    private JPanel zStackParameters;
    private JCheckBox isAZStackCheckBox;
    private JPanel zProj;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethods;

    private JCheckBox chooseSlicesToUseCheckBox;
    private JSpinner firstSlice;
    private JSpinner lastSlice;
    //    Macro
    private JPanel macroPannel;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;
    private JScrollPane macroAreaScroll;
    //    General parameters
    private JPanel maximaOrThreshold;
    private JPanel choiceMethod;
    private JButton previewButton;
    private JLabel choiceMethodLabel;
    private JCheckBox findMaximaCheckBox;
    private JCheckBox thresholdCheckBox;
    private JSpinner rollingBallSize;
    private JLabel rollingBallSizeLabel;
    //    FindMaxima
    private JPanel maxima;
    private JSpinner prominenceValue;
    private JLabel prominenceLabel;
    //    Threshold
    private JPanel threshold;
    private JComboBox<String> threshMethods;
    private JSpinner minSizeSpot;
    private JLabel minSizeSpotLabel;
    private JLabel threshMethodsLabel;

    //    NON GUI
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> model = new DefaultListModel<>();
    private ArrayList<ImageToAnalyze> selectedChannel;
    private ArrayList<ProteinDetector> selectedProteins;
    private final Calibration calibration;
//    private boolean fromDirectory;
//    private ImagePlusDisplay[] selectedChannel;

    public ProteinQuantificationPanel(ImageToAnalyze[] ip_list, Calibration calibration, int id) {
        $$$setupUI$$$();
        this.calibration = calibration;
        imagesNames = ip_list;
        this.id = id;

//        GET PREFERENCES
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            model.removeElement(null);
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                model.addElement(imagePlusDisplay);
            }
        }
        filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEnding.getText());
//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPannel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        chooseSlicesToUseCheckBox.addItemListener(e -> {
            firstSlice.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            lastSlice.setVisible(e.getStateChange() == ItemEvent.SELECTED);
        });

        findMaximaCheckBox.addItemListener(e -> maxima.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        thresholdCheckBox.addItemListener(e -> threshold.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        previewButton.addActionListener(e -> {
            for (int i = 0; i < model.getSize(); i++) {
                selectedChannel = new ArrayList<>();
                selectedChannel.add(model.getElementAt(i));
            }
            if (selectedChannel != null) {
                ImageToAnalyze first_image = selectedChannel.get(0);
                String name_experiment = first_image.getImagePlus().getTitle().split(imageEnding.getText())[0];
                String protein_name = proteinChannel.getText();
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
        imageEnding.addActionListener(e -> filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEnding.getText()));
    }

    private void getPreferences() {
        proteinChannel.setText(Prefs.get("PluginToName.proteinName_" + id, "CY5"));
        imageEnding.setText(Prefs.get("PluginToName.proteinEnding_" + id, "_w11 CY5"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackProtein_" + id, true));
        zProjMethods.setSelectedItem(Prefs.get("PluginToName.ProjMethodsProtein_" + id, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSlidesProtein_" + id, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            firstSlice.setVisible(false);
            lastSlice.setVisible(false);
        }
        firstSlice.setValue((int) Prefs.get("PluginToName.firstSliceProtein_" + id, 1));
        lastSlice.setValue((int) Prefs.get("PluginToName.lastSliceProtein_" + id, imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroProtein_" + id, false));
        if (!useAMacroCheckBox.isSelected()) macroPannel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroProtein_" + id, " ")); /*TODO default macro ?*/
        rollingBallSize.setValue(Prefs.get("PluginToName.rollingballSizeProtein_" + id, 10));
        findMaximaCheckBox.setSelected(Prefs.get("PluginToName.findMaximaSelectedProtein_" + id, true));
        maxima.setVisible(findMaximaCheckBox.isSelected());
        thresholdCheckBox.setSelected(Prefs.get("PluginToName.thresholdSelectedProtein_" + id, false));
        threshold.setVisible(thresholdCheckBox.isSelected());
        prominenceValue.setValue(Prefs.get("PluginToName.prominence_" + id, 500));
        threshMethods.setSelectedItem(Prefs.get("PluginToName.thresholdMethodProtein_" + id, "Li"));
        minSizeSpot.setValue(Prefs.get("PluginToName.minSizeSpot_" + id, 10));
    }

    private ProteinDetector getProteinDetector(ImageToAnalyze first_image, String name_experiment, String protein_name, boolean isPreview) {
        ProteinDetector proteinDetector;
        if (isPreview) {
            proteinDetector = new ProteinDetector(first_image.getImagePlus(), protein_name, name_experiment, (double) rollingBallSize.getValue()/*, isPreview*/, calibration, null);
        } else {
            proteinDetector = new ProteinDetector(first_image.getImagePlus(), protein_name, name_experiment, (double) rollingBallSize.getValue()/*, isPreview*/, calibration, first_image.getDirectory());
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                proteinDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()), (int) firstSlice.getValue(), (int) lastSlice.getValue());
            } else {
                proteinDetector.setzStackParameters(zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
            }
        }
        if (findMaximaCheckBox.isSelected()) {
            proteinDetector.setSpotByfindMaxima((double) prominenceValue.getValue());
        }
        if (thresholdCheckBox.isSelected()) {
            proteinDetector.setSpotByThreshold(threshMethods.getItemAt(threshMethods.getSelectedIndex()), (double) minSizeSpot.getValue());
        }
        if (useAMacroCheckBox.isSelected()) {
            proteinDetector.setPreprocessingMacro(macroArea.getText());
        }
        return proteinDetector;
    }

    public JPanel getMain() {
        return main;
    }

    public ArrayList<ProteinDetector> getImages() {
        selectedProteins = new ArrayList<>();
        ImageToAnalyze image;
        String name_experiment;
        String protein_name = proteinChannel.getText();
        for (int i = 0; i < model.getSize(); i++) {
            image = model.getElementAt(i);
            name_experiment = image.getImagePlus().getTitle().split(imageEnding.getText())[0];
            selectedProteins.add(getProteinDetector(image, name_experiment, protein_name, false));
        }
        return selectedProteins;
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
        main.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        main.setBorder(BorderFactory.createTitledBorder(null, "", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        choiceByExtension = new JPanel();
        choiceByExtension.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        main.add(choiceByExtension, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        imageListScroll = new JScrollPane();
        choiceByExtension.add(imageListScroll, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageList.setEnabled(false);
        imageList.setSelectionMode(0);
        imageListScroll.setViewportView(imageList);
        endingLabel = new JLabel();
        endingLabel.setText("Image ending without extension");
        choiceByExtension.add(endingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceByExtension.add(imageEnding, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEnding = new JLabel();
        errorImageEnding.setText("No image corresponding to ending");
        choiceByExtension.add(errorImageEnding, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        choiceByExtension.add(spacer1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        zProj = new JPanel();
        zProj.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        main.add(zProj, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProj.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a z-stack ?");
        zProj.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use a macro");
        zProj.add(useAMacroCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters = new JPanel();
        zStackParameters.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        zProj.add(zStackParameters, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjMethodsLabel = new JLabel();
        zProjMethodsLabel.setText("Method of projection");
        zStackParameters.add(zProjMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjMethods = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethods.setModel(defaultComboBoxModel1);
        zStackParameters.add(zProjMethods, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParameters.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters.add(firstSlice, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zStackParameters.add(lastSlice, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        maximaOrThreshold = new JPanel();
        maximaOrThreshold.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        main.add(maximaOrThreshold, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        maximaOrThreshold.add(previewButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceMethod = new JPanel();
        choiceMethod.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        maximaOrThreshold.add(choiceMethod, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        choiceMethod.setBorder(BorderFactory.createTitledBorder(null, "General parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        choiceMethodLabel = new JLabel();
        choiceMethodLabel.setText("Which method do you want to use ?");
        choiceMethod.add(choiceMethodLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxima = new JPanel();
        maxima.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethod.add(maxima, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maxima.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("Prominence (value for maxima function)");
        maxima.add(prominenceLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxima.add(prominenceValue, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshold = new JPanel();
        threshold.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethod.add(threshold, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        threshold.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        minSizeSpotLabel = new JLabel();
        minSizeSpotLabel.setText("Minimum size of spot");
        threshold.add(minSizeSpotLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshold.add(minSizeSpot, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshold.add(threshMethods, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold Method");
        threshold.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rollingBallSizeLabel = new JLabel();
        rollingBallSizeLabel.setText("Rolling ball size");
        choiceMethod.add(rollingBallSizeLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choiceMethod.add(rollingBallSize, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        findMaximaCheckBox = new JCheckBox();
        findMaximaCheckBox.setSelected(true);
        findMaximaCheckBox.setText("Find maxima");
        choiceMethod.add(findMaximaCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdCheckBox = new JCheckBox();
        thresholdCheckBox.setSelected(true);
        thresholdCheckBox.setText("Threshold");
        choiceMethod.add(thresholdCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        proteinChannel = new JTextField();
        proteinChannel.setText("");
        main.add(proteinChannel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        proteinChannelLabel = new JLabel();
        proteinChannelLabel.setText("Name of protein");
        main.add(proteinChannelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
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
        if (model.isEmpty()) {
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                model.addElement(imagePlusDisplay);
            }
            errorImageEnding.setVisible(true);
        } else {
            errorImageEnding.setVisible(false);
        }
    }


    private void createUIComponents() {
        model.addElement(null);
        imageList = new JList<>(model);


// List of methods for threshold
        threshMethods = new JComboBox<>(AutoThresholder.getMethods());

//  Textfield to filter extension
        imageEnding = new JTextField(15);
//        minSizeThr spinner
        minSizeSpot = new JSpinner(new SpinnerNumberModel(10, 0.0, 999999.0, 1));

//        prominence spinner
        prominenceValue = new JSpinner(new SpinnerNumberModel(500.0, 0.0, 99999999999.0, 0.5));
//rolling ball size
        rollingBallSize = new JSpinner(new SpinnerNumberModel(10.0, 0.0, 99999999.0, 1.0));
        firstSlice = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
        lastSlice = new JSpinner(new SpinnerNumberModel(33, 0, Integer.MAX_VALUE, 1));
    }

    public void setPrefs() {
        Prefs.set("PluginToName.proteinName_" + id, proteinChannel.getText());
        Prefs.set("PluginToName.proteinEnding_" + id, imageEnding.getText());
        Prefs.set("PluginToName.zStackProtein_" + id, isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsProtein_" + id, zProjMethods.getItemAt(zProjMethods.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesProtein_" + id, chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceProtein_" + id, (double) (int) firstSlice.getValue());
        Prefs.set("PluginToName.lastSliceProtein_" + id, (double) (int) lastSlice.getValue());
        Prefs.set("PluginToName.useMacroProtein_" + id, useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroProtein_" + id, macroArea.getText());
        Prefs.set("PluginToName.rollingballSizeProtein_" + id, (double) rollingBallSize.getValue());
        Prefs.set("PluginToName.findMaximaSelectedProtein_" + id, findMaximaCheckBox.isSelected());
        Prefs.set("PluginToName.thresholdSelectedProtein_" + id, thresholdCheckBox.isSelected());
        Prefs.set("PluginToName.prominence_" + id, (double) prominenceValue.getValue());
        Prefs.set("PluginToName.thresholdMethodProtein_" + id, threshMethods.getItemAt(threshMethods.getSelectedIndex()));
        Prefs.set("PluginToName.minSizeSpot_" + id, (double) minSizeSpot.getValue());
    }
}


//seuillage : surface et nombre de spots (mais nombre moins performant)
// prominence : nb de spots


