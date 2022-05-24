package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;


// TODO Cellpose gives entire cell, so for cytoplasm needs to remove nuclei
// TODO add progress bar
// TODO create composite image with good channel
// lien to nucleiPanel
public class CytoDetector {
    private final ImagePlus image; /*Original image*/
    private ImagePlus imageToMeasure; /*Projected or not image, that will be the one measured*/
    private ImagePlus imageToAnalyze; /*Binary mask that particle analyzer use to count and return the ROIs*/
    private final String name_experiment; /*Name of experiment (common with spot images)*/
    private final Detector detector; /*Helper class that do the projection, thresholding and particle analyzer that are common with the spots*/
    private final String resultsDirectory; /*Directory to save results if necessary*/
    private final boolean showImage; /*Display or not the images (projection and binary)*/
    private boolean deeplearning; /*use deeplearning, if false use thresholding*/
    private int minSizeDLNuclei;
    private String dLMethod;
    private boolean useWatershed; /*TODO*/
    private Roi[] nucleiRois;
    private Analyzer analyzer;
    private RoiManager roiManagerNuclei;
    private ResultsTable rawMesures;
    private boolean finalValidation;
    private boolean useMacro;
    private String macroText;
    private boolean excludeOnEdges;

    /**
     * Constructor with basic parameters, the other are initialized only if needed
     * @param image image to analyze
     * @param name_experiment name without channel
     * @param measureCalibration : calibration to use for ResultTable
     * @param resultsDir : directory for saving results
     * @param showImage : display or not of the images
     */
    public CytoDetector(ImagePlus image, String name_experiment, MeasureCalibration measureCalibration, String resultsDir, boolean showImage) {
        this.image = image;
        this.resultsDirectory =resultsDir;
        this.showImage=showImage;
        if (name_experiment.endsWith("_")){
            this.name_experiment = name_experiment.substring(0,name_experiment.length()-1);
        }else {
            this.name_experiment=name_experiment;
        }
        detector = new Detector(image, "Cytoplasm", measureCalibration); /*TODO cell or cytoplasm ?*/
    }

    /**
     * Set all parameters for projection if necessary
     * @param zStackProj Method of projection
     * @param zStackFirstSlice First slice of stack to use
     * @param zStackLastSlice Last slice of stack to use
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice,int zStackLastSlice){
        detector.setzStackParameters(zStackProj,zStackFirstSlice,zStackLastSlice);
    }

    /**
     * Set projection method and the slices to use with default values (1 and last slice)
     * @param zStackProj Method of projection
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     * Set parameters for macro if necessary
     * @param macroText : macro text to use
     */
    public void setPreprocessingMacro(String macroText){
        this.useMacro = true;
        this.macroText = macroText;
    }

    /**
     * TODO
     * @param minSizeDLNuclei
     * @param dlMethod
     * @param excludeOnEdges
     */
    public void setDeeplearning(int minSizeDLNuclei, String dlMethod, boolean excludeOnEdges) {
        this.deeplearning = true;
        this.minSizeDLNuclei = minSizeDLNuclei;
        this.dLMethod = dlMethod;
        this.excludeOnEdges = excludeOnEdges;
    }

    public void preview(){
        ImagePlus preprocessed = getPreprocessing();
        if (preprocessed!=null){
            preprocessed.show();
//            launch cellpose command to obtain mask
                /*cyto channel = 0 for gray*/
                /*nuclei channel = 0 for none*/
            Cellpose cellpose = new Cellpose(preprocessed,minSizeDLNuclei,dLMethod,excludeOnEdges);
            cellpose.analysis();
            cellpose.getCellposeOutput().show();
            IJ.log("finished");
            new WaitForUserDialog("Preview is done").show();
        }
        int[] ids = WindowManager.getIDList();
        for (int id : ids) {
            ImagePlus image = WindowManager.getImage(id);
            image.changes = false;
            image.close();
        }
    }

    private ImagePlus getPreprocessing() {
        if (detector.getImage()!=null){
            IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
            this.imageToMeasure = detector.getImage(); /*detector class does the projection if needed*/
            if (showImage){
                this.imageToMeasure.show(); /*show image that will be measured*/
            }
//      MACRO : apply custom commands of user
            if (useMacro){
                IJ.selectWindow(imageToMeasure.getID());
                IJ.runMacro("setBatchMode(true);"+macroText+"setBatchMode(false);");
                imageToMeasure = WindowManager.getCurrentImage();
//            imageToMeasure.show();
            }
//         MEDIAN FILTER : reduce noise
            //        new RankFilters().rank(filteredProjection.getProcessor(),5,RankFilters.MEDIAN);
            return imageToMeasure.duplicate();
        }else return null;
    }
}
