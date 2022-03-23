import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class Detect_Nuclei extends JPanel {
    private JCheckBox isAZStackCheckBox;
    private JComboBox zProjMethods;
    private JLabel zProjMethodsLabel;
    private JComboBox threshMethods;
    private JSpinner minSizeNucleus;
    private JLabel minSizeNucleus_label;
    private JLabel threshMethods_label;
    private JPanel zProj;
    private JCheckBox useWatershedCheckBox;
    private JCheckBox previewCheckBox;
    private JPanel main;
    private JPanel parameters;
    private JComboBox comboBox1;

    public Detect_Nuclei() {
        zProjMethods.setVisible(false);
        zProjMethodsLabel.setVisible(false);
        /*Are the images stacks ?*/

        isAZStackCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange()==ItemEvent.SELECTED){
                    zProjMethods.setVisible(true);
                    zProjMethodsLabel.setVisible(true);
                } else {
                    zProjMethods.setVisible(false);
                    zProjMethodsLabel.setVisible(false);
                }
            }
        });
    }

    public JPanel getMain() {
        return main;
    }
}
