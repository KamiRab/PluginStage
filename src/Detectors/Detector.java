package Detectors;

import Helpers.Calibration;
import ij.IJ;
import ij.ImagePlus;
import ij.measure.ResultsTable;
import ij.plugin.ZProjector;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import static ij.IJ.d2s;

/**
 * Class with common methods for spots and nuclei :
 * - Z-projection
 * - Thresholding
 * - Analyze particles
 * - Set results
 */
public class Detector {
    private ImagePlus image;
    private final String name_image;
    private final String name_object;
    private final Calibration calibration;

    //Projection parameters
    private String zStackProjMethod;
    private int zStackFirstSlice;
    private int zStackLastSlice;

    //Thresholding parameters
    private String thresholdMethod;
    private boolean excludeOnEdges;
    private double minSizeParticle;

    /**
     * Constructor with basics
     * @param image : image to analyze/measure
     * @param name_object : "Nucleus" or name of protein
     * @param calibration : Helpers.Calibration to use
     */
    public Detector(ImagePlus image, String name_object,Calibration calibration) {
        this.image = image;
        this.name_image = image.getTitle();
        this.name_object=name_object;
        this.calibration = calibration;
    }

//    SETTER

    /**
     * if projection to do, sets the necessary parameters and do the projection
     * @param zStackProjMethod : method of projection
     * @param zStackFirstSlice : first slice of stack to use for projection
     * @param zStackLastSlice : last slice of stack to use for projection
     */
    public void setzStackParameters(String zStackProjMethod, int zStackFirstSlice, int zStackLastSlice) {
        this.zStackProjMethod = zStackProjMethod;
        this.zStackFirstSlice = zStackFirstSlice;
        this.zStackLastSlice = zStackLastSlice;
        projection();
    }

    /**
     * if thresholding to do, sets the necessary parameters for thresholding and particle analysis
     * @param thresholdMethod : method to get value of threshold
     * @param excludeOnEdges : excludes particles on edges of selection/image ?
     * @param minSizeParticle : minimum size of particle to analyze
     *                        Maximum size of particle if the max value possible
     */
    public void setThresholdParameters(String thresholdMethod, boolean excludeOnEdges,double minSizeParticle){
        this.thresholdMethod = thresholdMethod + " dark"; /*TODO is the bkg always dark ?*/
        this.excludeOnEdges = excludeOnEdges;
        this.minSizeParticle = minSizeParticle;
    }

//    GETTER

    public ImagePlus getImage() {
        return image;
    }

//    FUNCTIONS/METHODS

    /**
     * If the original image is a stack, projects it in one image for further analysis
     * The projections proposed are maximum projection or standard deviation projection
     */
    private void projection() {
        if (zStackProjMethod.equals("Maximum projection")) {
            this.image = ZProjector.run(image, "max", zStackFirstSlice, zStackLastSlice); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        } else {
            this.image = ZProjector.run(image, "sd", zStackFirstSlice, zStackLastSlice); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        }
        rename_image(this.image,"projection");
    }

    /**
     * Analyze the binary image to detect particles (here spots or nuclei)
     * It scans the image/selection until it finds the edge on an object(different color)
     * Then it outlines the object using the wand tool and measures it.
     * It fills the found object to make it invisible, so it is not detected another time.
     * The scan ends when the end of image or selection is reached
     * @param threshold_IP : binary image with particles to find
     * @return RoiManager that contains the Rois corresponding to the particles found
     */
    public RoiManager analyzeParticles(ImagePlus threshold_IP) {
//        Get RoiManager
        RoiManager roiManager = RoiManager.getInstance();
        if (roiManager == null) { /*if no instance of roiManager, creates one*/
            roiManager = new RoiManager();
        } else { /*if already exists, empties it*/
            roiManager.reset();
        }

//        Set options for particle analyzer
        ParticleAnalyzer.setRoiManager(roiManager); /*precise with RoiManager to use*/
        int analyzer_option = ParticleAnalyzer.SHOW_OVERLAY_MASKS+ParticleAnalyzer.ADD_TO_MANAGER;
        if (excludeOnEdges) analyzer_option+= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
        ParticleAnalyzer particleAnalyzer=new ParticleAnalyzer(analyzer_option,0,null,minSizeParticle,Integer.MAX_VALUE);

//        Analyze
        ImageProcessor threshold_proc = threshold_IP.getProcessor();
        threshold_proc.setThreshold(128,255); /*Needs to set threshold for the binary image*/
        particleAnalyzer.analyze(threshold_IP); /*adds particles found to RoiManager and add overlay (see options)*/
        return roiManager;
    }

