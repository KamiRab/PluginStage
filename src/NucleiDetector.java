import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.EDM;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

public class NucleiDetector {
    private final ImagePlus image;
    private ImagePlus imageToMeasure;
    private ImagePlus imageToAnalyze;
    private final String name_experiment;
//    private final Calibration calibration;
    private boolean zStack =false;
    private final String directory;

    private boolean deeplearning;

    private String thresholdMethod;
    private double minSizeNucleus;
    private boolean useWatershed;
    private boolean excludeOnEdges;

//    private final boolean preview;
    private Roi[] nucleiRois;
    private Analyzer analyzer;
    private RoiManager roiManagerNuclei;
//    private ResultsTable resultsTableNuclei;
    private ResultsTable rawMesures;
    private boolean finalValidation;
    private final Detector detector;
    private boolean useMacro;
    private String macroText;
//    private final String [] measurements = new String[]{"Area", "Mean","RawIntDen"};

    /**
     *
     * @param image image to analyze
     * @param name_experiment name without channel
     * @param zStack is the image a z-stack ?
     * @param zStackProj if z-stack, method of projection to use
     * @param zStackFirstSlice if z-stack : first slice to use
     * @param zStackLastSlice if z-stack : last slice to use
     * @param deeplearning if true : use deep learning, else threshold
     * @param thresholdMethod if threshold : method to use
     * @param minSizeNucleus if threshold
     * @param useWatershed if threshold
     * @param excludeOnEdges if threshold
     * @param finalValidation TODO
     */
    public NucleiDetector(ImagePlus image, String name_experiment, boolean zStack, String zStackProj,int zStackFirstSlice,int zStackLastSlice, boolean deeplearning, String thresholdMethod, double minSizeNucleus, boolean useWatershed, boolean excludeOnEdges, /*boolean preview,*/ Calibration calibration,boolean finalValidation, String directory) {
        this(image,name_experiment/*,preview*/,calibration, directory);
        if (zStack){
            setzStackParameters(zStackProj,zStackFirstSlice,zStackLastSlice);
        }
        if(deeplearning){
            this.deeplearning = true;
        } else {
            setThresholdMethod(thresholdMethod,minSizeNucleus,useWatershed,excludeOnEdges,finalValidation);
        }
    }


    /**
     *
     * @param image TODO
     * @param name_experiment TODO
     */
    public NucleiDetector(ImagePlus image, String name_experiment, /*boolean preview,*/ Calibration calibration, String fromDir) {
        this.image = image;
        this.directory =fromDir;
        if (name_experiment.endsWith("_")){
            this.name_experiment = name_experiment.substring(0,name_experiment.length()-1);
        }else {
            this.name_experiment=name_experiment;
        }
        detector = new Detector(image,name_experiment, "Nucleus",calibration);
    }

    /**
     *
     * @param zStackProj TODO
     * @param zStackFirstSlice TODO
     * @param zStackLastSlice TODO
     */
    public void setzStackParameters(String zStackProj, int zStackFirstSlice,int zStackLastSlice){
        detector.setzStackParameters(zStackProj,zStackFirstSlice,zStackLastSlice);
        this.zStack=true;
    }

    public void setPreprocessingMacro(String macroText){
        this.useMacro = true;
        this.macroText = macroText;
    }


    /**
     *
     * @param zStackProj TODO
     */
    public void setzStackParameters(String zStackProj) {
        setzStackParameters(zStackProj, 1, image.getNSlices());
    }

    /**
     *
     * @param thrMethod TODO
     * @param minSizeNucleus TODO
     * @param useWatershed TODO
     * @param excludeOnEdges TODO
     */
    public void setThresholdMethod(String thrMethod,double minSizeNucleus,boolean useWatershed,boolean excludeOnEdges,boolean finalValidation) {
        this.deeplearning = false;
        this.thresholdMethod = thrMethod + " dark";
        this.useWatershed = useWatershed;
        this.minSizeNucleus = minSizeNucleus;
        this.excludeOnEdges = excludeOnEdges;
        this.finalValidation = finalValidation;
    }

    /**
     *
     * @return TODO
     */
    public Roi[] getRoiArray() {
        return nucleiRois;
    }

    public ImagePlus getImage() {
        return image;
    }

    public void preview(){
        preprocessing_threshold();
    }

