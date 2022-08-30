package gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import ij.Prefs;
import ij.measure.Measurements;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
/**
 * Author : Camille RABIER
 * Date : 18/04/2022
 * GUI Class for
 * - defining measures to use for cells or nuclei
 */
public class Measures extends JFrame {
    private final PluginCellProt.MeasureValue measurementsValueClass;
    private JCheckBox stdDevCheckBoxSelected;
    private JCheckBox feretCheckBox;
    private JCheckBox areaCheckBox;
    private JCheckBox integratedDensityCheckBoxSelected;
    private JCheckBox meanCheckBox;
    private JCheckBox minMaxCheckBox;
    private JCheckBox skewnessCheckBox;
    private JCheckBox areaFractionCheckBoxSelected;
    private JCheckBox centerOfMassCheckBoxSelected;
    private JCheckBox centroidCheckBox;
    private JCheckBox ellipseCheckBox;
    private JCheckBox kurtosisCheckBox;
    private JCheckBox perimeterCheckBox;
    private JCheckBox modeCheckBox;
    private JCheckBox shapeDescriptorsCheckBoxSelected;
    private JCheckBox rectCheckBox;
    private JCheckBox medianCheckBox;
    private JButton validateButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    private Integer measurementsValue;

    public Measures(String type, PluginCellProt.MeasureValue measures) {
        getPrefs(type);
        this.measurementsValueClass = measures;
        validateButton.addActionListener(e -> {
//            GET MEASUREMENTS VALUE
            measurementsValue = Measurements.MEAN + Measurements.INTEGRATED_DENSITY + Measurements.AREA;
            if (stdDevCheckBoxSelected.isSelected()) {
                measurementsValue += Measurements.STD_DEV;
            }
            if (minMaxCheckBox.isSelected()) {
                measurementsValue += Measurements.MIN_MAX;
            }
            if (centerOfMassCheckBoxSelected.isSelected()) {
                measurementsValue += Measurements.CENTER_OF_MASS;
            }
            if (rectCheckBox.isSelected()) {
                measurementsValue += Measurements.RECT;
            }
            if (shapeDescriptorsCheckBoxSelected.isSelected()) {
                measurementsValue += Measurements.SHAPE_DESCRIPTORS;
            }
            if (skewnessCheckBox.isSelected()) {
                measurementsValue += Measurements.SKEWNESS;
            }
            if (areaFractionCheckBoxSelected.isSelected()) {
                measurementsValue += Measurements.AREA_FRACTION;
            }
            if (modeCheckBox.isSelected()) {
                measurementsValue += Measurements.MODE;
            }
            if (centroidCheckBox.isSelected()) {
                measurementsValue += Measurements.CENTROID;
            }
            if (perimeterCheckBox.isSelected()) {
                measurementsValue += Measurements.PERIMETER;
            }
            if (ellipseCheckBox.isSelected()) {/*Fit Ellipse*/
                measurementsValue += Measurements.ELLIPSE;
            }
            if (feretCheckBox.isSelected()) {
                measurementsValue += Measurements.FERET;
            }
            if (medianCheckBox.isSelected()) {
                measurementsValue += Measurements.MEDIAN;
            }
            if (kurtosisCheckBox.isSelected()) {
                measurementsValue += Measurements.KURTOSIS;
            }
            Prefs.set("MICMAQ.Measurements_" + type, measurementsValue);
            measurementsValueClass.setMeasure(measurementsValue);
            Prefs.savePreferences();
            setVisible(false);
        });
        cancelButton.addActionListener(e -> dispose());
    }

//    PREFS

