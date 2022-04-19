import ij.IJ;
import ij.ImagePlus;

public class ImageToAnalyze {
    private ImagePlus imagePlus;
    private final String directory;
    private final String imageName;

    public ImageToAnalyze(String directory, String imageName) {
        this.directory = directory;
        this.imageName = imageName;
    }

    public ImageToAnalyze(ImagePlus imagePlus){
        this.imagePlus=imagePlus;
        this.imageName=imagePlus.getTitle();
        directory=null;
    }

    public String getNameWOextension(String name){
        int lastPoint = name.lastIndexOf(".");
        return name.substring(0,lastPoint);
    }

    public ImagePlus getImagePlus() {
        if (imagePlus==null){
            this.imagePlus=IJ.openImage(directory+"\\"+imageName);
        }
        return imagePlus;
    }

    public String getDirectory() {
        return directory;
    }

    public String getImageName() {
        return imageName;
    }

    @Override
    public String toString() {
        if (directory != null){
            return imageName;
        } else {
            return imageName+"#"+imagePlus.getID(); /*TODO ou juste boolean openImage qui est changé dans plugin ?*/
        }
    }
}
