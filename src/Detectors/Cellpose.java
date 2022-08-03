package Detectors;

import ch.epfl.biop.wrappers.cellpose.CellposeTaskSettings;
import ch.epfl.biop.wrappers.cellpose.DefaultCellposeTask;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.io.FileSaver;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.awt.*;
import java.io.File;
import java.util.stream.IntStream;


//TODO directory
/**
 * Class based on https://github.com/BIOP/ijl-utilities-wrappers/blob/master/src/main/java/ch/epfl/biop/wrappers/cellpose/ij2commands/Cellpose_SegmentImgPlusOwnModelAdvanced.java
 * It has been simplified for images with only one frame and to be used without GUI.
 * It is used to launch cellpose in command line.
 * All parameters are set by default other than the method and diameter and channels to use
 * TODO essayer cellpose sur foci
 */
public class Cellpose {
    private final ImagePlus imagePlus;
    private final int minSizeNucleus;
    private final String model;
    private final int nucleiChannel;
    private final int cytoChannel;
    private final boolean excludeOnEdges;
    private ImagePlus cellposeOutput;
    private RoiManager cellposeRoiManager;

    public Cellpose(ImagePlus imagePlus, int minSizeNucleus, String model, int cytoChannel, int nucleiChannel, boolean excludeOnEdges){
        this.imagePlus=imagePlus;
        this.minSizeNucleus = minSizeNucleus;
        this.model = model;
        this.nucleiChannel=nucleiChannel;
        this.cytoChannel=cytoChannel;
        this.excludeOnEdges = excludeOnEdges;
    }

    public Cellpose(ImagePlus imagePlus, int minSizeNucleus, String model, boolean excludeOnEdges){
        this(imagePlus,minSizeNucleus,model,0,0, excludeOnEdges);
    }


    public ImagePlus getCellposeOutput(){
        return cellposeOutput;
    }

    public RoiManager getCellposeRoiManager() {
        return cellposeRoiManager;
    }