    /**
     * Creates a binary image to differentiate the objects to analyze
     * @param image : image to threshold
     * @return binary image
     */
    public ImagePlus getThresholdMask(ImagePlus image) {
//        GET IMAGE TO THRESHOLD
        ImagePlus threshold_IP= image.duplicate();
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*get processor*/

//        DEFINE THRESHOLD VALUES THROUGH METHOD GIVEN
        /*TODO toujours bkg foncé ?*/
        threshold_proc.setAutoThreshold(thresholdMethod + " dark");
//        IJ.log(""+threshold_proc.getAutoThreshold());

//        GET BINARY MASK OF THRESHOLD IMAGE THROUGH PARTICLE ANALYZER
//        Set options
        int analyzer_option = ParticleAnalyzer.SHOW_MASKS;
        if (excludeOnEdges) analyzer_option+= ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES;
//        Analyze
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(analyzer_option,0,null,minSizeParticle,Integer.MAX_VALUE);
        particleAnalyzer.analyze(threshold_IP);
//        Get binary mask output and renames it
        ImagePlus mask_IP = particleAnalyzer.getOutputImage();
        rename_image(mask_IP,"binary_mask");
        return mask_IP;
    }

    /**
     * Renames image with original image prefix (without extension)
     * + modification identifier suffix (+if present, the extension)
     * @param imageToRename : ImagePlus that will be renamed
     * @param toAdd : suffix to add
     */
    public void rename_image (ImagePlus imageToRename, String toAdd){
        int lastPoint = name_image.lastIndexOf("."); /*get position of last point*/
        String getTitleWOExtension;
        String extension;
        if (lastPoint != -1){ /*a point is present in the string*/
            getTitleWOExtension = name_image.substring(0,lastPoint);
            extension = name_image.substring(lastPoint);
            imageToRename.setTitle(getTitleWOExtension +"_"+toAdd+extension);
        }else { /*no extension to add*/
            imageToRename.setTitle(name_image+"_"+toAdd);
        }
    }

    /**
     * Add results from a ResultTable to another for clearer headings and adding of calibrated area.
     * Removes IntDen results, as it is equal to RawIntDen value
     * @param rawMeasures : ResultTable with all the results for all the nucleus
     * @param customMeasures : ResultTable that will contain the final results
     * @param nucleus : line number of the nucleus for which the results have to be customized
     */
    public void setResultsAndRename(ResultsTable rawMeasures, ResultsTable customMeasures, int nucleus) {
        for (String measure : rawMeasures.getHeadings()){
            if (measure.equals("Area")){
                customMeasures.addValue(name_object +" "+measure+" (pixel)", d2s(rawMeasures.getValue(measure, nucleus)));
                customMeasures.addValue(name_object +" "+measure+" ("+calibration.getUnit()+")", d2s(rawMeasures.getValue("Area", nucleus)*calibration.getPixelArea()));
            }else if (measure.equals("IntDen")){
                continue;
            }else{
                customMeasures.addValue(name_object +" "+measure,d2s(rawMeasures.getValue(measure, nucleus)));
            }
        }
    }

    /**
     * Summarize results : from an entire ResultsTable create one line for another resultTable
     * For area, adding of with calibration column
     * Removal of IntDen column (RawIntDen is equal)
     * Differenciation between mean of mean and arithmetic mean
     * RawIntDen and Area are summed, the other use means
     * @param rawMeasures : ResultTable with measures of all spots not summarized
     * @param customMeasures : ResultTable that will contain the summarized results
     */
    public void setSummarizedResults(ResultsTable rawMeasures, ResultsTable customMeasures) {
        for (String measure : rawMeasures.getHeadings()){
            switch (measure) {
                case "Area":
                    customMeasures.addValue(name_object + " threshold "+ measure + " (pixel)", d2s(sum(rawMeasures.getColumn(measure))));
                    customMeasures.addValue(name_object + " threshold "+ measure + " (" + calibration.getUnit() + ")", d2s(sum(rawMeasures.getColumn(measure)) * calibration.getPixelArea()));
                    break;
                case "IntDen":
                    continue;
                case "Mean":
                    customMeasures.addValue(name_object + "threshold Mean of " + measure, d2s(mean(rawMeasures.getColumn(measure))));
                    break;
                case "RawIntDen":
                    customMeasures.addValue(name_object+ " threshold "+ measure,d2s(sum(rawMeasures.getColumn(measure))));
                default:
                    customMeasures.addValue(name_object + " threshold "+ measure, d2s(mean(rawMeasures.getColumn(measure))));
                    break;
            }
        }
        customMeasures.addValue(name_object+ " Arithmetic Mean",d2s(sum(rawMeasures.getColumn("RawIntDen"))/sum(rawMeasures.getColumn("Area"))));
    }

    /**
     * Sum an array
     * @param values : array of values
     * @return sum of the array
     */
    public double sum(double[] values){
        double sum=0;
        for (double value: values) {
            sum+= value;
        }
        return sum;
    }

    /**
     * Mean an array
     * @param values : array of values
     * @return the mean of the array
     */
    public double mean(double[] values){
        return sum(values)/values.length;
    }

    /**
     * Test for the class
     * @param args arguments to give to main. Here always null
     */
    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        Detector detector = new Detector(DAPI,"Nucleus",new Calibration());
        detector.setzStackParameters("Maximum projection",0,DAPI.getNSlices());
        detector.setThresholdParameters("Li",true,1000);
        DAPI.show();
    }
}
