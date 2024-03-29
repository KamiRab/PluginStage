package gui;

import detectors.CellDetector;
import detectors.NucleiDetector;
import helpers.ImageToAnalyze;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import ij.IJ;
import ij.Prefs;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Author : Camille RABIER
 * Date : 16/05/2022
 * GUI Class for
 * - defining parameters to use for cell and cyto
 */
// CONSTRUCTOR
public class CytoCellPanel {
    //    GUI
    private JPanel mainPanel;
    private JButton previewButton;

    //    Choose file by extension
    private JPanel chooseFilePanel;
    private JScrollPane imageListScrolling;
    private JList<ImageToAnalyze> imageList;
    private JTextField imageEndingField;
    private JLabel imageEndingLabel;
    private JLabel errorImageEndingLabel;

    //    Preprocessing : Z-Stack
    private JPanel zProjPanel;
    private JCheckBox isAZStackCheckBox;

    //    Preprocessing : Z-Stack general parameters
    private JPanel zStackParameters;
    private JLabel zProjMethodsLabel;
    private JComboBox<String> zProjMethodsCombo;
    private JCheckBox chooseSlicesToUseCheckBox;
    //    Preprocessing : Z-Stack slice choice
    private JPanel slicesPanel;
    private JSpinner firstSliceSpinner;
    private JSpinner lastSliceSpinner;

    //    Preprocessing : Macro
    private JPanel macroPanel;
    private JScrollPane macroAreaScroll;
    private JCheckBox useAMacroCheckBox;
    private JTextArea macroArea;
    //    CELLPOSE
    private JPanel deepLearningPanel;
    private JComboBox<String> cellPoseModelCombo;
    private JSpinner cellPoseMinDiameterSpinner;
    private JLabel cellPoseMinDiameterLabel;
    private JLabel cellPoseModelLabel;
    private JCheckBox cellPoseExcludeOnEdgesCheckBox;
    //    CELLPOSE : OWN MODEL
    private JPanel ownModelPanel;
    private JLabel modelPathLabel;
    private JTextField modelPathField;
    private JButton modelBrowseButton;


    //    SAVE & SHOW
    private JCheckBox showBinaryMaskCheckBox;
    private JCheckBox saveCellROIsCheckBox;
    private JCheckBox saveSegmentationMaskCheckBox;
    private JCheckBox showPreprocessingImageCheckBox;
    private JCheckBox showCompositeImageCheckBox;

    //    CYTOPLASM PARAMETERS
    private JPanel cytoParametersPanel;
    private JLabel minOverlapLabel;
    private JSpinner minOverlapSpinner;
    private JLabel minCytoSizeLabel;
    private JSpinner minCytoSizeSpinner;
    private JCheckBox finalValidationCheckBox;

    //    NON GUI
    private NucleiPanel nucleiPanel; /*Panel of nuclei to create association between cells and nuclei*/
    private final ImageToAnalyze[] imagesNames;
    private final boolean fromDirectory; /*True if image from directory*/
    private final DefaultListModel<ImageToAnalyze> imageListModel = new DefaultListModel<>();
    private boolean filteredImages; /*true if there are filtered image*/
    private int measurements;
    private File cellPoseModelPath;

