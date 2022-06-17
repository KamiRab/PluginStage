package GUI;

import Detectors.SpotDetector;
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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SpotPanel {
    private final int id;
    //    GUI COMPONENTS
    private JPanel mainPanel;
    //    Image selection
    private JPanel chooseFilePanel;
    private JLabel spotChannelLabel;
    private JTextField spotChannelField;
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
    private JCheckBox useFindMaximaCheckBox;
    private JCheckBox useThresholdCheckBox;
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
    private JCheckBox useWatershedCheckBox;
    private JCheckBox showBinaryMaskCheckBox;
    private JCheckBox showMaximaImageCheckBox;
    private JCheckBox saveMaximaImageCheckBox;
    private JCheckBox saveMaximaROIsCheckBox;
    private JCheckBox saveThresholdROIsCheckBox;
    private JCheckBox saveBinaryMaskCheckBox;
    private JCheckBox showPreprocessingImageCheckBox;

    //    NON GUI
    private final ImageToAnalyze[] imagesNames;
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages;
    private final MeasureCalibration measureCalibration;
    private final boolean showImages;

    //    TODO mean and integrated density always (even if no method)
    public SpotPanel(ImageToAnalyze[] ipList, MeasureCalibration measureCalibration, int id, boolean showImages) {
        $$$setupUI$$$();
        this.measureCalibration = measureCalibration;
        imagesNames = ipList;
        this.id = id;
        this.showImages = showImages;

//        GET PREFERENCES
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            imageListModel.removeElement(null);
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                imageListModel.addElement(imagePlusDisplay);
            }
            filteredImages = ImageToAnalyze.filterModelbyEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
            imageList.setSelectedIndex(0);
        }
