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
 * - For images from directory creates the ImagePlus instances at the necessary time
 * - If directory set, creates Results directory
 */
public class ImageToAnalyze {
    private ImagePlus imagePlus;
    private String directory; /*directory to save results them*/
    private final String imageName;

//    CONSTRUCTORS

    /**
     * Constructor for image from directory
     *
     * @param directory : directory containing image
     * @param imageName : name of image file
     */
    public ImageToAnalyze(String directory, String imageName) {
        this.setDirectory(directory);
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
            this.imagePlus = IJ.openImage(directory + "/" + imageName);
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
     * Unify path separators and create results directory
     * @param directory : path of directory
     */
    public void setDirectory(String directory) {
//        UNIFY PATH SEPARATOR
        if (directory.contains("\\")) {/*unify the directory delimiter*/
            directory = directory.replace("\\", "/");
        }
//        REMOVE EXTRA SEPARATOR IF NECESSARY
        if (directory.endsWith("/")) {
            directory = directory.substring(0, directory.length() - 1);
        }
        this.directory = directory;
//        CREATE RESULTS DIRECTORY
        createResultsDirectory(directory);
    }



    //    FUNCTIONS/METHODS
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

    /**
     * Create result directory (with subdirectories Images and ROI)
     * @param directory : path of directory
     */
    public static void createResultsDirectory(String directory) {
        Path path = Paths.get(new File(directory).getAbsolutePath() + "/Results/Images");
        Path path2 = Paths.get(new File(directory).getAbsolutePath() + "/Results/ROI");
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
     * Remove extension of imageName
     * @param imageName : name of image
     * @return name of image without extension, if extension exists
     */
    public static String nameWithoutExtension(String imageName) {
        int lastPoint = imageName.lastIndexOf(".");
        if (lastPoint != -1) {
            return imageName.substring(0, lastPoint);
        } else {
            return imageName;
        }
    }

    /**
     * Filter list of image by name ending and if necessary display error message in panel
     * Iterates on all names in the model and if the name does not end with the label given by user,
     * it is removed from the model
     * @param filteredImageList : JList model
     * @param endingFilter : String that the images names should end by
     * @param allImageList : all the images that have to be considered
     * @param errorImageEndingLabel : Jlabel that is displayed in case of empty model
     * @return true if there are images in the filteredImageList.
     */
    public static boolean filterModelbyEnding(DefaultListModel<ImageToAnalyze> filteredImageList, String endingFilter, ImageToAnalyze[] allImageList, JLabel errorImageEndingLabel) {
        for (ImageToAnalyze image : allImageList) {
            String title = image.getImageName();
            String titleWoExt = ImageToAnalyze.nameWithoutExtension(title);
//          Assert if image title ends by filter
            if (!title.endsWith(endingFilter) && !titleWoExt.endsWith(endingFilter)) { /*the title does not end by the filter*/
                if (filteredImageList.contains(image)) { /*if model contains the image, removes it*/
                    filteredImageList.removeElement(image);
                }
            } else { /*the title ends by the filter*/
                if (!filteredImageList.contains(image)) { /*if the model does not contain the image, adds it*/
                    filteredImageList.addElement(image);
                }
            }
        }
        if (filteredImageList.isEmpty()) {
            /*if no image corresponds to the filter, display all images names and an error*/
            for (ImageToAnalyze imagePlusDisplay : allImageList) {
                filteredImageList.addElement(imagePlusDisplay);
            }
            errorImageEndingLabel.setVisible(true);
            return false; /*there are no images corresponding to the label*/
        } else {
            errorImageEndingLabel.setVisible(false);
            return true; /*there are images corresponding to the label*/
        }
    }

}
