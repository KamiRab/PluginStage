import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import ij.IJ;
import ij.Prefs;
import ij.WindowManager;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static ij.IJ.selectWindow;

/*TODO dernier repertoire actif comme point de départ*/
public class OpenImages extends JFrame implements PlugIn {
    private JPanel openImages;
    private JPanel directoryImages;
    private JRadioButton useFileImagesRadioButton;
    private JRadioButton useOpenImagesRadioButton;
    private JList<ImageToAnalyze> windowList;
    private JButton OKButton;
    private JPanel main;
    private JButton cancelButton;
    private JComboBox<String> extension;
    private JButton chooseDirectoryButton;
    private JTextArea choosenDirectory;
    private JButton selectImageButton;
    private JButton removeButton;
    private JList<String> choosenDirectoryFiles;
    private JLabel preText;
    private JPanel radioButtonChoice;
    private JPanel originImages;
    private JPanel validation;
    private JLabel extensionLabel;
    private JScrollPane imageListScroll;
    private JTextField otherExtensionField;
    boolean useDirectory = false;
    File directory;
    ImageToAnalyze[] IP_list;
    //    DefaultListModel<ImagePlusDisplay> openImagesModel = new DefaultListModel<>();
    DefaultListModel<ImageToAnalyze> openImagesModel = new DefaultListModel<>();
    DefaultListModel<String> fileModel = new DefaultListModel<>();

    public OpenImages() {
        $$$setupUI$$$();
//        Look for open images to see if the choose from ImageJ panel is necessary
        if (WindowManager.getImageCount() == 0) { /*No open images*/
            /*No need for the radiobutton*/
            useOpenImagesRadioButton.setVisible(false);
            useFileImagesRadioButton.setVisible(false);
            /*Display panel to choose from directory and hide the other*/
            useDirectory = true;
            openImages.setVisible(false);
            pack();
        } else {
            /*if opened images, by default the directory panel is hidden*/
            directoryImages.setVisible(false);
            pack();
        }
        otherExtensionField.setVisible(false);
        extension.setSelectedItem(Prefs.get("PluginToName.ChoiceExtension", ".TIF"));
        otherExtensionField.setText(Prefs.get("PluginToName.ChoiceExtension", ".TIF"));
        cancelButton.addActionListener(e -> this.dispose()); /*TODO fonctionne pas*/
        OKButton.addActionListener(e -> {
            if (useDirectory) {
                if (directory == null) {
                    IJ.error("No directory chosen");
                } else {
                    if (IP_list == null) {
                        IJ.error("No images to analyze");
                    } else {
                        Path path = Paths.get(directory.getAbsolutePath() + "\\Results\\Images");
                        Path path2 = Paths.get(directory.getAbsolutePath() + "\\Results\\ROI");
                        try {
                            Files.createDirectories(path);
                        } catch (IOException ex) {
                            IJ.error("Failed to create results directory" + ex.getMessage());
                        }
                        try {
                            Files.createDirectories(path2);
                        } catch (IOException ex) {
                            IJ.error("Failed to create results directory" + ex.getMessage());
                        }
                        IJ.log("There are " + IP_list.length + " images");
                        Plugin_cellProt analysis = new Plugin_cellProt(IP_list, useDirectory);
                        analysis.run(null);
                        this.setVisible(false);
                        Prefs.set("PluginToName.ChoiceExtension", extension.getItemAt(extension.getSelectedIndex()));
                        Prefs.set("PluginToName.ChoiceExtensionCustom", otherExtensionField.getText());
                    }
                }
            } else {
                if (windowList.getModel().getSize() == 0) {
                    IJ.error("There are no images to analyse");
                } else {
                    IP_list = new ImageToAnalyze[openImagesModel.getSize()];
                    for (int i = 0; i < openImagesModel.getSize(); i++) {
                        IP_list[i] = openImagesModel.getElementAt(i);
                    }
                    IJ.log("There are " + IP_list.length + " images");
                    Plugin_cellProt analysis = new Plugin_cellProt(IP_list, useDirectory);
                    analysis.run(null);
                    this.setVisible(false);
                }

            }
        });
        chooseDirectoryButton.addActionListener(e -> {
            /*TODO  toujours current ou prefs ?*/
            JFileChooser directoryChooser = new JFileChooser(IJ.getDirectory("current"));
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (directoryChooser.showOpenDialog(OpenImages.this) == JFileChooser.APPROVE_OPTION) {
                directory = directoryChooser.getSelectedFile();
                if (!directory.exists()) {
                    IJ.error("The directory does not exists");
                } else if (!directory.isDirectory()) {
                    IJ.error("It needs to be a directory not a file");
                }
                /* TODO create imagePlus later*/
                String path = directory.getAbsolutePath();
                if (path.split("\\\\").length > 2) {
                    String path_shorten = path.substring(path.substring(0, path.lastIndexOf("\\")).lastIndexOf("\\"));
                    choosenDirectory.setText("..." + path_shorten);
                } else {
                    choosenDirectory.setText(path);
                }

                getFilesFromDirectory();
            }
        });
        useOpenImagesRadioButton.addActionListener(e -> {
            useDirectory = false;
            openImages.setVisible(true);
            directoryImages.setVisible(false);
            this.pack();
            /*TODO preciser répertoire de sortie des résultats*/
        });
        useFileImagesRadioButton.addActionListener(e -> {
            directoryImages.setVisible(true);
            openImages.setVisible(false);
            useDirectory = true;
            pack();
        });
        selectImageButton.addActionListener(e -> {
//                int selectedImageIndex = getImageIDFromJListElement(windowList.getSelectedValue());
            int selectedImageIndex = windowList.getSelectedValue().getImagePlus().getID();
            selectWindow(selectedImageIndex);
        });
        removeButton.addActionListener(e -> openImagesModel.removeElementAt(windowList.getSelectedIndex()));
        extension.addItemListener(e -> {
            String extensionSelected = (String) extension.getSelectedItem();
            if (extensionSelected.equals("Other")) {
                otherExtensionField.setVisible(true);
                otherExtensionField.setText(Prefs.get("PluginToName.ChoiceExtensionCustom", ".TIF"));
            } else {
                otherExtensionField.setText(extensionSelected);
                otherExtensionField.setVisible(false);
            }
            if (IP_list.length > 0) {
                getFilesFromDirectory();
            }
        });
    }

