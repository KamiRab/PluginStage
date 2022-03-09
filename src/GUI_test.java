import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI_test implements PlugIn {
    private JPanel general;
    private JButton okButton;
    private JRadioButton radioButton1;
    private JRadioButton radioButton2;
    private JTextField textField1;
    private JButton button1;
    private JFrame frame;
    static JFrame instance; /*only one copy of the plugin */

    public GUI_test() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }

    @Override
    public void run(String s) {
        if (instance != null){
            instance.toFront();
        } else {
            frame=new JFrame("Nom Plugin");
            instance=frame;
            frame.setSize(600,250);
        }
    }

    public JPanel getPanel(){
        return general;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
