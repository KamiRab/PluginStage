import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.ZProjector;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import static ij.IJ.d2s;
import static ij.process.AutoThresholder.Method.Default;

public class Detector {
    private ImagePlus image;
    private final String name_image;
    private final String name_experiment;
    private final String name_object;
    private final Calibration calibration;
//    private boolean zStack;
    private String zStackProj;
    private int zStackFirstSlice;
    private int zStackLastSlice;

    public Detector(ImagePlus image, String name_experiment, String name_object,Calibration calibration) {
        this.image = image;
        this.name_image = image.getTitle();
        this.name_experiment = name_experiment;
        this.name_object=name_object;
        this.calibration = calibration;
    }

//    SETTER

    public void setzStackParameters(String zStackProj, int zStackFirstSlice, int zStackLastSlice) {
        this.zStackProj = zStackProj;
        this.zStackFirstSlice = zStackFirstSlice;
        this.zStackLastSlice = zStackLastSlice;
        projection();
    }


//    GETTER

    public ImagePlus getImage() {
        return image;
    }

//    FUNCTIONS/METHODS

    private void projection() {
        if (zStackProj.equals("Maximum projection")) {
            this.image = ZProjector.run(image, "max", zStackFirstSlice, zStackLastSlice); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        } else {
            this.image = ZProjector.run(image, "sd", zStackFirstSlice, zStackLastSlice); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        }
        rename_image(this.image,"projection");
    }

    public ImagePlus getThresholdMask(ImagePlus image, String thresholdMethod, double minSizeParticle) {
//        GET IMAGE TO THRESHOLD
        ImagePlus threshold_IP= image.duplicate();
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*get processor*/

//        DEFINE THRESHOLD VALUES THROUGH METHOD GIVEN
        /*TODO toujours bkg foncé ?*/
        threshold_proc.setAutoThreshold(thresholdMethod + " dark");

//        APPLY THRESHOLD TO IMAGE
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_MASKS+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES+ParticleAnalyzer.CLEAR_WORKSHEET,0,null,minSizeParticle,999999999);
        particleAnalyzer.analyze(threshold_IP);
        ImagePlus mask_IP = particleAnalyzer.getOutputImage();
        rename_image(mask_IP,"binary_mask");
        return mask_IP;
    }

    /**
     * @param threshold_IP TODO
     */
    public RoiManager analyzeParticles(ImagePlus threshold_IP,boolean excludeOnEdges,double minSizeParticle) {
        RoiManager roiManager = RoiManager.getInstance(); /*recupere ROImanager actuel*/
        if (roiManager == null) { /*si aucun RoiManager trouvé, en crée un*/
            roiManager = new RoiManager();/*creation si besoin est, d'un ROI manager pour l'avoir en variable*/
        } else {
            if (roiManager.getCount()>0){ /*test to see if no exception of arrayboundoutofindex or similar*/
                roiManager.reset();
            }
        }
        ParticleAnalyzer.setRoiManager(roiManager); /*precise ROImanager à utiliser*/
        ImageProcessor threshold_proc = threshold_IP.getProcessor();
        ParticleAnalyzer particleAnalyzer;
        if(excludeOnEdges){
            particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS + ParticleAnalyzer.ADD_TO_MANAGER + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 0, null, minSizeParticle, 999999);
        } else {
            particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS + ParticleAnalyzer.ADD_TO_MANAGER, 0, null, minSizeParticle, 999999);
        }
        /*précise param a utiliser : actuellement créé overlay, ajouter ROI au ROIm, ne compte pas les particules partielles, ne mesure rien (je crois ?), taille minimale de particule = 1000*/

        threshold_proc.invertLut();
        threshold_proc.setAutoThreshold(Default, false); /*besoin de recreer threshold, vu que image est devenu binaire */
        particleAnalyzer.analyze(threshold_IP); /*recupere particules dans ROI + mesures*/
        return roiManager;
    }

    public void rename_image (ImagePlus imageToRename, String toAdd){
        int lastPoint = name_image.lastIndexOf(".");
        String getTitleWoExt;
        String ext;
        if (lastPoint != -1){ /*a point is present in the string*/
            getTitleWoExt = name_image.substring(0,lastPoint);
            ext = name_image.substring(lastPoint);
            imageToRename.setTitle(getTitleWoExt +"_"+toAdd+ext);
        }else {
            imageToRename.setTitle(name_image+"_"+toAdd);
        }
    }

    public void setResultsNucleus(ResultsTable rawMesures, ResultsTable customMesures, int nucleus) {
        for (String measure : rawMesures.getHeadings()){
            if (measure.equals("Area")){
                customMesures.addValue(name_object +" "+measure+" (pixel)", d2s(rawMesures.getValue(measure, nucleus)));
                customMesures.addValue(name_object +" "+measure+" ("+calibration.getUnit()+")", d2s(rawMesures.getValue("Area", nucleus)*calibration.getValue()));
            }else if (measure.equals("IntDen")){
                continue;
            }else{
                customMesures.addValue(name_object +" "+measure,d2s(rawMesures.getValue(measure, nucleus)));
            }
        }
    }

    public void setResultsThresholdSpot(ResultsTable rawMesures, ResultsTable customMesures) {
        for (String measure : rawMesures.getHeadings()){
            switch (measure) {
                case "Area":
                    customMesures.addValue(name_object + " threshold "+ measure + " (pixel)", d2s(sum(rawMesures.getColumn(measure))));
                    customMesures.addValue(name_object + " threshold "+ measure + " (" + calibration.getUnit() + ")", d2s(sum(rawMesures.getColumn(measure)) * calibration.getValue()));
                    break;
                case "IntDen":
                    continue;
                case "Mean":
                    customMesures.addValue(name_object + "threshold Mean of " + measure, d2s(mean(rawMesures.getColumn(measure))));
                    break;
                case "RawIntDen":
                    customMesures.addValue(name_object+ " threshold "+ measure,d2s(sum(rawMesures.getColumn(measure))));
                default:
                    customMesures.addValue(name_object + " threshold "+ measure, d2s(mean(rawMesures.getColumn(measure))));
                    break;
            }
        }
        customMesures.addValue(name_object+ " Arithmetic Mean",d2s(sum(rawMesures.getColumn("RawIntDen"))/sum(rawMesures.getColumn("Area"))));
    }

    public double sum(double[] values){
        double sum=0;
        for (double value: values) {
            sum+= value;
        }
        return sum;
    }

    public double mean(double[] values){
        return sum(values)/values.length;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        Detector detector = new Detector(DAPI,"test","Nucleus",new Calibration("None",1,"pix"));
        detector.setzStackParameters("Maximum projection",0,DAPI.getNSlices());
        ImagePlus test = detector.getThresholdMask(detector.getImage(),"Li",1000);
    }
}
