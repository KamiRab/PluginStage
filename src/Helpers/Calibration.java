package Helpers;

import ij.IJ;
import ij.Prefs;

import java.io.*;
import java.util.ArrayList;

import static ij.IJ.d2s;

/**
 * Class to define calibration and parse calibration file
 */
public class Calibration {
    private final String name;
    private final double pixelArea;
    private final String unit;

//    CONSTRUCTORS
    /**
     * Constructor mainly used when parsing file
     * @param name : label to identify calibration
     * @param pixelLength : length of pixel
     * @param unit : unit of calibration
     */
    public Calibration(String name, String pixelLength, String unit) {
//        SET NAME
        this.name = name;
//        SET VALUE
        /*The values are considered as given as length so they need to be converted to areas by multiplicating them*/
        double value_tmp;
        try {
            value_tmp = Double.parseDouble(pixelLength);
            value_tmp *=value_tmp;
        }catch (NumberFormatException e){ /*If the value is not a number*/
            IJ.error("The value is not a number, please correct the file.");
            value_tmp=1;
        }
        this.pixelArea = value_tmp;

//        SET UNIT
        /*convert micro in greek letter */
        if (unit.startsWith("um")){
            unit = "\u03BCm";
        }
        this.unit = unit+"\u00B2";
    }

    /**
     * Constructor for default value of no calibration
     */
    public Calibration(){
//        SET NAME
        this.name = "No calibration";
//        SET VALUE
        this.pixelArea =1;
//        SET UNIT
        this.unit = "pixel";
    }

//      GETTER
    public double getPixelArea() {
        return pixelArea;
    }

    public String getUnit() {
        return unit;
    }

    public String getName() {
        return name;
    }

//      METHODS/FUNCTIONS
    /**
     * Parse IJ_Calibration.txt file found in Prefs directory
     * If no file found, creates one with default value
     * @return ArrayList of all calibrations found in file
     */
    public static ArrayList<Calibration> getCalibrationFromFile(){
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt"; /*get localisation of file*/
        ArrayList<Calibration> calibrations = new ArrayList<>(); /*init ArrayList*/
        try{
            BufferedReader reader = new BufferedReader(new FileReader(calibration_filename));
            String currentLine;
//            Reads each line til no more
            while ((currentLine=reader.readLine())!=null){
                if (!currentLine.startsWith("(")){/*Headings line contains ()*/
                    String[] calibration_values = currentLine.split(";");/*The three infos are separated by ;*/
                    if (calibration_values.length==3){
                        calibrations.add(new Calibration(calibration_values[0],calibration_values[1],calibration_values[2]));
                    } else {/*If more or less infos display error*/
                        IJ.error("The calibration information need to be separated by ';' " +
                                "and there should only be the name, value and unit");
                    }
                }

            }
            reader.close();
        } catch (FileNotFoundException e) { /*If calibration file does not exists, creates one*/
            try {
                createCalibrationFile();
                return getCalibrationFromFile(); /* With file existing, it can be parsed */
            } catch (IOException ex) { /*If the file can not be created, display error message*/
                IJ.error(calibration_filename+" could not be found and could not be created." +
                        "Please verify rights of access or create it yourself."); /*TODO expert option for other directory ?*/
                ex.printStackTrace();
            }
        } catch (IOException e) { /*If the file can not be read, display error message*/
            IJ.error("Could not read the file, please verify rights of access");
            e.printStackTrace();
        }
        return calibrations;
    }

    /**
     * From a created Helpers.Calibration instance, adds it to file
     */
    public void addCalibrationToFile(){
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt";
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(calibration_filename,true));
            output.newLine();
            output.append(name).append(";").append(String.valueOf(pixelArea)).append(";").append(unit);
            IJ.log("Added new calibration : name: "+ name +" value: " + pixelArea +" unit: "+ unit);
            output.close();
        } catch (IOException e) { /*If file can not be written in, display error message*/
            IJ.error("Could not add new calibration to the file, please verify rights of access \n" + e.getMessage());
        }
    }

    /**
     * Convert getCalibrationFromFile ArrayLis to array
     * @return array of Calibrations contained in Helpers.Calibration file
     */
    public static Calibration[] getCalibrationArrayFromFile(){
        ArrayList<Calibration> calibrationsArrayList = getCalibrationFromFile();
        Calibration[] calibrationsArray = new Calibration[calibrationsArrayList.size()];
        for (int i = 0; i < calibrationsArrayList.size(); i++) {
            calibrationsArray[i]=calibrationsArrayList.get(i);
        }
        return calibrationsArray;
    }

    /**
     * Creates Helpers.Calibration file in ImageJ prefs directory
     * @throws IOException : if file can not be created or written to, getCalibrationFromFile display error message
     */
    private static void createCalibrationFile() throws IOException {
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt";
        File calibration_file = new File(calibration_filename);
        if (calibration_file.createNewFile()){
            IJ.log("Creation of calibration file in "+ Prefs.getPrefsDir());
        }else {
            IJ.log("File already exists, but file was not found previously.");
        }
        FileWriter fileWriter = new FileWriter(calibration_file.getAbsoluteFile());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("(Name;Value;Measurement unit)\n" + /*Write headings*/
                "No calibration;1;pix");/*Write default calibration*/
        bufferedWriter.close();
    }

    /**
     * From name of calibration, found the Helpers.Calibration object corresponding
     * Used for the JComboBox
     * @param calibrations : array of Calibrations objects
     * @param nameToFind : name of calibration to find
     * @return Helpers.Calibration corresponding to name
     */
    public static Calibration findCalibrationFromName(Calibration[] calibrations, String nameToFind){
        Calibration toReturn = null;
        for (Calibration c: calibrations
             ) {
            if(nameToFind.equals(c.getName())&& toReturn==null){
                toReturn=c;
            } else if (nameToFind.equals(c.getName())&&toReturn!=null){
                IJ.error("Multiple calibration with same name");
            }
        }
        if (toReturn == null){
            IJ.error("No calibration was found");
        }
        return toReturn;
    }


    @Override
    public String toString() {
        return name+"(x"+ d2s(pixelArea,4) +","+ unit+")";
    }

    /**
     * Used for tests
     * @param args : arguments
     */
    public static void main(String[] args) {
        ArrayList<Calibration> calibrations = getCalibrationFromFile();
        for (Calibration c: calibrations
             ) {
            IJ.log(c.toString());
        }
    }
}