    public CytoCellPanel(ImageToAnalyze[] imageNames, boolean fromDirectory) {
        this.imagesNames = imageNames;
        this.fromDirectory = fromDirectory;
        this.nucleiPanel = null;
        $$$setupUI$$$();
        getPreferences();
        //        List of images
        if (imagesNames != null) {
            imageListModel.removeElement(null);
            for (ImageToAnalyze ip : imagesNames) {
                imageListModel.addElement(ip);
            }
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
            imageList.setSelectedIndex(0);
        }
        imageEndingField.addActionListener(e -> {
            imageEndingField.setText(imageEndingField.getText().trim());
            /*trim removes extra spaces*/
            filteredImages = ImageToAnalyze.filterModelByEnding((DefaultListModel<ImageToAnalyze>) imageList.getModel(), imageEndingField.getText(), imagesNames, errorImageEndingLabel);
        });

//        ITEM LISTENERS : Add/Remove element of panel according to choice
        isAZStackCheckBox.addItemListener(e -> zStackParameters.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        chooseSlicesToUseCheckBox.addItemListener(e -> {
            slicesPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED);
            mainPanel.revalidate();
        });

        useAMacroCheckBox.addItemListener(e -> macroPanel.setVisible(e.getStateChange() == ItemEvent.SELECTED));

        cellPoseModelCombo.addItemListener(e -> {
            String modelSelected = (String) cellPoseModelCombo.getSelectedItem();
            ownModelPanel.setVisible(modelSelected.equals("own_model"));
        });

//        BUTTON : preview
        previewButton.addActionListener(e -> {
            if (imageListModel.getSize() > 0) {
                if (!imageList.isSelectionEmpty()) {

                    ImageToAnalyze imageToPreview = imageList.getSelectedValue();
                    String nameExperiment = ImageToAnalyze.getNameExperiment(imageToPreview, imageEndingField);
                    CellDetector previewCD = getCellDetector(imageToPreview, nameExperiment, true);
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

//        BUTTON TO CHOOSE CELLPOSE MODEL PATH
        modelBrowseButton.addActionListener(e -> {
            cellPoseModelPath = chooseCellposeModelFile(this.mainPanel);
            if (cellPoseModelPath != null) { /*cellpose model path chosen*/
                String path = cellPoseModelPath.getAbsolutePath();
                if (path.split("\\\\").length > 2) { /*for visibility shorten the path to display*/
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    modelPathField.setText("..." + path_shorten);
                } else if (path.split("/").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("/")).lastIndexOf("/"));
                    modelPathField.setText("..." + path_shorten);
                } else {
                    modelPathField.setText(path);
                }
            }
        });
    }

//    SETTERS

    /**
     * @param measurements : value corresponding to the measurements to do (bitwise operator)
     */
    public void setMeasurements(int measurements) {
        this.measurements = measurements;
    }

    /**
     * Associate nuclei to cell and show options for cytoplasm if needed
     *
     * @param nucleiPanel : panel containing nuclei image to associate
     */
    public void setNucleiPanel(NucleiPanel nucleiPanel) {
        this.nucleiPanel = nucleiPanel;
        cytoParametersPanel.setVisible(nucleiPanel != null);
    }

//    GETTERS

    /**
     * @param imageToAnalyze : image
     * @param nameExperiment : name without channel info
     * @param isPreview      : if true, will not do measurements and always show images
     * @return Detector object associated to image corresponding to cell channel
     */
    private CellDetector getCellDetector(ImageToAnalyze imageToAnalyze, String nameExperiment, boolean isPreview) {
        CellDetector cellDetector;
        if (isPreview) {
            cellDetector = new CellDetector(imageToAnalyze.getImagePlus(), nameExperiment, null, true, true, true);
        } else {
            cellDetector = new CellDetector(imageToAnalyze.getImagePlus(), nameExperiment, imageToAnalyze.getDirectory(), showPreprocessingImageCheckBox.isSelected(), showBinaryMaskCheckBox.isSelected(), showCompositeImageCheckBox.isSelected());
            cellDetector.setMeasurements(measurements);
        }

//        Cytoplasm ?
        if (nucleiPanel != null) { /*cytoplasm measurement will be done*/
            ArrayList<NucleiDetector> nucleiDetectors = nucleiPanel.getImages(false);
            if (nucleiDetectors != null) {
                cellDetector.setCytoplasmParameters((Double) minOverlapSpinner.getValue(), (Double) minCytoSizeSpinner.getValue());
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

//        Projection ?
        if (isAZStackCheckBox.isSelected()) {
            if (chooseSlicesToUseCheckBox.isSelected()) {
                cellDetector.setZStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()), (int) firstSliceSpinner.getValue(), (int) lastSliceSpinner.getValue());
            } else {
                cellDetector.setZStackParameters(zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            }
        }
//        Macro pretreatment?
        if (useAMacroCheckBox.isSelected()) {
            cellDetector.setPreprocessingMacro(macroArea.getText());
        }

//        Cellpose parameters ?
        String cellposeModel = cellPoseModelCombo.getSelectedItem() == "own_model" ? cellPoseModelPath.getAbsolutePath() : (String) cellPoseModelCombo.getSelectedItem();
        if (cellposeModel == null) {
            IJ.error("Please choose a model for cellpose.");
            return null;
        }
        cellDetector.setDeepLearning((Integer) cellPoseMinDiameterSpinner.getValue(), cellposeModel, cellPoseExcludeOnEdgesCheckBox.isSelected(), finalValidationCheckBox.isSelected(), isPreview || showBinaryMaskCheckBox.isSelected());

//        Saving results ?
        if (saveSegmentationMaskCheckBox.isVisible()) {
            cellDetector.setSavings(saveSegmentationMaskCheckBox.isSelected(), saveCellROIsCheckBox.isSelected());
        }
        return cellDetector;
    }

//    OTHER METHODS/FUNCTIONS
//    --> get all images associated to cell channel

    /**
     * @return all {@link CellDetector} corresponding to filtered images
     */
    public ArrayList<CellDetector> getImages() {
        ArrayList<CellDetector> cyto = new ArrayList<>();
        ImageToAnalyze image;
        String nameExperiment;
        if (!filteredImages) {
            IJ.error("No images given for nuclei. Please verify the image ending corresponds to at least an image.");
            return null;
        } else {
            addParametersToFile();
            for (int i = 0; i < imageListModel.getSize(); i++) {
                image = imageListModel.getElementAt(i);
                nameExperiment = ImageToAnalyze.getNameExperiment(image, imageEndingField);
                CellDetector cellDetector = getCellDetector(image, nameExperiment, false);
                if (cellDetector != null) {
                    cyto.add(getCellDetector(image, nameExperiment, false));
                } else {
                    return null;
                }
            }
            return cyto;
        }
    }

//    --> create parameters file

    /**
     * Add all parameters chosen in panel to file
     */
    private void addParametersToFile() {
        String directory = imageListModel.getElementAt(0).getDirectory();
        if (directory != null) {
            String parameterFilename = directory + "/Results/Parameters.txt";
            try {
                FileWriter fileWriter = new FileWriter(parameterFilename, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                if (nucleiPanel != null) {
                    bufferedWriter.append("\nCell/Cytoplasm PARAMETERS:");
                } else {
                    bufferedWriter.append("\nCell PARAMETERS:");
                }
                if (useAMacroCheckBox.isSelected()) {
                    bufferedWriter.append("\nMacro used:\n").append(macroArea.getText());
                }
                bufferedWriter.append("\nUse of cellpose: ");
                bufferedWriter.append("\nCellpose model: ").append(cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
                bufferedWriter.append("\nCellpose cell diameter: ").append(String.valueOf(cellPoseMinDiameterSpinner.getValue()));
                bufferedWriter.append("\nCellpose excludeOnEdges: ").append(cellPoseExcludeOnEdgesCheckBox.isSelected() ? "yes" : "no");
                if (nucleiPanel != null) {
                    bufferedWriter.append("\nMinimal overlap with nuclei (% of nuclei): ").append(Double.toString((Double) minOverlapSpinner.getValue()));
                    bufferedWriter.append("\nMinimal size of cytoplasm (% of cell): ").append(Double.toString((Double) minCytoSizeSpinner.getValue()));
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                IJ.log("The parameters could not be written.");
            }
        }
    }

    /**
     * For {@link NucleiPanel} and {@link CytoCellPanel}
     * Allows choice of cellpose model
     *
     * @param parent : {@link NucleiPanel} or {@link CytoCellPanel} main panel
     * @return File chosen corresponding to Cellpose Model
     */
    public static File chooseCellposeModelFile(Component parent) {
        //            Create JFileChooser to get directory
        JFileChooser fileChooser = new JFileChooser(IJ.getDirectory("current"));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

//            If file approved by user
        if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.exists()) {
                IJ.error("The path does not exists");
            } else if (file.isDirectory()) {
                IJ.error("It needs to be a file not a directory");
            }
            return file;
        }
        return null;
    }

//    --> PREFS

    /**
     * Preset panel choices according to prefs
     */
    private void getPreferences() {
//        Name
        imageEndingField.setText(Prefs.get("MICMAQ.CytoEnding", "_w31 FITC"));

//        Projection
        isAZStackCheckBox.setSelected(Prefs.get("MICMAQ.zStackCyto", true));
        zProjMethodsCombo.setSelectedItem(Prefs.get("MICMAQ.ProjMethodsCyto", "Maximum projection"));
        chooseSlicesToUseCheckBox.setSelected(Prefs.get("MICMAQ.chooseSliceCyto", false));
        if (!chooseSlicesToUseCheckBox.isSelected()) {
            slicesPanel.setVisible(false);
        }
        int maxSlices = imagesNames[0].getImagePlus().getNSlices();
        int firstSlice = (int) Prefs.get("MICMAQ.firstSliceCyto", 1);
        int lastSlice = (int) Prefs.get("MICMAQ.lastSliceCyto", imagesNames[0].getImagePlus().getNSlices());
        ImageToAnalyze.assertSlices(maxSlices, firstSlice, lastSlice, firstSliceSpinner, lastSliceSpinner);

//        Macro
        useAMacroCheckBox.setSelected(Prefs.get("MICMAQ.useMacroCyto", false));
        if (!useAMacroCheckBox.isSelected()) macroPanel.setVisible(false);
        macroArea.append(Prefs.get("MICMAQ.macroCyto", " "));


//        Segmentation
        cellPoseModelCombo.setSelectedItem(Prefs.get("MICMAQ.cellposeCytoMethods", "cyto2"));
        if (cellPoseModelCombo.getSelectedItem() != "own_model") {
            ownModelPanel.setVisible(false);
        }
        cellPoseMinDiameterSpinner.setValue((int) Prefs.get("MICMAQ.cellposeCytoMinDiameter", 100));
        cellPoseExcludeOnEdgesCheckBox.setSelected(Prefs.get("MICMAQ.cellposeCytoExcludeOnEdges", true));
        finalValidationCheckBox.setSelected(Prefs.get("MICMAQ.cytoFinalValidation", false));
        saveCellROIsCheckBox.setSelected(Prefs.get("MICMAQ.cytoSaveROI", true));
        saveSegmentationMaskCheckBox.setSelected(Prefs.get("MICMAQ.cytoSaveMask", true));

//        Cytoplasm options
        if (nucleiPanel != null) {
            minCytoSizeSpinner.setValue(Prefs.get("MICMAQ.minCytoSize", 25));
            minOverlapSpinner.setValue(Prefs.get("MICMAQ.minOverlap", 50));
        }

//        Show images ?
        if (fromDirectory) {
            showPreprocessingImageCheckBox.setSelected(false);
            showPreprocessingImageCheckBox.setVisible(false);
            showBinaryMaskCheckBox.setSelected(false);
            showBinaryMaskCheckBox.setVisible(false);
            showCompositeImageCheckBox.setSelected(false);
            showCompositeImageCheckBox.setVisible(false);
        } else {
            showPreprocessingImageCheckBox.setSelected(Prefs.get("MICMAQ.cytoShowPrepro", true));
            showCompositeImageCheckBox.setSelected(Prefs.get("MICMAQ.showCompositeImageCheckBox", true));
            showBinaryMaskCheckBox.setSelected(Prefs.get("MICMAQ.cytoShowMask", true));
        }
    }

    /**
     * set prefs in prefs file
     */
    public void setPreferences() {
//        Name
        Prefs.set("MICMAQ.CytoEnding", imageEndingField.getText());

//        Projection
        Prefs.set("MICMAQ.zStackCyto", isAZStackCheckBox.isSelected());
        if (isAZStackCheckBox.isSelected()) {
            Prefs.set("MICMAQ.ProjMethodsCyto", zProjMethodsCombo.getItemAt(zProjMethodsCombo.getSelectedIndex()));
            Prefs.set("MICMAQ.chooseSliceCyto", chooseSlicesToUseCheckBox.isSelected());
            if (chooseSlicesToUseCheckBox.isSelected()) {
                Prefs.set("MICMAQ.firstSliceCyto", (double) (int) firstSliceSpinner.getValue());
                Prefs.set("MICMAQ.lastSliceCyto", (double) (int) lastSliceSpinner.getValue());
            }
        }

//        Macro
        Prefs.set("MICMAQ.useMacroCyto", useAMacroCheckBox.isSelected());
        Prefs.set("MICMAQ.macroCyto", macroArea.getText());

//        Segmentation
        Prefs.set("MICMAQ.cellposeCytoMethods", cellPoseModelCombo.getItemAt(cellPoseModelCombo.getSelectedIndex()));
        Prefs.set("MICMAQ.cellposeCytoMinDiameter", (double) (int) cellPoseMinDiameterSpinner.getValue());
        Prefs.set("MICMAQ.cellposeCytoExcludeOnEdges", cellPoseExcludeOnEdgesCheckBox.isSelected());
        Prefs.set("MICMAQ.cytoFinalValidation", finalValidationCheckBox.isSelected());
        Prefs.set("MICMAQ.cytoSaveROI", saveCellROIsCheckBox.isSelected());
        Prefs.set("MICMAQ.cytoSaveMask", saveSegmentationMaskCheckBox.isSelected());

//        Cytoplasm options
        if (nucleiPanel != null) {
            Prefs.set("MICMAQ.minCytoSize", (Double) minCytoSizeSpinner.getValue());
            Prefs.set("MICMAQ.minOverlap", (Double) minCytoSizeSpinner.getValue());
        }


//        Show images
        if (!fromDirectory) {
            Prefs.set("MICMAQ.showCompositeImageCheckBox", showCompositeImageCheckBox.isSelected());
            Prefs.set("MICMAQ.cytoShowMask", showBinaryMaskCheckBox.isSelected());
            Prefs.set("MICMAQ.cytoShowPrepro", showPreprocessingImageCheckBox.isSelected());
        }
    }

//    --> GUI

    /**
     * Show or not saving results checkbox
     *
     * @param wantToSave : true if there is a need to show savings checkbox
     */
    public void saveResultsCheckBox(boolean wantToSave) {
        saveCellROIsCheckBox.setVisible(wantToSave);
        saveSegmentationMaskCheckBox.setVisible(wantToSave);
    }

    /**
     * @return panel containing everything
     */
    public JPanel getMainPanel() {
        return mainPanel;
    }

    /**
     * Customize panel elements
     */
    private void createUIComponents() {
        imageListModel.addElement(null);
        imageList = new JList<>(imageListModel);
//        minSize spinner
        cellPoseMinDiameterSpinner = new JSpinner(new SpinnerNumberModel(200, 0, Integer.MAX_VALUE, 100));
//        min overlap of nuclei and cell
        minOverlapSpinner = new JSpinner(new SpinnerNumberModel(50, 0.0, 100.0, 10));
//        minimal size of cytoplasm compared to cell
        minCytoSizeSpinner = new JSpinner(new SpinnerNumberModel(25, 0.0, 100.0, 10));
//        Text field to filter extension
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
        deepLearningPanel.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(deepLearningPanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deepLearningPanel.setBorder(BorderFactory.createTitledBorder(null, "Deep learning parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        deepLearningPanel.add(cellPoseMinDiameterSpinner, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseMinDiameterLabel = new JLabel();
        cellPoseMinDiameterLabel.setText("Minimum diameter of cell (pixel)");
        deepLearningPanel.add(cellPoseMinDiameterLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelLabel = new JLabel();
        cellPoseModelLabel.setText("Model for Cellpose");
        deepLearningPanel.add(cellPoseModelLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseExcludeOnEdgesCheckBox = new JCheckBox();
        cellPoseExcludeOnEdgesCheckBox.setSelected(true);
        cellPoseExcludeOnEdgesCheckBox.setText("Exclude on edges");
        deepLearningPanel.add(cellPoseExcludeOnEdgesCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showBinaryMaskCheckBox = new JCheckBox();
        showBinaryMaskCheckBox.setText("Show segmentation(s) mask(s)");
        showBinaryMaskCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        deepLearningPanel.add(showBinaryMaskCheckBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        showCompositeImageCheckBox = new JCheckBox();
        showCompositeImageCheckBox.setText("Show composite image");
        showCompositeImageCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
        deepLearningPanel.add(showCompositeImageCheckBox, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cellPoseModelCombo = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("cyto");
        defaultComboBoxModel1.addElement("cyto2");
        defaultComboBoxModel1.addElement("tissuenet");
        defaultComboBoxModel1.addElement("livecell");
        defaultComboBoxModel1.addElement("own_model");
        cellPoseModelCombo.setModel(defaultComboBoxModel1);
        deepLearningPanel.add(cellPoseModelCombo, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ownModelPanel = new JPanel();
        ownModelPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        deepLearningPanel.add(ownModelPanel, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        modelPathLabel = new JLabel();
        modelPathLabel.setText("Path own model");
        ownModelPanel.add(modelPathLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modelPathField = new JTextField();
        ownModelPanel.add(modelPathField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        modelBrowseButton = new JButton();
        modelBrowseButton.setText("Browse");
        ownModelPanel.add(modelBrowseButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoParametersPanel = new JPanel();
        cytoParametersPanel.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        deepLearningPanel.add(cytoParametersPanel, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        minOverlapLabel = new JLabel();
        minOverlapLabel.setText("Minimal overlap of nucleus with cell (% of nucleus)");
        cytoParametersPanel.add(minOverlapLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoParametersPanel.add(minOverlapSpinner, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minCytoSizeLabel = new JLabel();
        minCytoSizeLabel.setText("Minimal size of cytoplasm (% of cell)");
        cytoParametersPanel.add(minCytoSizeLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cytoParametersPanel.add(minCytoSizeSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveSegmentationMaskCheckBox = new JCheckBox();
        saveSegmentationMaskCheckBox.setText("Save segmentation(s) mask(s)");
        deepLearningPanel.add(saveSegmentationMaskCheckBox, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveCellROIsCheckBox = new JCheckBox();
        saveCellROIsCheckBox.setText("Save all ROIs");
        deepLearningPanel.add(saveCellROIsCheckBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        finalValidationCheckBox = new JCheckBox();
        finalValidationCheckBox.setText("Final validation");
        deepLearningPanel.add(finalValidationCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        showPreprocessingImageCheckBox.setToolTipText("Warning ! Process will pause after each set of images' measurement");
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
        cellPoseMinDiameterLabel.setLabelFor(cellPoseMinDiameterSpinner);
        cellPoseModelLabel.setLabelFor(cellPoseModelCombo);
        minOverlapLabel.setLabelFor(minOverlapSpinner);
        minCytoSizeLabel.setLabelFor(minCytoSizeSpinner);
        zProjMethodsLabel.setLabelFor(zProjMethodsCombo);
        imageEndingLabel.setLabelFor(imageEndingField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
