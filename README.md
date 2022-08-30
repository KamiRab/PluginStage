# PluginStage
!! Tutorial is in progress


Plugin MIC-MAQ for segmenting nuclei, cell and cytoplasm in images and then measuring protein expression in those segmented object.
The source code needs to be compiled with IJ.jar corresponding to ImageJ/Fiji functions and ijl-utilities wrapper (at this day 0.60) that corresponds to the biop plugin
To use Cellpose, cellpose (command line) and biop (Fiji plugin) needs to be installed 
The jar resulting is usable in ImageJ/Fiji when placed in the plugin folder. After restart, the plugin will be in the plugin tab with the name MIC-MAQ

The gui package corresponds to the graphical classes
The detectors package contains all classes related to image manipulation (preprocessing, segmentation, detection, saving)
The helper package contains a class to improve image manipulation in the plugin (ImageToAnalyze class) and the Calibration class
