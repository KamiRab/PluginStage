package Helpers;

import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to facilitate display of ImagePlus names
 * and for images from directory creates the ImagePlus instances at the necessary time
 */
public class ImageToAnalyze {
    private ImagePlus imagePlus;
    private String directory; /*directory to save results to and for not opened images, the directory containing them*/
    private final String imageName;

//    CONSTRUCTORS

    /**
     * Constructor for image from directory
     *
     * @param directory : directory containing image
     * @param imageName : name of image file
     */
    public ImageToAnalyze(String directory, String imageName) {
        if (directory.contains("/")) {/*unify the directory delimiter*/
            directory = directory.replace("/", "\\");
        }
        if (directory.endsWith("\\")) {
            directory = directory.substring(0, directory.length() - 1);
        }
        this.directory = directory;
//                        CREATE RESULTS DIRECTORY
        createResultsDirectory(directory);
        this.imageName = imageName;
    }

    /**
     * Constructor for image opened in ImageJ
     *
     * @param imagePlus : opened image
     */
    public ImageToAnalyze(ImagePlus imagePlus) {
        this.imagePlus = imagePlus;
        this.imagePlus.setCalibration(null);/*remove calibration*/
        this.imageName = imagePlus.getTitle();
        directory = null;
    }


//    GETTERS

    /**
     * If ImagePlus does not exists, creates the instance
     *
     * @return ImagePlus instance
     */
    public ImagePlus getImagePlus() {
        if (imagePlus == null) {
            this.imagePlus = IJ.openImage(directory + "\\" + imageName);
            this.imagePlus.setCalibration(null); /*remove calibration*/
        }
        ImagePlus toReturn = imagePlus.duplicate();
        toReturn.setTitle(imageName);
        return toReturn;
    }

    /**
     * @return directory to save results
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * @return name of image
     */
    public String getImageName() {
        return imageName;
    }


//    SETTERS

    /**
     * Used for saving the results when the images are opened
     *
     * @param directory : name to save results
     */
    public void setDirectory(String directory) {
        this.directory = directory;
        createResultsDirectory(directory);
    }

    /**
     * @return String with either name of image (image from directory) or image name with ID (if opened image)
     */
    @Override
    public String toString() {
        if (directory != null) {
            return imageName;
        } else { /*For openImages, we want the ID to be displayed in case there are multiple images opened with same name*/
            return imageName + "#" + imagePlus.getID();
        }
    }


    //    FUNCTIONS/METHODS

    /**
     * Create result directory (with subdirectories Images and ROI)
     */
    public static void createResultsDirectory(String directory) {
        Path path = Paths.get(new File(directory).getAbsolutePath() + "\\Results\\Images");
        Path path2 = Paths.get(new File(directory).getAbsolutePath() + "\\Results\\ROI");
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
    }

    /**
     * For clarity in results
     *
     * @param image_name : name of image
     * @return name of image without extension, if extension exists
     */
    public static String name_without_extension(String image_name) {
        int lastPoint = image_name.lastIndexOf(".");
        if (lastPoint != -1) {
            return image_name.substring(0, lastPoint);
        } else {
            return image_name;
        }
    }

    public static boolean filterModel(DefaultListModel<ImageToAnalyze> model, String filter, ImageToAnalyze[] imagesNames, JLabel errorImageEndingLabel) {
        for (ImageToAnalyze image : imagesNames) {
            String title = image.getImageName();
            String title_wo_ext = ImageToAnalyze.name_without_extension(title);
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
            /*if no image corresponds to the filter, display all images names and an error*/
            for (ImageToAnalyze imagePlusDisplay : imagesNames) {
                model.addElement(imagePlusDisplay);
            }
            errorImageEndingLabel.setVisible(true);
//            filteredImages = false; /*there are no images corresponding to the label*/
            return false; /*there are no images corresponding to the label*/
        } else {
            errorImageEndingLabel.setVisible(false);
//            filteredImages = true; /*there are images corresponding to the label*/
            return true; /*there are images corresponding to the label*/
        }
    }

}