    public void prepare(){
        if (deeplearning){
            IJ.error("Sorry not done yet");
        } else {
            preprocessing_threshold();
            // Analyse particule
//            this.resultsTableNuclei = new ResultsTable(); /*creation d'une resultTable pour PA*/
            this.rawMesures = new ResultsTable();
            roiManagerNuclei=detector.analyzeParticles(imageToAnalyze,excludeOnEdges,minSizeNucleus);
            if (finalValidation){
                roiManagerNuclei.runCommand("Show All");
                new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
            }
            nucleiRois =roiManagerNuclei.getRoisAsArray();
            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA+Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
        }
    }
    /**
     *
     */
    public void run(){
        if (deeplearning){
            IJ.error("Sorry not done yet");
        } else {
            preprocessing_threshold();
            // Analyse particule
//            this.resultsTableNuclei = new ResultsTable(); /*creation d'une resultTable pour PA*/
            this.rawMesures = new ResultsTable();
            roiManagerNuclei=detector.analyzeParticles(imageToAnalyze,excludeOnEdges,minSizeNucleus);
            if (finalValidation){
                roiManagerNuclei.runCommand("Show All");
                new WaitForUserDialog("Nuclei selection", "Delete nuclei : select the ROIs + delete").show();
            }
            nucleiRois =roiManagerNuclei.getRoisAsArray();
            analyzer = new Analyzer(imageToMeasure, Measurements.MEAN + Measurements.AREA+Measurements.INTEGRATED_DENSITY, rawMesures); /*precise mesures à faire et image sur laquelle faire*/
        }
        ResultsTable test = new ResultsTable();
        for (int nucleus = 0; nucleus < nucleiRois.length; nucleus++) {
            test.addValue("Name experiment", name_experiment);
            test.addValue("Cell name", "Cell name nr. "+ (nucleus+1));
            measureEachNuclei(nucleus, test);
            test.incrementCounter();
        }
    }

    public void measureEachNuclei(int nucleus,ResultsTable resultsTableFinal) {
        imageToMeasure.setRoi(nucleiRois[nucleus]);
        analyzer.measure();
        detector.setResultsNucleus(rawMesures,resultsTableFinal,nucleus);
    }


//    TODO mesureNuclei par ROI ==> besoin de globaliser analyser et results table
//    TODO Mesure noyau et prot dans meme boucle de Roi
//    TODO Save RT final a chaque fin d'exp, meme si veut tous les exp dans save
//    TODO preprocess puis boucle ROI
//    TODO remplit RT final ligne par ligne (copie toutes les colonnes une à une)


    /**
     *
     * @param binary_threshold_proc : processor of mask, already ByteProcessor
     * @return TODO
     */
    private ImagePlus getWatershed(ImageProcessor binary_threshold_proc) {
//        ByteProcessor binary_threshold_proc = threshold_proc.convertToByteProcessor();
        EDM edm = new EDM();
        edm.toWatershed(binary_threshold_proc);
        return new ImagePlus(image.getTitle()+"_watershed",binary_threshold_proc);
    }

    public void preprocessing_threshold(){

        IJ.run("Options...","iterations=1 count=1 black");
//        PROJECTION : convert stack to one image
        this.imageToMeasure = (zStack) ? detector.getImage() : image.duplicate();
        if (directory==null){
            this.imageToMeasure.show(); /*show image that will be measured*/
        }
        if (useMacro){
            IJ.selectWindow(imageToMeasure.getID());
            IJ.runMacro(macroText);
            imageToMeasure.show();
        }
//         MEDIAN FILTER : reduce noise
        ImagePlus filteredProjection = imageToMeasure.duplicate();
        new RankFilters().rank(filteredProjection.getProcessor(),5,RankFilters.MEDIAN);

//        OBTAIN BINARY MASK OF THRESHOLDED IMAGE
        ImagePlus mask_IP = detector.getThresholdMask(filteredProjection,thresholdMethod,minSizeNucleus);
        if (directory==null){
            mask_IP.show();
        } else {
            IJ.save(mask_IP,directory+"\\Results\\"+mask_IP.getTitle());
        }
        mask_IP.getProcessor().invertLut();
        IJ.run(mask_IP, "Fill Holes","");
        if (useWatershed){
            ImagePlus watersheded_mask = getWatershed(mask_IP.getProcessor());
            if (directory==null){
                watersheded_mask.show();
            } else {
                IJ.save(directory+"Results/"+watersheded_mask.getTitle());
            }
            imageToAnalyze = watersheded_mask;
        }else {
            imageToAnalyze = mask_IP.duplicate();
        }
    }

    /**
     *
     * @return TODO
     */
    public String getName_experiment() {
        return name_experiment;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI,"WT_HU_Ac-2re--cell003",/*false,*/new Calibration("No calibration",0.5,"pix"),"C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/");
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setThresholdMethod("Li",1000,false,true,false);
        nucleiDetector.run();
    }
}