    /**
     * Preset the checkbox according to prefs
     * @param type : Cell or Nuclei
     */
    private void getPrefs(String type) {
        int measurements = (int) Prefs.get("MICMAQ.Measurements_" + type, (Measurements.AREA + Measurements.MEAN + Measurements.INTEGRATED_DENSITY));
        stdDevCheckBoxSelected.setSelected((measurements & Measurements.STD_DEV) != 0);
        minMaxCheckBox.setSelected((measurements & Measurements.MIN_MAX) != 0);
        centerOfMassCheckBoxSelected.setSelected((measurements & Measurements.CENTER_OF_MASS) != 0);
        rectCheckBox.setSelected((measurements & Measurements.RECT) != 0);
        shapeDescriptorsCheckBoxSelected.setSelected((measurements & Measurements.SHAPE_DESCRIPTORS) != 0);
        skewnessCheckBox.setSelected((measurements & Measurements.SKEWNESS) != 0);
        areaFractionCheckBoxSelected.setSelected((measurements & Measurements.AREA_FRACTION) != 0);
        centroidCheckBox.setSelected((measurements & Measurements.CENTROID) != 0);
        perimeterCheckBox.setSelected((measurements & Measurements.PERIMETER) != 0);
        ellipseCheckBox.setSelected((measurements & Measurements.ELLIPSE) != 0);
        feretCheckBox.setSelected((measurements & Measurements.FERET) != 0);
        medianCheckBox.setSelected((measurements & Measurements.MEDIAN) != 0);
        kurtosisCheckBox.setSelected((measurements & Measurements.KURTOSIS) != 0);
    }


    public void run() {
        setTitle("Choose measurements");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/IconPlugin.png")));
        setContentPane(this.mainPanel);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        pack();
        setVisible(true);
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(10, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setBorder(BorderFactory.createTitledBorder(null, "Measures", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        stdDevCheckBoxSelected = new JCheckBox();
        stdDevCheckBoxSelected.setText("Standard deviation");
        mainPanel.add(stdDevCheckBoxSelected, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minMaxCheckBox = new JCheckBox();
        minMaxCheckBox.setText("Min and max gray value");
        mainPanel.add(minMaxCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rectCheckBox = new JCheckBox();
        rectCheckBox.setText("Bounding rectangle");
        mainPanel.add(rectCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        centerOfMassCheckBoxSelected = new JCheckBox();
        centerOfMassCheckBoxSelected.setText("Center of mass");
        mainPanel.add(centerOfMassCheckBoxSelected, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shapeDescriptorsCheckBoxSelected = new JCheckBox();
        shapeDescriptorsCheckBoxSelected.setText("Shape descriptors");
        mainPanel.add(shapeDescriptorsCheckBoxSelected, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        areaFractionCheckBoxSelected = new JCheckBox();
        areaFractionCheckBoxSelected.setText("Area fraction");
        mainPanel.add(areaFractionCheckBoxSelected, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        skewnessCheckBox = new JCheckBox();
        skewnessCheckBox.setText("Skewness");
        mainPanel.add(skewnessCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeCheckBox = new JCheckBox();
        modeCheckBox.setText("Modal gray value");
        mainPanel.add(modeCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ellipseCheckBox = new JCheckBox();
        ellipseCheckBox.setText("Fit ellipse");
        mainPanel.add(ellipseCheckBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kurtosisCheckBox = new JCheckBox();
        kurtosisCheckBox.setText("Kurtosis");
        mainPanel.add(kurtosisCheckBox, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        centroidCheckBox = new JCheckBox();
        centroidCheckBox.setText("Centroid");
        mainPanel.add(centroidCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        perimeterCheckBox = new JCheckBox();
        perimeterCheckBox.setText("Perimeter");
        mainPanel.add(perimeterCheckBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        feretCheckBox = new JCheckBox();
        feretCheckBox.setText("Feret's diameter");
        mainPanel.add(feretCheckBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        areaCheckBox = new JCheckBox();
        areaCheckBox.setEnabled(false);
        areaCheckBox.setSelected(true);
        areaCheckBox.setText("Area");
        mainPanel.add(areaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        integratedDensityCheckBoxSelected = new JCheckBox();
        integratedDensityCheckBoxSelected.setEnabled(false);
        integratedDensityCheckBoxSelected.setSelected(true);
        integratedDensityCheckBoxSelected.setText("Integrated density");
        mainPanel.add(integratedDensityCheckBoxSelected, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        meanCheckBox = new JCheckBox();
        meanCheckBox.setEnabled(false);
        meanCheckBox.setSelected(true);
        meanCheckBox.setText("Mean gray value");
        mainPanel.add(meanCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        medianCheckBox = new JCheckBox();
        medianCheckBox.setText("Median");
        mainPanel.add(medianCheckBox, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validateButton = new JButton();
        validateButton.setText("Validate");
        mainPanel.add(validateButton, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        mainPanel.add(cancelButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
