package GUI;

import Detectors.CellDetector;
import Detectors.NucleiDetector;
import Helpers.ImageToAnalyze;
import Helpers.MeasureCalibration;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.Prefs;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;


//TODO cyto : aire (le reste plus optionnel)
//
public class CytoCellPanel {
    //    GUI
    private JPanel mainPanel;
    private JButton previewButton;

    //    Cytoplasm
    private JPanel chooseFilePanel;
    private JScrollPane imageListScrolling;
    private JList<ImageToAnalyze> imageList;
    private JTextField imageEndingField;
    private JLabel imageEndingLabel;
    private JLabel errorImageEndingLabel;
    private NucleiPanel nucleiPanel;

    //    CellPose parameters
    private JPanel deepLearningPanel;
    private JSpinner cellPoseMinDiameterSpinner;
    private JLabel cellPoseMinDiameterLabel;
    private JLabel cellPoseModelLabel;
    private JComboBox<String> cellPoseModelCombo;
    private JPanel zProjPanel;
    private JCheckBox isAZStackCheckBox;
    private JPanel zStackParameters;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;
    private JCheckBox chooseSlicesToUseCheckBox;
    private JPanel slicesPanel;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;
    private JCheckBox useAMacroCheckBox;
    private JPanel macroPanel;
    private JScrollPane macroAreaScroll;
    private JTextArea macroArea;
    private JCheckBox cellPoseExcludeOnEdgesCheckBox;
    private JCheckBox showBinaryMaskCheckBox;
    private JCheckBox saveCellROIsCheckBox;
    private JCheckBox saveSegmentationMaskCheckBox;
    private JCheckBox showPreprocessingImageCheckBox;

    //    NON GUI
    private final MeasureCalibration measureCalibration;
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages;
    private final boolean showImage;

    public CytoCellPanel(ImageToAnalyze[] imageNames, MeasureCalibration measureCalibration, boolean showImage) {
        this.measureCalibration = measureCalibration;
        this.imagesNames = imageNames;
        this.showImage = showImage;
        this.nucleiPanel = null;
        $$$setupUI$$$();
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            imageListModel.removeElement(null);
            for (ImageToAnalyze ip : imagesNames) {
                imageListModel.addElement(ip);
            }
            imageList.setSelectedIndex(0);
            filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        }

