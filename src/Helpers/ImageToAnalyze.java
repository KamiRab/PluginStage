package Helpers;

import ij.IJ;
import ij.ImagePlus;

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
     * @param directory : directory containing image
     * @param imageName : name of image file
     */
    public ImageToAnalyze(String directory, String imageName) {
        if (directory.endsWith("\\")){
            directory = directory.substring(0,directory.length()-1);
        }
        this.directory = directory;
        this.imageName = imageName;
    }

    /**
     * Constructor for image opened in ImageJ
     * @param imagePlus
     */
    public ImageToAnalyze(ImagePlus imagePlus){
        this.imagePlus=imagePlus;
        this.imageName=imagePlus.getTitle();
        directory=null;
    }

//    GETTERS

    /**
     * If ImagePlus does not exists, creates the instance
     * @return ImagePlus instance
     */
    public ImagePlus getImagePlus() {
        if (imagePlus==null){
            this.imagePlus=IJ.openImage(directory+"\\"+imageName);
        }
        return imagePlus;
    }

    /**
     *
     * @return directory to save results
     */
    public String getDirectory() {
        return directory;
    }

    /**
     *
     * @return name of image
     */
    public String getImageName() {
        return imageName;
    }

//    SETTERS

    /**
     * Used for saving the results when the images are opened
     * @param directory : name to save results
     */
    public void setDirectory(String directory){
        this.directory = directory;
    }


    @Override
    public String toString() {
        if (directory != null){
            return imageName;
        } else { /*For openImages, we want the ID to be displayed in case there are multiple images opened with same name*/
            return imageName+"#"+imagePlus.getID(); /*TODO ou juste boolean openImage qui est changé dans plugin ?*/
        }
    }

//    FUNCTIONS/METHODS

    /**
     * For clarity in results
     * @param image_name : name of image
     * @return name of image without extension, if extension exists
     */
    public static String name_without_extension(String image_name){
        int lastPoint = image_name.lastIndexOf(".");
        if (lastPoint!=-1){
            return image_name.substring(0,lastPoint);
        }else {
            return image_name;
        }
    }
}