//        ITEM LISTENERS
        isAZStackCheckBox.addItemListener(e -> zStackParametersPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        useAMacroCheckBox.addItemListener(e -> macroPannel.setVisible(e.getStateChange() == ItemEvent.SELECTED));
        chooseSlicesToUseCheckBox.addItemListener(e -> slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        useFindMaximaCheckBox.addItemListener(e -> maximaPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        useThresholdCheckBox.addItemListener(e -> thresholdPanel.setEnabled(e.getStateChange() == ItemEvent.SELECTED));
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                if (!imageList.isSelectionEmpty()) {
                    ImageToAnalyze imageToPreview = imageList.getSelectedValue();
                    String nameExperiment = imageToPreview.getImagePlus().getTitle().split(imageEndingField.getText())[0];
                    String spotName = spotChannelField.getText();
                    SpotDetector previewPD = getSpotDetector(imageToPreview, spotName, nameExperiment, true);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            previewPD.preview();
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
            filteredImages = ImageToAnalyze.filterModelbyEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        });
    }

    public void setResultsCheckbox(boolean wantToSave) {
        saveMaximaImageCheckBox.setVisible(wantToSave);
        saveMaximaROIsCheckBox.setVisible(wantToSave);
        saveBinaryMaskCheckBox.setVisible(wantToSave);
        saveThresholdROIsCheckBox.setVisible(wantToSave);
    }

    private SpotDetector getSpotDetector(ImageToAnalyze firstImage, String spotName, String nameExperiment, boolean isPreview) {
        SpotDetector spotDetector;
        if (isPreview) {
            spotDetector = new SpotDetector(firstImage.getImagePlus(), spotName, nameExperiment, measureCalibration, null, true);
        } else {
            spotDetector = new SpotDetector(firstImage.getImagePlus(), spotName, nameExperiment, measureCalibration, firstImage.getDirectory(), showPreprocessingImageCheckBox.isSelected());
        }
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                spotDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                spotDetector.setzStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
        if (useRollingBallCheckBox.isSelected()) {
            spotDetector.setRollingBallSize((double) rollingBallSizeSpinner.getValue());
        }
        if (useFindMaximaCheckBox.isSelected()) {
            spotDetector.setSpotByfindMaxima((double) prominenceSpinner.getValue(), showMaximaImageCheckBox.isSelected());
            spotDetector.setMaximaSaving(saveMaximaImageCheckBox.isSelected(), saveMaximaROIsCheckBox.isSelected());
        }
        if (useThresholdCheckBox.isSelected()) {
            spotDetector.setSpotByThreshold(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()), (double) minSizeSpotSpinner.getValue(), useWatershedCheckBox.isSelected(), showBinaryMaskCheckBox.isSelected());
            spotDetector.setThresholdSaving(saveBinaryMaskCheckBox.isSelected(), saveThresholdROIsCheckBox.isSelected());
        }
        if (useAMacroCheckBox.isSelected()) {
            spotDetector.setPreprocessingMacro(macroArea.getText());
        }

        return spotDetector;
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public ArrayList<SpotDetector> getImages() {
        ArrayList<SpotDetector> selectedSpots = new ArrayList<>();
        ImageToAnalyze image;
        String nameExperiment;
        String spotName = spotChannelField.getText();
        /*if no image selected*/
        if (!filteredImages) {
            IJ.error("No images given for spot " + (id + 1) + ". Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            addParametersToFile();
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = image.getImagePlus().getTitle().split(imageEndingField.getText())[0];
                selectedSpots.add(getSpotDetector(image, spotName, nameExperiment, false));
            }
            return selectedSpots;
        }
    }

    private void addParametersToFile() {
        String directory = imageListModel.getElementAt(0).getDirectory();
        if (directory != null) {
            String parameterFilename = directory + "/Results/Parameters.txt";
            try {
                FileWriter fileWriter = new FileWriter(parameterFilename, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.append("\nSPOT ").append(spotChannelField.getText()).append(" Parameters");
                if (useFindMaximaCheckBox.isSelected()) {
                    bufferedWriter.append("\nFind by maxima parameters:");
                    bufferedWriter.append("\nProminence :").append(String.valueOf(prominenceSpinner.getValue()));
                }
                if (useThresholdCheckBox.isSelected()) {
                    bufferedWriter.append("\nThresholding parameters : ");
                    bufferedWriter.append("\nThreshold method :").append(threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
                    bufferedWriter.append("\nMinimum spot diameter :").append(String.valueOf(minSizeSpotSpinner.getValue()));
                    bufferedWriter.append("\nThresholding watershed ").append(useWatershedCheckBox.isSelected() ? "yes" : "no");
                }
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                IJ.log("The parameters could not be written.");
            }
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
        imageList.setEnabled(true);
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
        zProjPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(zProjPanel, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        zProjPanel.setBorder(BorderFactory.createTitledBorder(null, "Preprocessing", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        isAZStackCheckBox = new JCheckBox();
        isAZStackCheckBox.setSelected(true);
        isAZStackCheckBox.setText("Is a z-stack ?");
        zProjPanel.add(isAZStackCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useAMacroCheckBox = new JCheckBox();
        useAMacroCheckBox.setText("Use macro code");
        zProjPanel.add(useAMacroCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        zProjPanel.add(macroPannel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        macroAreaScroll = new JScrollPane();
        macroPannel.add(macroAreaScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        macroArea = new JTextArea();
        macroArea.setRows(2);
        macroAreaScroll.setViewportView(macroArea);
        final Spacer spacer2 = new Spacer();
        zProjPanel.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 50), null, null, 0, false));
        useRollingBallCheckBox = new JCheckBox();
        useRollingBallCheckBox.setText("Substract background");
        zProjPanel.add(useRollingBallCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        zProjPanel.add(panel1, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Size of rolling ball");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel1.add(rollingBallSizeSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showPreprocessingImageCheckBox = new JCheckBox();
        showPreprocessingImageCheckBox.setText("Show preprocessing image");
        zProjPanel.add(showPreprocessingImageCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        maximaPanel.setLayout(new GridLayoutManager(3, 3, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(maximaPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        maximaPanel.setBorder(BorderFactory.createTitledBorder(null, "Find maxima method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        prominenceLabel = new JLabel();
        prominenceLabel.setText("Prominence");
        maximaPanel.add(prominenceLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maximaPanel.add(prominenceSpinner, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showMaximaImageCheckBox = new JCheckBox();
        showMaximaImageCheckBox.setText("Show maxima image");
        maximaPanel.add(showMaximaImageCheckBox, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveMaximaImageCheckBox = new JCheckBox();
        saveMaximaImageCheckBox.setText("Save maxima image");
        maximaPanel.add(saveMaximaImageCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveMaximaROIsCheckBox = new JCheckBox();
        saveMaximaROIsCheckBox.setText("Save maxima ROIs");
        maximaPanel.add(saveMaximaROIsCheckBox, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        choiceMethodPanel.add(thresholdPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder(null, "Threshold method", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        minSizeSpotLabel = new JLabel();
        minSizeSpotLabel.setText("Minimum size of spot");
        thresholdPanel.add(minSizeSpotLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(minSizeSpotSpinner, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        threshMethodsLabel = new JLabel();
        threshMethodsLabel.setText("Threshold Method");
        thresholdPanel.add(threshMethodsLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showBinaryMaskCheckBox = new JCheckBox();
        showBinaryMaskCheckBox.setText("Show binary mask");
        thresholdPanel.add(showBinaryMaskCheckBox, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        thresholdPanel.add(threshMethodsCombo, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        useWatershedCheckBox = new JCheckBox();
        useWatershedCheckBox.setText("Use watershed");
        thresholdPanel.add(useWatershedCheckBox, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveThresholdROIsCheckBox = new JCheckBox();
        saveThresholdROIsCheckBox.setText("Save threshold ROIs");
        thresholdPanel.add(saveThresholdROIsCheckBox, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveBinaryMaskCheckBox = new JCheckBox();
        saveBinaryMaskCheckBox.setText("Save segmentation mask");
        thresholdPanel.add(saveBinaryMaskCheckBox, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useFindMaximaCheckBox = new JCheckBox();
        useFindMaximaCheckBox.setSelected(true);
        useFindMaximaCheckBox.setText("Find maxima");
        choiceMethodPanel.add(useFindMaximaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useThresholdCheckBox = new JCheckBox();
        useThresholdCheckBox.setSelected(true);
        useThresholdCheckBox.setText("Threshold");
        choiceMethodPanel.add(useThresholdCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        spotChannelField = new JTextField();
        spotChannelField.setText("");
        mainPanel.add(spotChannelField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        spotChannelLabel = new JLabel();
        spotChannelLabel.setText("Name of protein");
        mainPanel.add(spotChannelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    //PREFERENCES

    /**
     * Get preferences from ImageJ/Fiji file
     */
    private void getPreferences() {
        spotChannelField.setText(Prefs.get("PluginToName.spotName_" + id, "CY5"));
        imageEndingField.setText(Prefs.get("PluginToName.spotEnding_" + id, "_w11 CY5"));
        isAZStackCheckBox.setSelected(Prefs.get("PluginToName.zStackSpot_" + id, true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("PluginToName.ProjMethodsSpot_" + id, "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("PluginToName.chooseSlidesSpot_" + id, false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        firstSliceSpinner.setValue((int) Prefs.get("PluginToName.firstSliceSpot_" + id, 1));
        lastSliceSpinner.setValue((int) Prefs.get("PluginToName.lastSliceSpot_" + id, imagesNames[0].getImagePlus().getNSlices()));
        useAMacroCheckBox.setSelected(Prefs.get("PluginToName.useMacroSpot_" + id, false));
        if (!useAMacroCheckBox.isSelected()) macroPannel.setVisible(false);
        macroArea.append(Prefs.get("PluginToName.macroSpot_" + id, " ")); /*TODO default macro ?*/
        useRollingBallCheckBox.setSelected(Prefs.get("PluginToName.rollingballCheckSpot_" + id, true));
        rollingBallSizeSpinner.setValue(Prefs.get("PluginToName.rollingballSizeSpot_" + id, 10));
        useFindMaximaCheckBox.setSelected(Prefs.get("PluginToName.findMaximaSelectedSpot_" + id, true));
        maximaPanel.setEnabled(useFindMaximaCheckBox.isSelected());
        useThresholdCheckBox.setSelected(Prefs.get("PluginToName.thresholdSelectedSpot_" + id, false));
        thresholdPanel.setEnabled(useThresholdCheckBox.isSelected());
        prominenceSpinner.setValue(Prefs.get("PluginToName.prominence_" + id, 500));
        threshMethodsCombo.setSelectedItem(Prefs.get("PluginToName.thresholdMethodSpot_" + id, "Li"));
        minSizeSpotSpinner.setValue(Prefs.get("PluginToName.minSizeSpot_" + id, 10));
        useWatershedCheckBox.setSelected(Prefs.get("PluginToName.useWatershedSpot_" + id, false));
    }

    /**
     * Set preferences for ImageJ/Fiji
     */
    public void setPreferences() {
        Prefs.set("PluginToName.spotName_" + id, spotChannelField.getText());
        Prefs.set("PluginToName.spotEnding_" + id, imageEndingField.getText());
        Prefs.set("PluginToName.zStackSpot_" + id, isAZStackCheckBox.isSelected());
        Prefs.set("PluginToName.ProjMethodsSpot_" + id, zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.chooseSlidesSpot_" + id, chooseSlicesToUseCheckBox.isSelected());
        Prefs.set("PluginToName.firstSliceSpot_" + id, (double) (int) firstSliceSpinner.getValue());
        Prefs.set("PluginToName.lastSliceSpot_" + id, (double) (int) lastSliceSpinner.getValue());
        Prefs.set("PluginToName.useMacroSpot_" + id, useAMacroCheckBox.isSelected());
        Prefs.set("PluginToName.macroSpot_" + id, macroArea.getText());
        Prefs.set("PluginToName.rollingballCheckSpot_" + id, useRollingBallCheckBox.isSelected());
        Prefs.set("PluginToName.rollingballSizeSpot_" + id, (double) rollingBallSizeSpinner.getValue());
        Prefs.set("PluginToName.findMaximaSelectedSpot_" + id, useFindMaximaCheckBox.isSelected());
        Prefs.set("PluginToName.thresholdSelectedSpot_" + id, useThresholdCheckBox.isSelected());
        Prefs.set("PluginToName.prominence_" + id, (double) prominenceSpinner.getValue());
        Prefs.set("PluginToName.thresholdMethodSpot_" + id, threshMethodsCombo.getItemAt(threshMethodsCombo.getSelectedIndex()));
        Prefs.set("PluginToName.minSizeSpot_" + id, (double) minSizeSpotSpinner.getValue());
        Prefs.set("PluginToName.useWatershedSpot_" + id, useWatershedCheckBox.isSelected());
    }
}


//seuillage : surface et nombre de spots (mais nombre moins performant)
// prominence : nb de spots


