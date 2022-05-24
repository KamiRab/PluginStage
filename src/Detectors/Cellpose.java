package Detectors;

import Helpers.MeasureCalibration;
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

/**
 * Class based on https://github.com/BIOP/ijl-utilities-wrappers/blob/master/src/main/java/ch/epfl/biop/wrappers/cellpose/ij2commands/Cellpose_SegmentImgPlusOwnModelAdvanced.java
 * It has been simplified for images with only one frame and to be used without GUI.
 * It is used to launch cellpose in command line.
 * All parameters are set by default other than the method and diameter and channels to use
 * TODO modify for cytoplasm detection
 * TODO exclude on edges
 * TODO compatibility cellpose2 (new model)
 */
public class Cellpose {
    private final ImagePlus imagePlus;
    private final int minSizeNucleus;
    private final String model;
    private final int nucleiChannel;
    private final int cytoChannel;
    private final boolean excludeOnEdges;
    private ImagePlus cellposeOutput;
    public Cellpose(ImagePlus imagePlus, int minSizeNucleus, String model, int nuclei_channel,int cyto_channel, boolean excludeOnEdges){
        this.imagePlus=imagePlus;
        this.minSizeNucleus = minSizeNucleus;
        this.model = model;
        this.nucleiChannel=nuclei_channel;
        this.cytoChannel=cyto_channel;
        this.excludeOnEdges = excludeOnEdges;
    }

    public Cellpose(ImagePlus imagePlus,int minSizeNucleus, String model,boolean excludeOnEdges){
        this(imagePlus,minSizeNucleus,model,0,0,excludeOnEdges);
    }

    public ImagePlus getCellposeOutput(){
        return cellposeOutput;
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

            cellposeTask.run();

//              Open output mask file
            cellposeOutput = IJ.openImage(cellpose_imp_path.toString());
//            cellpose_imp.setCalibration(calibration);
            cellposeOutput.setTitle(imagePlus.getShortTitle() + "-cellpose");
//            cellpose_imp.show();

//            Delete images and temp directory
            boolean imp_delete = cellpose_imp_path.delete();
            if (!imp_delete) IJ.error(cellpose_imp_path +" could not be deleted");
            boolean outlines_delete = cellpose_outlines_path.delete();
            if (!outlines_delete) IJ.error(cellpose_outlines_path +" could not be deleted");
            boolean tempdirDelete = cellposeTempDir.delete();
            if (!tempdirDelete) IJ.error("Cellpose temp directory could not be deleted");
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
                System.out.println("cyto_channel:" + cytoChannel + ":nuclei_channel:" + nucleiChannel);
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
     * Based on https://github.com/BIOP/ijp-LaRoMe/blob/master/src/main/java/ch/epfl/biop/ij2command/Labels2Rois.java
     * Simplified for only one frame, one slice and 1 channel
     * @return RoiManager containing all particle Rois
     */
    public RoiManager label2Roi(){
        ImageProcessor ip = cellposeOutput.getProcessor();
        Wand wand = new Wand(ip);

//        Set RoiManager
        RoiManager rm = RoiManager.getRoiManager();
        rm.reset();

//        Create range list
        int width = cellposeOutput.getWidth();
        int height = cellposeOutput.getHeight();

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
        ip.setColor(0);

        for ( int y_coord : pixel_height) {
            for ( int x_coord : pixel_width) {
                if ( ip.getPixel( x_coord, y_coord ) > 0.0 ){
                    // use the magic wand at this coordinate
                    wand.autoOutline( x_coord, y_coord );

                    // if there is a region , then it has npoints
                    if ( wand.npoints > 0 ) {
                        // get the Polygon, fill with 0 and add to the manager
                        Roi roi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);
                        roi.setPosition( cellposeOutput.getCurrentSlice() );
                        // ip.fill should use roi, otherwise make a rectangle that erases surrounding pixels
                        ip.fill(roi);
                        if (excludeOnEdges){
                            Rectangle r = roi.getBounds();
                            if (r.x ==0||r.y==0||r.x+r.width==ip.getWidth()||r.y+r.height==ip.getHeight()){
                                continue;
                            }
                        }
                        rm.addRoi( roi );
                    }
                }
            }
        }
        rm.runCommand( cellposeOutput , "Show All" );

        return rm;
    }

    public static void main(String[] args) {
        ImagePlus DAPI = IJ.openImage("C:/Users/Camille/Downloads/Camille_Stage2022/Macro 1_Foci_Noyaux/Images/WT_HU_Ac-2re--cell003_w31 DAPI 405.TIF");
        Detector detector = new Detector(DAPI,"Nucleus",new MeasureCalibration());
        detector.setzStackParameters("Maximum projection",0,DAPI.getNSlices());
        Cellpose cellpose = new Cellpose(detector.getImage(),200,"cyto2",true);
        cellpose.analysis();
        detector.getImage().show();
        cellpose.label2Roi().show();
        cellpose.getCellposeOutput().show();

    }
}