    public void analysis(){
        DefaultCellposeTask cellposeTask = new DefaultCellposeTask();

//        Calibration calibration = imagePlus.getCalibration();

        File cellposeTempDir = getCellposeTempDir();
        setSettings(cellposeTask, cellposeTempDir);
        try {
//              Save image in CellposeTempDir
            File t_imp_path = new File(cellposeTempDir, imagePlus.getShortTitle() + ".tif");
            FileSaver fs = new FileSaver(imagePlus);
            fs.saveAsTiff(t_imp_path.toString());

//              Prepare path for mask output
            File cellpose_imp_path = new File(cellposeTempDir,imagePlus.getShortTitle()+"_cp_masks.tif");
//              Prepare path for additionnal text file created by Cellpose
            File cellpose_outlines_path = new File(cellposeTempDir,imagePlus.getShortTitle()+"_cp_outlines.txt");

            IJ.setDebugMode(true);
            cellposeTask.run();
            IJ.setDebugMode(false);

//              Open output mask file
            ImagePlus cellposeAllRois = IJ.openImage(cellpose_imp_path.toString());
//            Get Rois
            label2Roi(cellposeAllRois);
            cellposeOutput=Detector.labeledImage(cellposeAllRois.getWidth(),cellposeAllRois.getHeight(),cellposeRoiManager.getRoisAsArray());
            cellposeOutput.setTitle(imagePlus.getShortTitle() + "-cellpose");

//            Delete images and temp directory
            cellpose_imp_path.delete();
//            if (!imp_delete) IJ.log(cellpose_imp_path +" could not be deleted");
            cellpose_outlines_path.delete();
//            if (!outlines_delete) IJ.log(cellpose_outlines_path +" could not be deleted");
            cellposeTempDir.delete();
//            if (!tempdirDelete) IJ.log("Cellpose temp directory could not be deleted");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setSettings(DefaultCellposeTask cellposeTask, File cellposeTempDir) {
        CellposeTaskSettings settings = new CellposeTaskSettings();
        settings.setFromPrefs(); /* get info on if to use GPU or CPU and other particularities*/
        switch (model) {
            case "nuclei":
                settings.setChannel1(nucleiChannel);
                //settings.setChannel2(-1) ;
                break;
            case "bact_omni":
                settings.setChannel1(cytoChannel);
                break;
            case "cyto":
            case "cyto2":
            case "cyto2_omni":
                System.out.println("cyto_channel:" + cytoChannel + ";nuclei_channel:" + nucleiChannel);
                settings.setChannel1(cytoChannel);
                settings.setChannel2(nucleiChannel);
                break;
            default:
                settings.setChannel1(cytoChannel);
                settings.setChannel2(nucleiChannel);
                break;
        }
        settings.setModel(model);
        settings.setDatasetDir(cellposeTempDir.toString());
        settings.setDiameter(minSizeNucleus);
        settings.setDiamThreshold(12.0); /*default value*/

        cellposeTask.setSettings(settings);
    }

    private File getCellposeTempDir() {
        String tempDir = IJ.getDirectory("Temp");
        File cellposeTempDir = new File(tempDir,"cellposeTemp");
        if (cellposeTempDir.exists()){
            File[] contents = cellposeTempDir.listFiles();
            if (contents !=null) for (File f : contents) {
                boolean delete = f.delete();
                if (!delete) IJ.error("Files could not be deleted from temp directory");
            }
        } else {
            boolean cellposeMkdir = cellposeTempDir.mkdir();
            if (!cellposeMkdir){
                IJ.error("The temp directory could not be created");
            }
        }
        return cellposeTempDir;
    }

    /**
     * Based on <a href="https://github.com/BIOP/ijp-LaRoMe/blob/master/src/main/java/ch/epfl/biop/ij2command/Labels2Rois.java">...</a>
     * Simplified for only one frame, one slice and 1 channel
     * Can exclude on edges
     * Creates the RoiManager containing all particle Rois
     */
    public void label2Roi(ImagePlus cellposeIP){
        ImageProcessor cellposeProc = cellposeIP.getProcessor();
        Wand wand = new Wand(cellposeProc);

//        Set RoiManager
        cellposeRoiManager = RoiManager.getRoiManager();
        cellposeRoiManager.reset();

//        Create range list
        int width = cellposeProc.getWidth();
        int height = cellposeProc.getHeight();

        int[] pixel_width = new int[ width ];
        int[] pixel_height = new int[ height ];

        IntStream.range(0,width-1).forEach(val -> pixel_width[val] = val);
        IntStream.range(0,height-1).forEach(val -> pixel_height[val] = val);

        /*
         * Will iterate through pixels, when getPixel > 0 ,
         * then use the magic wand to create a roi
         * finally set value to 0 and add to the roiManager
         */

        // will "erase" found ROI by setting them to 0
        cellposeProc.setColor(0);

        for ( int y_coord : pixel_height) {
            for ( int x_coord : pixel_width) {
                if ( cellposeProc.getPixel( x_coord, y_coord ) > 0.0 ){
                    // use the magic wand at this coordinate
                    wand.autoOutline( x_coord, y_coord );

                    // if there is a region , then it has npoints
//                    There can be problems with very little ROIs, so threshold of 20 points
                    if ( wand.npoints > 20 ) {
                        // get the Polygon, fill with 0 and add to the manager
                        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
                        roi.setPosition( cellposeIP.getCurrentSlice() );
                        // ip.fill should use roi, otherwise make a rectangle that erases surrounding pixels
                        cellposeProc.fill(roi);
                        if (excludeOnEdges){
                            Rectangle r = roi.getBounds();
                            if (r.x<=1||r.y<=1||r.x+r.width>=cellposeProc.getWidth()-1||r.y+r.height>=cellposeProc.getHeight()-1){
                                continue;
                            }
                        }
                        cellposeRoiManager.addRoi( roi );
                    }
                }
            }
        }
//        cellposeRoiManager.runCommand( cellposeIP , "Show All" );
    }

    /**
     * Tests
     * @param args none
     */
    public static void main(String[] args) {
        ImagePlus FITC = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/test/MAX_Cell_02_w21 FITC-1.tif");
        ImagePlus FITC_cellpose = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 2_Foci_Cytoplasme/Images/test/MAX_Cell_02_w21 FITC-1_cp_masks.tif");
        Cellpose cellpose = new Cellpose(FITC,200,"cyto 2",true);
        cellpose.label2Roi(FITC_cellpose);
        FITC.show();
        cellpose.getCellposeRoiManager().show();
//        Detector detector = new Detector(FITC,"Nucleus");
//        detector.setMeasureCalibration(new MeasureCalibration());
//        detector.setzStackParameters("Maximum projection",0,DAPI.getNSlices());
//        Cellpose cellpose = new Cellpose(detector.getImage(),200,"cyto2", false);
//        cellpose.analysis();
//        detector.getImage().show();
//        cellpose.getCellposeRoiManager().toFront();
//        ImagePlus cellposeOutput = cellpose.getCellposeOutput();
//        cellposeOutput.show();
//        cellposeOutput.setDisplayRange(0,35);
//        cellposeOutput.updateAndDraw();

    }
}
