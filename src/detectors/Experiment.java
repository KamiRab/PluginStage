package detectors;

import helpers.MeasureCalibration;
import ij.IJ;
import ij.gui.Roi;
import ij.measure.ResultsTable;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
/**
 * Author : Camille RABIER
 * Date : 31/03/2022
 * Class for
 * - associating different channels of a set of experience
 */
public class Experiment {
    private final NucleiDetector nuclei;
    private final CellDetector cell;
    private final ArrayList<SpotDetector> spots;
    private final String experimentName;
    private final ResultsTable finalResultsNuclei;
    private final ResultsTable finalResultsCellSpot;

    private boolean interrupt = false;

    private final MeasureCalibration measureCalibration;

    /**
     * Constructor
     * @param nucleiDetector : {@link NucleiDetector}
     * @param cellDetector : {@link CellDetector}
     * @param spotDetectors :{@link ArrayList} of {@link SpotDetector}
     * @param finalResultsCellSpot : ResultsTable corresponding to measure in cell/cytoplasm or nuclei if no cell
     * @param finalResultsNuclei : ResultsTable corresponding to measure in nuclei if cell
     * @param measureCalibration : calibration to use for measures
     */
    public Experiment(NucleiDetector nucleiDetector, CellDetector cellDetector, ArrayList<SpotDetector> spotDetectors, ResultsTable finalResultsCellSpot, ResultsTable finalResultsNuclei, MeasureCalibration measureCalibration) {
        this.nuclei = nucleiDetector;
        this.cell = cellDetector;
        this.spots = spotDetectors;
        if (nucleiDetector!=null) this.experimentName = nucleiDetector.getNameExperiment();
        else if (cellDetector!=null) this.experimentName = cellDetector.getNameExperiment();
        else if (spotDetectors.size()>0) this.experimentName = spotDetectors.get(0).getNameExperiment();
        else this.experimentName = null;
        this.finalResultsNuclei = finalResultsNuclei;
        this.finalResultsCellSpot = finalResultsCellSpot;
        this.measureCalibration=measureCalibration;
    }

    /**
     *
     * @return name without channel specific information
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Do segmentation and detection for channel concerned and aggregates the measurements done in ResultTable(s)
     * @return true if everything worked
     */
    public boolean run() {
        Instant dateBegin = Instant.now(); /*get starting time*/
//        PREPARE NECESSARY IMAGES FOR MEASUREMENTS
//        --> Cells images
        if (cell !=null && !interrupt){
            IJ.log("Cell/Cytoplasm image: "+ cell.getImageTitle());
            cell.setMeasureCalibration(measureCalibration);
            if (!cell.prepare()) {
                interrupt=true;
                return false;
            }
        }
//        --> Nuclei images
        if (nuclei!=null && !interrupt){
            IJ.log("Nuclei image: "+nuclei.getImageTitle());
            nuclei.setMeasureCalibration(measureCalibration);
            if (!nuclei.prepare()) {
                interrupt=true;
                return false;
            }
        }
//        --> Spots images
        for (int i = 0; i < spots.size(); i++) {
            if (!interrupt){
                IJ.log("Spot channel "+(i+1)+ " image:" + spots.get(i).getImageTitle());
                SpotDetector spot = spots.get(i);
                spot.setMeasureCalibration(measureCalibration);
                if (!spot.prepare()) {
                    interrupt=true;
                    return false;
                }
            }
        }
//        Prepare cytoplasm and get the ROIs for spot detection
        Roi[] cellRois = null;
        Roi[] nucleiRois = null;
        Roi[] cytoplasmRois = null;
        boolean onlySpot = false;
        int numberOfObject;
        CytoDetector cytoDetector = null;
        if (cell!=null && !interrupt){
            cellRois = cell.getRoiArray();
            if (nuclei!=null){ /*Prepare cytoplasm et get new cellRois*/
                cytoDetector = cell.getCytoDetector();
                cytoDetector.setNucleiRois(nuclei.getRoiArray());
                cytoDetector.setMeasureCalibration(measureCalibration);
                cytoDetector.prepare();
                nucleiRois = cytoDetector.getAssociatedNucleiRois();
                cytoplasmRois = cytoDetector.getCytoRois();
                cellRois = cytoDetector.getCellRois();
                cell.setCellRois(cellRois);
            }
            numberOfObject = cellRois.length;
        }else { /*No cell images*/
            if (nuclei!=null && !interrupt){ /*But nuclei images*/
                nucleiRois = nuclei.getRoiArray();
                numberOfObject = nucleiRois.length;
            } else { /*Only spot images*/
                numberOfObject = 1;
                onlySpot = true;
            }
        }
        if (cell!=null && nuclei!=null && !interrupt){ /*Get result table for all nuclei in addition to cell ResultTable*/
            Roi[] allNuclei = nuclei.getRoiArray();
            int[] association2Cell = cytoDetector.getAssociationCell2Nuclei();
            for (int nucleusID = 0; nucleusID < allNuclei.length; nucleusID++) {
                finalResultsNuclei.addValue("Name experiment", experimentName);
                finalResultsNuclei.addValue("Nucleus nr.", "" + (nucleusID + 1));
                finalResultsNuclei.addValue("Cell associated",""+association2Cell[nucleusID]);
                nuclei.measureEachNuclei(nucleusID, finalResultsNuclei,allNuclei[nucleusID]);
                for (SpotDetector spot : spots) {
                    spot.analysisPerRegion(nucleusID,allNuclei[nucleusID], finalResultsNuclei,"Nuclei");
                }
                finalResultsNuclei.incrementCounter();
            }
            nuclei.setNucleiAssociatedRois(nucleiRois);
        }
//        Measurements for each cell or nuclei or image (in case of only spot)
        for (int cellID = 0; cellID < numberOfObject; cellID++) {
            if (!interrupt){
                finalResultsCellSpot.addValue("Name experiment", experimentName);
                finalResultsCellSpot.addValue("Cell nr.", "" + (cellID + 1));
                if (cell!=null){
                    cell.measureEachCell(cellID, finalResultsCellSpot);
                }
                if (nuclei!=null && cell==null){
                    nuclei.measureEachNuclei(cellID, finalResultsCellSpot,nucleiRois[cellID]);
                }
                if (cytoDetector!=null){
                    cytoDetector.measureEachCytoplasm(cellID, finalResultsCellSpot);
                }
                for (SpotDetector spot : spots) {
                    if (cellRois!=null)spot.analysisPerRegion(cellID,cellRois[cellID], finalResultsCellSpot,"Cell");
                    if (nucleiRois!=null && cellRois==null)spot.analysisPerRegion(cellID,nucleiRois[cellID], finalResultsCellSpot,"Nuclei");
                    if (cytoplasmRois!=null)spot.analysisPerRegion(cellID, cytoplasmRois[cellID], finalResultsCellSpot,"Cytoplasm");
                    if (onlySpot) spot.analysisPerRegion(cellID,null, finalResultsCellSpot,"Image");
                }
                finalResultsCellSpot.incrementCounter();
            }
        }
//        TIMING OF EXPERIENCE
        Instant dateEnd = Instant.now();
        long duration = Duration.between(dateBegin,dateEnd).toMillis();
        IJ.log("Experiment "+experimentName+" is done in :" +duration/1000+" seconds");
        return true;
    }

    /**
     * interrupt process if error or user clicked on cancel button
     */
    public void interruptProcess() {
        interrupt = true;
    }
}
