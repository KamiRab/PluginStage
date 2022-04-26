package GUI;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import ij.measure.Measurements;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;

public class Measures extends JFrame {
    private JCheckBox std_devCheckBox;
    private JCheckBox feretCheckBox;
    private JCheckBox areaCheckBox;
    private JCheckBox integrated_densityCheckBox;
    private JCheckBox meanCheckBox;
    private JCheckBox minMaxCheckBox;
    private JCheckBox skewnessCheckBox;
    private JCheckBox area_fractionCheckBox;
    private JCheckBox center_of_massCheckBox;
    private JCheckBox centroidCheckBox;
    private JCheckBox ellipseCheckBox;
    private JCheckBox kurtosisCheckBox;
    private JCheckBox perimeterCheckBox;
    private JCheckBox modeCheckBox;
    private JCheckBox shape_descriptorsCheckBox;
    private JCheckBox rectCheckBox;
    private JCheckBox medianCheckBox;
    private JButton validateButton;
    private JButton cancelButton;
    private JPanel measuresPanel;
    private ArrayList<String> measurements_names = new ArrayList<>();
    //    private String[] measurements_names;
    private int measurements_value = 0;

    public Measures() {
        validateButton.addActionListener(e -> {
            if (areaCheckBox.isSelected()) {
                measurements_value += Measurements.AREA;
                measurements_names.add("Area");
            }
            if (std_devCheckBox.isSelected()) {
                measurements_value += Measurements.STD_DEV;
                measurements_names.add("StdDev");
            }
            if (minMaxCheckBox.isSelected()) {
                measurements_value += Measurements.MIN_MAX;
                measurements_names.add("Min");
                measurements_names.add("Max");
            }
            if (center_of_massCheckBox.isSelected()) {
                measurements_value += Measurements.CENTER_OF_MASS;
                measurements_names.add("XM");
                measurements_names.add("YM");
            }
            if (rectCheckBox.isSelected()) {
                measurements_value += Measurements.RECT;
                measurements_names.add("BX");
                measurements_names.add("BY");
                measurements_names.add("Width");
                measurements_names.add("Height");
            }
            if (shape_descriptorsCheckBox.isSelected()) {
                measurements_value += Measurements.SHAPE_DESCRIPTORS;
                measurements_names.add("Circ");
                measurements_names.add("AR");
                measurements_names.add("Round");
                measurements_names.add("Solidity");
            }
            if (integrated_densityCheckBox.isSelected()) {
                measurements_value += Measurements.INTEGRATED_DENSITY;
//                    measurements_names.add("IntDen");
                measurements_names.add("RawIntDen");
            }
            if (skewnessCheckBox.isSelected()) {
                measurements_value += Measurements.SKEWNESS;
                measurements_names.add("Skew");
            }
            if (area_fractionCheckBox.isSelected()) {
                measurements_value += Measurements.AREA_FRACTION;
                measurements_names.add("%Area");
            }
            if (meanCheckBox.isSelected()) {
                measurements_value += Measurements.MEAN;
                measurements_names.add("Mean");
            }
            if (modeCheckBox.isSelected()) {
                measurements_value += Measurements.MODE;
                measurements_names.add("Mode");
            }
            if (centroidCheckBox.isSelected()) {
                measurements_value += Measurements.CENTROID;
                measurements_names.add("X");
                measurements_names.add("Y");
            }
            if (perimeterCheckBox.isSelected()) {
                measurements_value += Measurements.PERIMETER;
                measurements_names.add("Perim.");
            }
            if (ellipseCheckBox.isSelected()) {/*Fit Ellipse*/
                measurements_value += Measurements.ELLIPSE;
                measurements_names.add("Major");
                measurements_names.add("Minor");
                measurements_names.add("Angle");
            }
            if (feretCheckBox.isSelected()) {
                measurements_value += Measurements.FERET;
                measurements_names.add("Feret");
                measurements_names.add("FeretX");
                measurements_names.add("FeretY");
                measurements_names.add("FeretAngle");
                measurements_names.add("MinFeret");
            }
            if (medianCheckBox.isSelected()) {
                measurements_value += Measurements.MEDIAN;
                measurements_names.add("Median");
            }
            if (kurtosisCheckBox.isSelected()) {
                measurements_value += Measurements.KURTOSIS;
                measurements_names.add("Kurt");
            }
        });
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
        measuresPanel = new JPanel();
        measuresPanel.setLayout(new GridLayoutManager(10, 2, new Insets(0, 0, 0, 0), -1, -1));
        measuresPanel.setBorder(BorderFactory.createTitledBorder(null, "Measures", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        std_devCheckBox = new JCheckBox();
        std_devCheckBox.setText("Standard deviation");
        measuresPanel.add(std_devCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minMaxCheckBox = new JCheckBox();
        minMaxCheckBox.setText("Min and max gray value");
        measuresPanel.add(minMaxCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        rectCheckBox = new JCheckBox();
        rectCheckBox.setText("Bounding rectangle");
        measuresPanel.add(rectCheckBox, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        center_of_massCheckBox = new JCheckBox();
        center_of_massCheckBox.setText("Center of mass");
        measuresPanel.add(center_of_massCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        shape_descriptorsCheckBox = new JCheckBox();
        shape_descriptorsCheckBox.setText("Shape descriptors");
        measuresPanel.add(shape_descriptorsCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        area_fractionCheckBox = new JCheckBox();
        area_fractionCheckBox.setText("Area fraction");
        measuresPanel.add(area_fractionCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        skewnessCheckBox = new JCheckBox();
        skewnessCheckBox.setText("Skewness");
        measuresPanel.add(skewnessCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        modeCheckBox = new JCheckBox();
        modeCheckBox.setText("Modal gray value");
        measuresPanel.add(modeCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ellipseCheckBox = new JCheckBox();
        ellipseCheckBox.setText("Fit ellipse");
        measuresPanel.add(ellipseCheckBox, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        kurtosisCheckBox = new JCheckBox();
        kurtosisCheckBox.setText("Kurtosis");
        measuresPanel.add(kurtosisCheckBox, new GridConstraints(7, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        centroidCheckBox = new JCheckBox();
        centroidCheckBox.setText("Centroid");
        measuresPanel.add(centroidCheckBox, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        perimeterCheckBox = new JCheckBox();
        perimeterCheckBox.setText("Perimeter");
        measuresPanel.add(perimeterCheckBox, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        feretCheckBox = new JCheckBox();
        feretCheckBox.setText("Feret's diameter");
        measuresPanel.add(feretCheckBox, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        areaCheckBox = new JCheckBox();
        areaCheckBox.setEnabled(false);
        areaCheckBox.setSelected(true);
        areaCheckBox.setText("Area");
        measuresPanel.add(areaCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        integrated_densityCheckBox = new JCheckBox();
        integrated_densityCheckBox.setEnabled(false);
        integrated_densityCheckBox.setSelected(true);
        integrated_densityCheckBox.setText("Integrated density");
        measuresPanel.add(integrated_densityCheckBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        meanCheckBox = new JCheckBox();
        meanCheckBox.setEnabled(false);
        meanCheckBox.setSelected(true);
        meanCheckBox.setText("Mean gray value");
        measuresPanel.add(meanCheckBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        medianCheckBox = new JCheckBox();
        medianCheckBox.setText("Median");
        measuresPanel.add(medianCheckBox, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        validateButton = new JButton();
        validateButton.setText("Validate");
        measuresPanel.add(validateButton, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        measuresPanel.add(cancelButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return measuresPanel;
    }
}