    @Override
    public void run(String s) {
        createUIComponents();
        setTitle("Plugin2Name");
        setContentPane(new OpenImages().main);
        setPreferredSize(new Dimension(500, 300));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void getFilesFromDirectory() {
        String extension = otherExtensionField.getText().toLowerCase(Locale.ROOT);
        FileFilter fileFilter = file -> !file.isDirectory()
                && (file.getName().toLowerCase(Locale.ROOT).endsWith(extension));
        File[] images = directory.listFiles(fileFilter);
        if (images != null) {
            if (images.length == 0) {
                IJ.error("No file corresponding to the extension chosen has been found");
                if (!fileModel.contains("No file")) {
                    fileModel.removeAllElements();
                    fileModel.addElement("No file");
                }
            } else {
                IP_list = new ImageToAnalyze[images.length];
                fileModel.removeAllElements();
                for (int i = 0; i < images.length; i++) {
//                    IP_list[i] = new ImageToAnalyze(new ImagePlus(images[i].getAbsolutePath()));
                    IP_list[i] = new ImageToAnalyze(directory.getAbsolutePath(), images[i].getName());
//                    IJ.log(IP_list[i].getImagePlus().getTitle() + ":" + images[i].getName());
                    fileModel.addElement(images[i].getName());
                }

            }
        }
    }

//    public static ImagePlus getImagePlusFromJListElement(String listElement) {
//        return WindowManager.getImage(getImageIDFromJListElement(listElement));
//    }
//
//    public static int getImageIDFromJListElement(String listElement) {
//        String id = listElement.split("#")[1];
//        return Integer.parseInt(id);
//    }

//    public void processWindowEvent(WindowEvent e) {
//        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
////            WindowManager.closeAllWindows();
//            this.dispose();
//        }
//    }

    private void createUIComponents() {
        if (WindowManager.getImageCount() > 0)
            for (int id_Image : WindowManager.getIDList()) {
                ImageToAnalyze image = new ImageToAnalyze(WindowManager.getImage(id_Image));
                openImagesModel.addElement(image);
                /*TODO mettre ID + title puis parseint*/
            }
        windowList = new JList<>(openImagesModel);
        fileModel.addElement("No file");
        choosenDirectoryFiles = new JList<>(fileModel);
        windowList.setSelectedIndex(0);

        preText = new JLabel();
        if (WindowManager.getImageCount() > 0) {
            preText.setText("There are open images, do you want to use them ?");
        } else {
            preText.setText("There are no open images, please choose a directory");
            useDirectory = true;
        }
//        windowList = new JList<>(model);//
//        for(String image : ij.WindowManager.getImageTitles()){
//            model.addElement(image);
//        }
    }

    public static void main(String[] args) {
//        ImagePlus[] imagesToAnalyze = new ImagePlus[3];
//        imagesToAnalyze[0] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
//        imagesToAnalyze[1] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
//        imagesToAnalyze[2] = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w21 FITC.TIF");
//        for (ImagePlus images : imagesToAnalyze
//        ) {
//            images.show();
//        }
        OpenImages openImages = new OpenImages();
        openImages.run(null);
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
        main.add(preText, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonChoice = new JPanel();
        radioButtonChoice.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        main.add(radioButtonChoice, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        useOpenImagesRadioButton = new JRadioButton();
        useOpenImagesRadioButton.setSelected(true);
        useOpenImagesRadioButton.setText("Yes");
        radioButtonChoice.add(useOpenImagesRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        useFileImagesRadioButton = new JRadioButton();
        useFileImagesRadioButton.setText("No");
        radioButtonChoice.add(useFileImagesRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validation = new JPanel();
        validation.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        main.add(validation, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        OKButton = new JButton();
        OKButton.setText("OK");
        validation.add(OKButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        validation.add(cancelButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        originImages = new JPanel();
        originImages.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        main.add(originImages, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openImages = new JPanel();
        openImages.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        originImages.add(openImages, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        selectImageButton = new JButton();
        selectImageButton.setText("Select image");
        openImages.add(selectImageButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        removeButton = new JButton();
        removeButton.setText("Remove");
        openImages.add(removeButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        openImages.add(scrollPane1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        windowList.setSelectionMode(0);
        scrollPane1.setViewportView(windowList);
        directoryImages = new JPanel();
        directoryImages.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        originImages.add(directoryImages, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        extensionLabel = new JLabel();
        extensionLabel.setText("Extension of files");
        directoryImages.add(extensionLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extension = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement(".TIF");
        defaultComboBoxModel1.addElement(".STK");
        defaultComboBoxModel1.addElement("Other");
        extension.setModel(defaultComboBoxModel1);
        directoryImages.add(extension, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseDirectoryButton = new JButton();
        chooseDirectoryButton.setText("Choose directory");
        directoryImages.add(chooseDirectoryButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choosenDirectory = new JTextArea();
        choosenDirectory.setLineWrap(true);
        choosenDirectory.setText("No directory choosen");
        choosenDirectory.setWrapStyleWord(false);
        directoryImages.add(choosenDirectory, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageListScroll = new JScrollPane();
        directoryImages.add(imageListScroll, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        choosenDirectoryFiles.setEnabled(true);
        imageListScroll.setViewportView(choosenDirectoryFiles);
        otherExtensionField = new JTextField();
        otherExtensionField.setText("");
        directoryImages.add(otherExtensionField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(useOpenImagesRadioButton);
        buttonGroup.add(useFileImagesRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

}
