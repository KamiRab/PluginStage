import ij.ImagePlus;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import static ij.plugin.frame.RoiManager.getInstance;
import static ij.process.AutoThresholder.Method.Default;

public class NucleiDetector {
    private ImagePlus image;
    private boolean zStack;
    private String zStackProj;
    private boolean deeplearning;
    private String thrMethod;
    private double minSizeNucleus;
    private boolean useWatershed;
    private boolean preview;

    public NucleiDetector(ImagePlusDisplay image, boolean zStack, String zStackProj, boolean deeplearning, String thrMethod, double minSizeNucleus, boolean useWatershed) {
        this.image = image.getImagePlus();
        this.zStack = zStack;
        this.zStackProj = zStackProj;
        this.deeplearning = deeplearning;
        this.thrMethod = thrMethod;
        this.minSizeNucleus = minSizeNucleus;
        this.useWatershed = useWatershed;
    }

    public void run(){
        //        OPEN DAPI IMAGE
        image.show();

        // PROJECTION
        ImagePlus threshold_IP;
        ImagePlus projection_IP;
        if (zStack){
            if (zStackProj.equals("Maximum projection")){
                projection_IP = ZProjector.run(image, "max"); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
            } else {
                projection_IP = ZProjector.run(image, "sd"); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
            }
            projection_IP.show(); /*image projetée*/
            threshold_IP = new Duplicator().run(projection_IP); /*Duplique image au cas où*/ /*==duplicate()*/
        } else {
            projection_IP=image;
            threshold_IP= image;
        }

        // SEUILLAGE
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*recupere processor alias image*/
        threshold_proc.setAutoThreshold(thrMethod);/*definit methode et bkg foncé (par defaut dark)*/
        threshold_proc.autoThreshold(); /*crée masque binaire avec thr*/
        threshold_IP.show(); /*Montre image binaire : correspond a la creation d'un masque*/

        // Analyse particule
        ResultsTable resultsNuclei = new ResultsTable(); /*creation d'une resultTable pour PA*/
        RoiManager roiManager_nuclei = getInstance(); /*recupere ROImanager actuel*/
        if (roiManager_nuclei == null) { /*si aucun ROiM trouvé, en crée un*/
            roiManager_nuclei = new RoiManager();/*creation si besoin est, d'un ROI manager pour l'avoir en variable*/
        } else {
            roiManager_nuclei.reset();
        }
        ParticleAnalyzer.setRoiManager(roiManager_nuclei); /*precise ROImanager à utiliser*/
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS + ParticleAnalyzer.ADD_TO_MANAGER + ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, 0, resultsNuclei, 1000, 999999);
        /*précise param a utiliser : actuellement créé overlay, ajouter ROI au ROIm, ne compte pas les particules partielles, ne mesure rien (je crois ?), taille minimale de particule = 1000*/

        threshold_proc.setAutoThreshold(Default, true); /*besoin de recreer threshold, vu que image est devenu binaire */
        particleAnalyzer.analyze(threshold_IP); /*recupere particules dans ROI + mesures*/

        resultsNuclei.reset();/*vide tableau*/

        int number_nuclei = roiManager_nuclei.getCount(); /*recupere nombre de particules*/
            Analyzer analyzer = new Analyzer(projection_IP, Measurements.MEAN + Measurements.AREA, resultsNuclei); /*precise mesures à faire et image sur laquelle faire*/
            //Iteration sur ROI manager pour mesure
            for (int i = 0; i < number_nuclei; i++) {
                roiManager_nuclei.select(projection_IP, i);
                analyzer.measure();
            }
            resultsNuclei.show("Results nuclei");
    }
}
