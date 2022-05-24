package GUI;

import Detectors.NucleiDetector;
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

public class NucleiPanel extends JPanel {
    //    GUI
    private JPanel mainPanel;
    private JButton previewButton;

    //    Choose file by extension
    private JPanel chooseFilePanel;
    private JScrollPane imageListScrolling;
    private JList<ImageToAnalyze> imageList;
    private JTextField imageEndingField;
    private JLabel endingLabel;
    private JLabel errorImageEndingLabel;

    //    Preprocessing : Z-stack
    private JPanel zProjPanel;
    private JCheckBox isAZStackCheckBox;
    // Preprocessing : Z-stack general parameters
    private JPanel zStackParameters;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;
    private JCheckBox chooseSlicesToUseCheckBox;
    //Preprocessing : Z-stack slice choice
    private JPanel slicesPanel;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;

    //    Preprocessing : Macro
    private JPanel macroPanel;
    private JScrollPane macroAreaScroll;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;

    //    Choice of segmentation method
    private JPanel methodPanel;
    private JLabel segmentationMethodsLabel;
    private JRadioButton thresholdRadioButton;
    private JRadioButton deepLearningRadioButton;

    //    Thresholding parameters
    private JPanel thresholdingParametersPanel;
    private JLabel threshMethodsLabel;
    private JComboBox<String> threshMethodsCombo;
    private JLabel minSizeNucleusLabel;
    private JSpinner minSizeNucleusSpinner;
    //  Thresholding parameters : supplementary options
    private JCheckBox useWatershedCheckBox;
    private JCheckBox thresholdExcludeOnEdgesCheckBox;
    private JCheckBox finalValidationCheckBox;
    private JPanel deepLearningPanel;
    private JComboBox<String> cellPoseModelCombo;
    private JSpinner cellPoseMinDiameterSpinner;
    private JLabel cellPoseMinDiameterLabel;
    private JLabel cellPoseModelLabel;
    private JCheckBox cellPoseExcludeOnEdgesCheckBox;

    //    NON GUI
    private final MeasureCalibration measureCalibration;
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages;
    private final boolean showImage;

//    TODO attention message d'erreur si mauvaise concordance
//    TODO get Last substring index
//    TODO faire finalValidation (selection noyau + coupe noyau)
//    TODO Rajouter cellpose en tant que classe au lieu de run (rajouter JAR)
//    TODO label2roi

    /**
     * @param imageNames
     * @param measureCalibration
     * @param showImage
     */
    public NucleiPanel(ImageToAnalyze[] imageNames, MeasureCalibration measureCalibration, boolean showImage) {
        $$$setupUI$$$();
        this.measureCalibration = measureCalibration;
        this.imagesNames = imageNames;
        this.showImage = showImage;
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            for (ImageToAnalyze ip : imagesNames) {
                imageListModel.removeElement(null);
                imageListModel.addElement(ip);
            }
        }
        filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
//        filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText());

//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        deepLearningRadioButton.addItemListener(e -> {
            deepLearningPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            thresholdingParametersPanel.setVisible(e.getStateChange() == ItemEvent.DESELECTED);
        });

        chooseSlicesToUseCheckBox.addItemListener(e -> {
            slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            mainPanel.revalidate();
        });
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                ImageToAnalyze firstImage = imageListModel.firstElement();
                String nameExperiment = getNameExperiment(firstImage);
                NucleiDetector previewND = getNucleiDetector(firstImage, nameExperiment, true);
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
        cellPoseExcludeOnEdgesCheckBox.addItemListener(e -> thresholdExcludeOnEdgesCheckBox.setSelected(e.getStateChange() == ItemEvent.SELECTED));
