import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

import static ij.IJ.selectWindow;

public class OpenImages extends JFrame implements PlugIn {
    private JPanel openImages;
    private JPanel directoryImages;
    private JRadioButton noRadioButton;
    private JRadioButton yesRadioButton;
    private JList<String> windowList;
    private JButton OKButton;
    private JPanel main;
    private JButton cancelButton;
    private JComboBox<String> extension;
    private JButton chooseDirectoryButton;
    private JLabel choosenDirectory;
    private JButton selectImageButton;
    private JButton removeButton;
    private JTextArea choosenDirectoryFiles;
    private JLabel preText;
    private JPanel radioButtonChoice;
    private JPanel originImages;
    private JPanel validation;
    private JLabel extensionLabel;
    boolean useDirectory = false;
    File directory;
    ImagePlus[] IP_list;
    DefaultListModel<String> model = new DefaultListModel<>();

    public OpenImages() {
        $$$setupUI$$$();
        directoryImages.setVisible(false);
        OKButton.addActionListener(e -> {
            if (useDirectory) {
                if (directory == null) {
                    IJ.error("No directory chosen");
                } else {
                    if (IP_list == null) {
                        IJ.error("No images to analyze");
                    } else {
                        IJ.log("There are " + IP_list.length + " images");
                        Plugin_cellProt analysis = new Plugin_cellProt(IP_list, useDirectory);
                        analysis.run(null);
                        this.setVisible(false);
                    }
                }

            } else {
                if (windowList.getModel().getSize() == 0) {
                    IJ.error("There are no images to analyse");
                } else {
                    for (int i = 0; i < windowList.getModel().getSize(); i++) {
                        IJ.log(windowList.getModel().getElementAt(i).split("#")[0]);
                    }
                    IJ.log("There are " + IP_list.length + " images");
                    Plugin_cellProt analysis = new Plugin_cellProt(IP_list, useDirectory);
                    analysis.run(null);
                    this.setVisible(false);
                }

            }
        });
        chooseDirectoryButton.addActionListener(e -> {
            JFileChooser directoryChooser = new JFileChooser();
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (directoryChooser.showOpenDialog(OpenImages.this) == JFileChooser.APPROVE_OPTION) {
                directory = directoryChooser.getSelectedFile();
                if (!directory.exists()) {
                    IJ.error("The directory does not exists");
                } else if (!directory.isDirectory()) {
                    IJ.error("It needs to be a directory not a file");
                }
                choosenDirectory.setText(directory.getAbsolutePath());
                getFilesFromDirectory();
            }
        });

        yesRadioButton.addActionListener(e -> {
            useDirectory = false;
            openImages.setVisible(true);
            directoryImages.setVisible(false);
//            model.addElement("OK");
            this.pack();
            /*TODO preciser répertoire de sortie des résultats*/
        });
        noRadioButton.addActionListener(e -> {
            directoryImages.setVisible(true);
            openImages.setVisible(false);
            useDirectory = true;
            pack();
        });
        cancelButton.addActionListener(e -> this.dispose()); /*TODO fonctionne pas*/
        selectImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedImageIndex = getImageIDFromJListElement(windowList.getSelectedValue());
                selectWindow(selectedImageIndex);
            }
        });
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.removeElementAt(windowList.getSelectedIndex());
            }
        });
    }

    @Override
    public void run(String s) {
        if (WindowManager.getImageCount() == 0) {
            yesRadioButton.setVisible(false);
            noRadioButton.setVisible(false);
        }
        createUIComponents();
        setTitle("Plugin2Name");
        setContentPane(new OpenImages().main);
        setPreferredSize(new Dimension(500, 300));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    private void getFilesFromDirectory() {
        FileFilter fileFilter = file -> !file.isDirectory()
                && (file.getName().endsWith("" + extension.getItemAt(extension.getSelectedIndex()))
                || file.getName().endsWith("" + extension.getItemAt(extension.getSelectedIndex()).toLowerCase(Locale.ROOT)));
        File[] images = directory.listFiles(fileFilter);
        if (images != null) {
            if (images.length == 0) {
                IJ.error("No file corresponding to the extension chosen has been found");
            } else {
                IP_list = new ImagePlus[images.length];
                for (int i = 0; i < images.length; i++) {
                    IP_list[i] = new ImagePlus(images[i].getAbsolutePath());
                    IJ.log(IP_list[i].getTitle() + ":" + images[i].getName());
                    choosenDirectoryFiles.append(images[i].getName() + "\n");
                }

            }
        }
    }

    public static ImagePlus getImagePlusFromJListElement(String listElement) {
        return WindowManager.getImage(getImageIDFromJListElement(listElement));
    }

    public static int getImageIDFromJListElement(String listElement) {
        String id = listElement.split("#")[1];
        return Integer.parseInt(id);
    }

    public void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
//            WindowManager.closeAllWindows();
            this.dispose();
        }
    }

    private void createUIComponents() {
        if (WindowManager.getImageCount() > 0)
            for (int id_Image : WindowManager.getIDList()) {
                String image_title = WindowManager.getImage(id_Image).getTitle();
                model.addElement(image_title + "#" + id_Image);
                /*TODO mettre ID + title puis parseint*/
            }
        windowList = new JList<String>(model);
        windowList.setSelectedIndex(0);

        preText = new JLabel();
        if (WindowManager.getImageCount() > 0) {
            preText.setText("There are open images, do you want to use them ?");
        } else {
            preText.setText("There are no open images, please choose a directory");
            useDirectory = true;
        }
//        windowList = new JList<>(model);// TODO: place custom component creation code here
//        for(String image : ij.WindowManager.getImageTitles()){
//            model.addElement(image);
//        }
    }

    public static void main(String[] args) {

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
        yesRadioButton = new JRadioButton();
        yesRadioButton.setSelected(true);
        yesRadioButton.setText("Yes");
        radioButtonChoice.add(yesRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        noRadioButton = new JRadioButton();
        noRadioButton.setText("No");
        radioButtonChoice.add(noRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        windowList.setSelectionMode(0);
        openImages.add(windowList, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        directoryImages = new JPanel();
        directoryImages.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        originImages.add(directoryImages, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        extensionLabel = new JLabel();
        extensionLabel.setText("Extension of files");
        directoryImages.add(extensionLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extension = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement(".TIF");
        defaultComboBoxModel1.addElement(".SVG");
        extension.setModel(defaultComboBoxModel1);
        directoryImages.add(extension, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chooseDirectoryButton = new JButton();
        chooseDirectoryButton.setText("Choose directory");
        directoryImages.add(chooseDirectoryButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choosenDirectory = new JLabel();
        choosenDirectory.setText("No directory choosen");
        directoryImages.add(choosenDirectory, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        choosenDirectoryFiles = new JTextArea();
        choosenDirectoryFiles.setEditable(false);
        choosenDirectoryFiles.setLineWrap(true);
        choosenDirectoryFiles.setText("");
        choosenDirectoryFiles.setWrapStyleWord(false);
        directoryImages.add(choosenDirectoryFiles, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 2, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(yesRadioButton);
        buttonGroup.add(noRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return main;
    }

    public class toto {

    }
}
