import ij.ImagePlus;
import ij.WindowManager;

public class ImagePlusDisplay {
    ImagePlus imagePlus;

    public ImagePlusDisplay(ImagePlus imagePlus){
        this.imagePlus = imagePlus;
    }
    @Override
    public String toString() {
        return imagePlus.getTitle() + "#" + imagePlus.getID();
    }

    public int getID(){
        return imagePlus.getID();
    }

    public ImagePlus getImagePlus() {
        return imagePlus;
    }
}