        isAZStackCheckBox.addItemListener(e -> zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        chooseSlicesToUseCheckBox.addItemListener(e -> {
            slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            mainPanel.revalidate();
        });

        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                if (!imageList.isSelectionEmpty()) {

                    ImageToAnalyze imageToPreview = imageList.getSelectedValue();
                    String nameExperiment = getNameExperiment(imageToPreview);
                    CellDetector previewCD = getCytoDetector(imageToPreview, nameExperiment, true);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            previewCD.preview();
                            return null;
                        }
                    };
                    worker.execute();
                } else {
                    IJ.error("Please select an image name.");
                }
            } else {
                IJ.error("There is no image to be used to do a preview.");
            }
        });
        imageEndingField.addActionListener(e -> {
            imageEndingField.setText(imageEndingField.getText().trim());
            filteredImages = ImageToAnalyze.filterModel((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        });
    }

    public void setResultsCheckbox(boolean wantToSave) {
        saveCellROIsCheckBox.setVisible(wantToSave);
        saveSegmentationMaskCheckBox.setVisible(wantToSave);
    }

    public void setNucleiPanel(NucleiPanel nucleiPanel) {
        this.nucleiPanel = nucleiPanel;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private String getNameExperiment(ImageToAnalyze imageToAnalyze) {
        if (imageEndingField.getText().length() == 0) {
            return imageToAnalyze.getImageName();
        } else {
            return imageToAnalyze.getImageName().split(imageEndingField.getText())[0];
        }
    }

    private CellDetector getCytoDetector(ImageToAnalyze imageToAnalyze, String nameExperiment, boolean isPreview) {
        CellDetector cellDetector;
        if (isPreview) {
            cellDetector = new CellDetector(imageToAnalyze.getImagePlus(), nameExperiment, measureCalibration, null, true);
        } else {
            cellDetector = new CellDetector(imageToAnalyze.getImagePlus(), nameExperiment, measureCalibration, imageToAnalyze.getDirectory(), showImage);
        }

        if (nucleiPanel != null) {
            ArrayList<NucleiDetector> nucleiDetectors = nucleiPanel.getImages();
            if (nucleiDetectors != null) {
                for (NucleiDetector nucleiDetector : nucleiDetectors) {
                    if (nucleiDetector.getNameExperiment().equals(nameExperiment)) {
                        cellDetector.setNucleiDetector(nucleiDetector);
                        IJ.log("The cytoplasm image : " + imageToAnalyze.getImageName() + " is associated to the nuclei image :" + nucleiDetector.getImageTitle());
                    }
                }
            } else {
                IJ.log("The analysis is done on the whole cell.");
            }
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                cellDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                cellDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        cellDetector.setDeeplearning((Integer) cellPoseMinDiameterSpinner.getValue(), cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()), cellPoseExcludeOnEdgesCheckBox.isSelected(), showBinaryMaskCheckBox.isSelected());
        if (useAMacroCheckBox.isSelected()) {
            cellDetector.setPreprocessingMacro(macroArea.getText());
        }
        if (saveSegmentationMaskCheckBox.isVisible()) {
            cellDetector.setSavings(saveSegmentationMaskCheckBox.isSelected(), saveCellROIsCheckBox.isSelected());
        }
        return cellDetector;
    }

    public ArrayList<CellDetector> getImages() {
        ArrayList<CellDetector> cyto = new ArrayList<>();
        ImageToAnalyze image;
        String nameExperiment;
        if (!filteredImages) {
            IJ.error("No images given for nuclei. Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = getNameExperiment(image);
                cyto.add(getCytoDetector(image, nameExperiment, false));
            }
            return cyto;
        }
    }


    private void getPreferences() {
        imageEndingField.setText(Prefs.get("PluginToName.CytoEnding", "_w31 FITC"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackCyto", true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("PluginToName.ProjMethodsCyto", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSliceCyto", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        firstSliceSpinner.setValue((int) Prefs.get("PluginToName.firstSliceCyto", 1));
        lastSliceSpinner.setValue((int) Prefs.get("PluginToName.lastSliceCyto", imagesNames[0].getImagePlus().getNSlices()));
        if ((Integer) firstSliceSpinner.getValue() > imagesNames[0].getImagePlus().getNSlices()) {
            firstSliceSpinner.setValue(1);
        }
        if ((Integer) lastSliceSpinner.getValue() > imagesNames[0].getImagePlus().getNSlices()) {
            lastSliceSpinner.setValue(1);
        }
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroCyto", false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroCyto", " ")); /*TODO default macro ?*/
        cellPoseModelCombo.setSelectedItem(Prefs.get("PluginToName.cellposeCytoMethods", "cyto2"));
        cellPoseMinDiameterSpinner.setValue((int) Prefs.get("PluginToName.cellposeCytoMinDiameter", 100));
        cellPoseExcludeOnEdgesCheckBox.setSelected(Prefs.get("PluginToName.cellposeCytoExcludeOnEdges", true));
    }

    public void setPreferences() {
        Prefs.set("PluginToName.CytoEnding", imageEndingField.getText());
        Prefs.set("PluginToName.zStackCyto", isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsCyto", zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSliceCyto", chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceCyto", (double) (int) firstSliceSpinner.getValue());
        Prefs.set("PluginToName.lastSliceCyto", (double) (int) lastSliceSpinner.getValue());
        Prefs.set("PluginToName.useMacroCyto", useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroCyto", macroArea.getText());
        Prefs.set("PluginToName.cellposeCytoMethods", cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
        Prefs.set("PluginToName.cellposeCytoMinDiameter", (double) (int) cellPoseMinDiameterSpinner.getValue());
        Prefs.set("PluginToName.cellposeCytoExcludeOnEdges", cellPoseExcludeOnEdgesCheckBox.isSelected());
    }

    private void createUIComponents() {
        imageListModel.addElement(null);
        imageList = new JList<>(imageListModel);


// minSize spinner
        cellPoseMinDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 100));

//  Text field to filter extension
        imageEndingField = new JTextField(15);
        firstSliceSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
        lastSliceSpinner = new JSpinner(new SpinnerNumberModel(33, 0, 9999, 1));
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
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Detect cytoplasm", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deepLearningPanel = new JPanel();
        deepLearningPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(deepLearningPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deepLearningPanel.setBorder(BorderFactory.createTitledBorder(null, "Deep learning parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deepLearningPanel.add(cellPoseMinDiameterSpinner, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseMinDiameterLabel = new JLabel();
        cellPoseMinDiameterLabel.setText("Minimum diameter of cell (pixel)");
        deepLearningPanel.add(cellPoseMinDiameterLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelLabel = new JLabel();
        cellPoseModelLabel.setText("Model for Cellpose");
        deepLearningPanel.add(cellPoseModelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("cyto");
        defaultComboBoxModel1.addElement("cyto2");
        cellPoseModelCombo.setModel(defaultComboBoxModel1);
        deepLearningPanel.add(cellPoseModelCombo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseExcludeOnEdgesCheckBox = new JCheckBox();
        cellPoseExcludeOnEdgesCheckBox.setSelected(true);
        cellPoseExcludeOnEdgesCheckBox.setText("Exclude on edges");
        deepLearningPanel.add(cellPoseExcludeOnEdgesCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showBinaryMaskCheckBox = new JCheckBox();
        showBinaryMaskCheckBox.setText("Show segmentation image");
        deepLearningPanel.add(showBinaryMaskCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveCellROIsCheckBox = new JCheckBox();
        saveCellROIsCheckBox.setText("Save cell (and cytoplasm) ROIs");
        deepLearningPanel.add(saveCellROIsCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveSegmentationMaskCheckBox = new JCheckBox();
        saveSegmentationMaskCheckBox.setText("Save segmentation mask");
        deepLearningPanel.add(saveSegmentationMaskCheckBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        previewButton = new JButton();
        previewButton.setText("Preview");
        mainPanel.add(previewButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        zProjPanel = new JPanel();
        zProjPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
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
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        defaultComboBoxModel2.addElement("Maximum projection");
        defaultComboBoxModel2.addElement("Standard Deviation projection");
        zProjMethodsCombo.setModel(defaultComboBoxModel2);
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
        final Spacer spacer1 = new Spacer();
        zProjPanel.add(spacer1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        showPreprocessingImageCheckBox = new JCheckBox();
        showPreprocessingImageCheckBox.setText("Show preprocessing image");
        zProjPanel.add(showPreprocessingImageCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseFilePanel = new JPanel();
        chooseFilePanel.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(chooseFilePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        chooseFilePanel.setBorder(BorderFactory.createTitledBorder(null, "Choice of cytoplasm image", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        imageListScrolling = new JScrollPane();
        chooseFilePanel.add(imageListScrolling, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        imageListScrolling.setViewportView(imageList);
        imageEndingLabel = new JLabel();
        imageEndingLabel.setText("Image ending without extension");
        chooseFilePanel.add(imageEndingLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageEndingField.setText("");
        chooseFilePanel.add(imageEndingField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        errorImageEndingLabel = new JLabel();
        errorImageEndingLabel.setText("No image corresponding to ending.");
        chooseFilePanel.add(errorImageEndingLabel, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        chooseFilePanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}