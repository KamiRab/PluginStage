package Detectors;

import Helpers.MeasureCalibration;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.io.File;

public class CytoDetector {

    private final Roi[] nucleiRois;
    private ImagePlus cellLabeledImage;
    private String nameExperiment;
    private MeasureCalibration measureCalibration;
    private String resultsDir;
    private boolean showImage;

    public CytoDetector(ImagePlus cellLabeledImage, Roi[] nucleiRois, String nameExperiment, MeasureCalibration measureCalibration, String resultsDir, boolean showImage) {
        this.nucleiRois = nucleiRois;
        this.cellLabeledImage = cellLabeledImage;
        this.nameExperiment = nameExperiment;
        this.measureCalibration = measureCalibration;
        this.resultsDir = resultsDir;
        this.showImage = showImage;
    }

    public boolean prepare(){
        ImagePlus cellImage = cellLabeledImage.duplicate();
        ImageProcessor cellProc = cellImage.getProcessor();
        cellProc.setColor(0);
        for (Roi nucleiRoi : nucleiRois){
            cellProc.fill(nucleiRoi);
        }
        cellImage.show();
        return false;
    }

//    TODO soustrait image noyau a cell avec NAN quand noyau
//    TODO

    public static void main(String[] args) {
        ImagePlus cytoImage = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w21 FITC.TIF");
        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\Images").mkdirs();
        new File("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Results\\ROI").mkdirs();
        CellDetector cellDetector = new CellDetector(cytoImage, "test", new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", true);
        cellDetector.setzStackParameters("Maximum projection");
        cellDetector.setDeeplearning(200, "cyto2", true,true);
        ImagePlus DAPI = IJ.openImage("C:\\Users\\Camille\\Downloads\\Camille_Stage2022\\Macro 2_Foci_Cytoplasme\\Images\\Cell_02_w31 DAPI 405.TIF");
        NucleiDetector nucleiDetector = new NucleiDetector(DAPI, "WT_HU_Ac-2re--cell003", new MeasureCalibration(), "C:\\Users\\Camille\\Downloads\\Camille_Stage2022", true);
        nucleiDetector.setzStackParameters("Maximum projection");
        nucleiDetector.setSegmentation(false,true);
        nucleiDetector.setDeeplearning(100,"cyto2",true);
        nucleiDetector.prepare();
        cellDetector.setNucleiDetector(nucleiDetector);
        cellDetector.prepare();
        new CytoDetector(cellDetector.getCellposeOutput(),nucleiDetector.getRoiArray(),"test",new MeasureCalibration(),"C:\\Users\\Camille\\Downloads\\Camille_Stage2022",true).prepare();
    }
}