//        imageEndingField.addActionListener(e -> filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText()));
        imageEndingField.addActionListener(e -> filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel));
    }

    private String getNameExperiment(ImageToAnalyze imageToAnalyze) {
        if (imageEndingField.getText().length() == 0) {
            return ImageToAnalyze.name_without_extension(imageToAnalyze.getImageName());
        } else {
            return imageToAnalyze.getImageName().split(imageEndingField.getText())[0];
        }
    }

    private NucleiDetector getNucleiDetector(ImageToAnalyze firstImage, String nameExperiment, boolean isPreview) {
        NucleiDetector nucleiDetector;
        if (isPreview) {
            nucleiDetector = new NucleiDetector(firstImage.getImagePlus(), nameExperiment, measureCalibration, null, true);
        } else {
            nucleiDetector = new NucleiDetector(firstImage.getImagePlus(), nameExperiment, measureCalibration, firstImage.getDirectory(), showImage);
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                nucleiDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                nucleiDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        if (deepLearningRadioButton.isSelected()) {/*TODO*/
            nucleiDetector.setDeeplearning((Integer) cellPoseMinDiameterSpinner.getValue(), cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()), cellPoseExcludeOnEdgesCheckBox.isSelected());
        } else {
            nucleiDetector.setThresholdMethod(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()), (Double) minSizeNucleusSpinner.getValue(), useWatershedCheckBox.isSelected(), thresholdExcludeOnEdgesCheckBox.isSelected(), finalValidationCheckBox.isSelected());
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
        String nameExperiment;
        if (!filteredImages) {
            IJ.error("No images given for nuclei. Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = getNameExperiment(image);
                nuclei.add(getNucleiDetector(image, nameExperiment, false));
            }
            return nuclei;
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
        mainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Detect nuclei", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        thresholdingParametersPanel = new JPanel();
        thresholdingParametersPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(thresholdingParametersPanel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdingParametersPanel.setBorder(BorderFactory.createTitledBorder(null, "Thresholding parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold method");
        thresholdingParametersPanel.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minSizeNucleusLabel = new JLabel();
        minSizeNucleusLabel.setText("Minimum size of nucleus");
        thresholdingParametersPanel.add(minSizeNucleusLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingParametersPanel.add(minSizeNucleusSpinner, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        thresholdingParametersPanel.add(useWatershedCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdingParametersPanel.add(threshMethodsCombo, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdExcludeOnEdgesCheckBox = new JCheckBox();
        thresholdExcludeOnEdgesCheckBox.setSelected(true);
        thresholdExcludeOnEdgesCheckBox.setText("Exclude on edges");
        thresholdingParametersPanel.add(thresholdExcludeOnEdgesCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        finalValidationCheckBox = new JCheckBox();
        finalValidationCheckBox.setText("Final validation");
        thresholdingParametersPanel.add(finalValidationCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        chooseFilePanel.add(imageEndingField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEndingLabel = new JLabel();
        errorImageEndingLabel.setText("No image corresponding to ending");
        chooseFilePanel.add(errorImageEndingLabel, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        zProjMethodsCombo = new JComboBox();
        zProjMethodsCombo.setEnabled(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Maximum projection");
        defaultComboBoxModel1.addElement("Standard Deviation projection");
        zProjMethodsCombo.setModel(defaultComboBoxModel1);
        zStackParameters.add(zProjMethodsCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseSlicesToUseCheckBox = new JCheckBox();
        chooseSlicesToUseCheckBox.setText("Choose slices to use");
        zStackParameters.add(chooseSlicesToUseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel = new JPanel();
        slicesPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zStackParameters.add(slicesPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        slicesPanel.add(firstSliceSpinner, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        slicesPanel.add(lastSliceSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use macro code");
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
        deepLearningPanel = new JPanel();
        deepLearningPanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(deepLearningPanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deepLearningPanel.setBorder(BorderFactory.createTitledBorder(null, "Deep learning parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deepLearningPanel.add(cellPoseMinDiameterSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseMinDiameterLabel = new JLabel();
        cellPoseMinDiameterLabel.setText("Minimum diameter of nuclei (pixel)");
        deepLearningPanel.add(cellPoseMinDiameterLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelLabel = new JLabel();
        cellPoseModelLabel.setText("Model for Cellpose");
        deepLearningPanel.add(cellPoseModelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("cyto");
        defaultComboBoxModel2.addElement("cyto2");
        defaultComboBoxModel2.addElement("nuclei");
        cellPoseModelCombo.setModel(defaultComboBoxModel2);
        deepLearningPanel.add(cellPoseModelCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseExcludeOnEdgesCheckBox = new JCheckBox();
        cellPoseExcludeOnEdgesCheckBox.setSelected(true);
        cellPoseExcludeOnEdgesCheckBox.setText("Exclude on edges");
        deepLearningPanel.add(cellPoseExcludeOnEdgesCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        mainPanel.add(previewButton, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        imageListModel.addElement(null);
        imageList = new JList<>(imageListModel);


// List of methods for threshold
        threshMethodsCombo = new JComboBox<>(AutoThresholder.getMethods());

// minSize spinner
        minSizeNucleusSpinner = new JSpinner(new SpinnerNumberModel(1000.0, 0.0, Integer.MAX_VALUE, 100.0));
        cellPoseMinDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 100));

//  Text field to filter extension
        imageEndingField = new JTextField(15);
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
    }

    private void getPreferences() {
        imageEndingField.setText(Prefs.get("PluginToName.nucleusEnding", "_w31 DAPI 405"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackNucleus", true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("PluginToName.ProjMethodsNucleus", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSlidesNucleus", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        firstSliceSpinner.setValue((int) Prefs.get("PluginToName.firstSliceNucleus", 1));
        lastSliceSpinner.setValue((int) Prefs.get("PluginToName.lastSliceNucleus", imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroNucleus", false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroNucleus", " ")); /*TODO default macro ?*/
        deepLearningRadioButton.setSelected(Prefs.get("PluginToName.useDeepLearningNucleus", false));
        if (deepLearningRadioButton.isSelected()) thresholdingParametersPanel.setVisible(false);
        else deepLearningPanel.setVisible(false);
        threshMethodsCombo.setSelectedItem(Prefs.get("PluginToName.thresholdMethodNucleus", "Li"));
        minSizeNucleusSpinner.setValue(Prefs.get("PluginToName.minSizeNucleus", 1000));
        useWatershedCheckBox.setSelected(Prefs.get("PluginToName.useWaterShed", false));
        thresholdExcludeOnEdgesCheckBox.setSelected(Prefs.get("PluginToName.thresholdExcludeOnEdges", true));
        cellPoseExcludeOnEdgesCheckBox.setSelected(Prefs.get("PluginToName.cellposeNucleusExcludeOnEdges", true));
        cellPoseModelCombo.setSelectedItem(Prefs.get("PluginToName.cellposeNucleusMethods", "cyto2"));
        cellPoseMinDiameterSpinner.setValue((int) Prefs.get("PluginToName.cellposeNucleusMinDiameter", 100));
        finalValidationCheckBox.setSelected(Prefs.get("PluginToName.finalValidation", false));
    }

    public void setPreferences() {
        Prefs.set("PluginToName.nucleusEnding", imageEndingField.getText());
        Prefs.set("PluginToName.zStackNucleus", isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsNucleus", zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesNucleus", chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceNucleus", (double) (int) firstSliceSpinner.getValue());
        Prefs.set("PluginToName.lastSliceNucleus", (double) (int) lastSliceSpinner.getValue());
        Prefs.set("PluginToName.useMacroNucleus", useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroNucleus", macroArea.getText());
        Prefs.set("PluginToName.useDeepLearningNucleus", deepLearningRadioButton.isSelected());
        Prefs.set("PluginToName.thresholdMethodNucleus", threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.minSizeNucleus", (double) minSizeNucleusSpinner.getValue());
        Prefs.set("PluginToName.useWaterShed", useWatershedCheckBox.isSelected());
        Prefs.set("PluginToName.thresholdExcludeOnEdges", thresholdExcludeOnEdgesCheckBox.isSelected());
        Prefs.set("PluginToName.cellposeNucleusExcludeOnEdges", cellPoseExcludeOnEdgesCheckBox.isSelected());
        Prefs.set("PluginToName.cellposeNucleusMethods", cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
        Prefs.set("PluginToName.cellposeNucleusMinDiameter", (double) (int) cellPoseMinDiameterSpinner.getValue());
        Prefs.set("PluginToName.finalValidation", finalValidationCheckBox.isSelected());
    }
}
