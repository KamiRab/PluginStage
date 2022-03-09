import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ZProjector;
import ij.plugin.filter.*;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

import static ij.plugin.frame.RoiManager.*;
import static ij.process.AutoThresholder.Method.Default;
import static ij.process.AutoThresholder.Method.Li;

/*TODO transformer en class plugin pour ouvrir image ?*/
/*TODO class image avec zprojector fait par defaut ?*/
public class Nuclei_detect implements PlugInFilter {
    ImagePlus imp;
    public int setup(String s, ImagePlus imagePlus) {
        /*TODO Dialogue*/
        this.imp = imagePlus;
//        return DOES_ALL;
        return DOES_ALL+STACK_REQUIRED;
        /*8G ?*/
    }

    public void run(ImageProcessor imageProcessor) {
//        TEST OPEN IMAGE
        ImagePlus test = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        test.show();

        // PROJECTION
        ImagePlus projection_IP = ZProjector.run(test,"max"); /*projette stack en une seule image avec à chaque pixel correspondant au maximum d'intensité*/
        projection_IP.show(); /*image projetée*/
        ImagePlus threshold_IP = new Duplicator().run(projection_IP); /*Duplique image au cas où*/ /*==duplicate()*/

        // SEUILLAGE
        ImageProcessor threshold_proc = threshold_IP.getProcessor(); /*recupere processor alias image*/
        threshold_proc.setAutoThreshold(Li,true);/*definit methode et bkg foncé*/
        threshold_proc.autoThreshold(); /*crée masque binaire avec thr*/
        threshold_IP.show(); /*Montre image binaire : correspond a la creation d'un masque*/
//        threshold_proc.getAutoThreshold(); /*threshold is done*/
//        ImageProcessor binary_mask_IP = threshold_proc.createMask(); /**/
//        ImagePlus masked_thr = new ImagePlus("mask",binary_mask_IP);
//        binary_mask_IP.setAutoThreshold(Default,true);
//        masked_thr.show();
//        IJ.log("seuil ="+seuil);
//        IJ.log("min"+binary_mask_IP.getMinThreshold());
//        IJ.log("max"+binary_mask_IP.getMaxThreshold());

        // Analyse particule
        ResultsTable resultsTable = new ResultsTable(); /*creation d'une resultTable pour PA*/
        RoiManager roiManager_nuclei = getInstance(); /*recupere ROImanager actuel*/
        if (roiManager_nuclei == null){ /*si aucun ROiM trouvé, en crée un*/
            roiManager_nuclei = new RoiManager();/*creation si besoin est, d'un ROI manager pour l'avoir en variable*/
        } else {
            roiManager_nuclei.reset();
        }
        ParticleAnalyzer.setRoiManager(roiManager_nuclei); /*precise ROImanager à utiliser*/
        ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(ParticleAnalyzer.SHOW_OVERLAY_MASKS+ParticleAnalyzer.ADD_TO_MANAGER+ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES,0,resultsTable,1000,999999);
        /*précise param a utiliser : actuellement créé overlay, ajouter ROI au ROIm, ne compte pas les particules partielles, ne mesure rien (je crois ?), taille minimale de particule = 1000*/
//        particleAnalyzer.analyze(masked_thr);
        threshold_proc.setAutoThreshold(Default,true); /*besoin de recreer threshold, vu que image est devenu binaire */
        particleAnalyzer.analyze(threshold_IP); /*recupere particules dans ROI + mesures*/
//        IJ.wait(1000);
        int number_nuclei = roiManager_nuclei.getCount(); /*recupere nombre de particules*/
//        IJ.log("There are "+ number_nuclei + " nuclei");

        resultsTable.reset();/*vide tableau*/

        Analyzer analyzer = new Analyzer(projection_IP,Measurements.ALL_STATS,resultsTable); /*precise mesures à faire et image sur laquelle faire*/
        //Iteration sur ROI manager pour mesure
        for (int i = 0; i < number_nuclei; i++) {
            roiManager_nuclei.select(projection_IP,i);
            analyzer.measure();
        }
        resultsTable.show("Results");

/*parametre DAPI, choix ouverte/dossier et quelle image correspond a quoi*/
        /*creer JPanel segmentation/deeplearning pour pouvoir switcher*/
        /*differentes methodes de seuillage*/
        /*preview pour seuillage : taille noyau (rond comme cellpose ou direct sur image)*/

        /*projection : proposer std deviation en plus de max*/
        /*reflechir a integration cytoplasme*/
        /*taille minimal noyau*/
        /*detection spot : find maxima (prominence) ou threshold (methode a demande + taille minimal spot)*/
        /*garder 1ere/derniere image en option cachée*/
        /*taille rolling ball : soit implicite soit en mode preview en expert mode*/
        /*option run macro pour preprocessing*/

        /*affichage des parametres utilisé dnas IJ.log quand lance programme et sur quels fichiers il a travaillé*/
        /*dans resulttable, nom de l'image travaillé*/
        /*sauvegarder automatiquement log*/
        /*sauvegarder parametres souvent utilisé ==> prefs : sauvegarde preférence avec set et essaie de récupérer avec get (!! mettre valeur par défaut)*/
        /*si veut recuperer valeurs par défaut : reset pref ou recupere dans fichier imageJ*/




        ImagePlus test2_IP = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w11 CY5.TIF");
        ImagePlus projection2_IP = ZProjector.run(test2_IP,"max");
        projection2_IP.show();
        ImagePlus wobkg_IP = projection2_IP.duplicate();

        BackgroundSubtracter backgroundSubtracter = new BackgroundSubtracter();
        backgroundSubtracter.rollingBallBackground(wobkg_IP.getProcessor(),10,false,false,false,true,false); /*Hmm correctCorners ?*/
        wobkg_IP.show();

        /*Find maxima*/
        MaximumFinder maximumFinder = new MaximumFinder();
        for (int i = 0; i < number_nuclei-1; i++) {
            roiManager_nuclei.select(wobkg_IP,i);
            ImageProcessor wobkg_proc = wobkg_IP.getProcessor();
            Polygon maxima = maximumFinder.getMaxima(wobkg_proc,1000,true);
            int nb_maxima = maxima.npoints;
            PointRoi roi_maxima = new PointRoi(maxima);
            RoiManager roiManager_foci = new RoiManager();
            roiManager_foci.addRoi(roi_maxima);
            wobkg_IP.setRoi(roi_maxima);
            Analyzer foci_analyzer = new Analyzer(wobkg_IP,Measurements.MEAN + Measurements.INTEGRATED_DENSITY,)

//            ByteProcessor tryMaxima =  maximumFinder.findMaxima(wobkg_proc,1000,MaximumFinder.POINT_SELECTION,true);
//            new ImagePlus("Particule_"+i,tryMaxima).show();
        }
//        resultsTable.show("Results");
//        imageProcessor.setAutoThreshold(Otsu,true);
//        imageProcessor.getAutoThreshold();
    }
}
//d2s (limite nombre de decimal en string), pad ("001" a partir de 1)
//getID de windows manager pour connaitre fenetre d'images ouvertes

