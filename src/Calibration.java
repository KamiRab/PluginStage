import ij.IJ;
import ij.Prefs;

import java.io.*;
import java.util.ArrayList;

import static ij.IJ.d2s;

public class Calibration {
    private final String name;
    private final double value;
    private final String unit;

    public Calibration(String name, String value, String unit) {
        this.name = name;
        double value_tmp;
        if (value.contains("*")){
            String[] multiplication = value.split("\\*");
            value_tmp=1.0;
            for (String multiplicator: multiplication) {
                try {
                    value_tmp*=Double.parseDouble(multiplicator);
                }catch (NumberFormatException e){
                    IJ.error("The value should be a number, please correct the file.");
                    value_tmp=1;
                }
            }
        } else {
            try {
                value_tmp=Double.parseDouble(value);
            }catch (NumberFormatException e){
                IJ.error("The value is not a number, please correct the file.");
                value_tmp=1;
            }
        }
        this.value =value_tmp;
        if (unit.endsWith("2")){
            unit = unit.replace("2","²");
        }
        if (unit.startsWith("um")){
            unit=unit.replace("u","µ");
        }
        this.unit = unit;
    }

    public Calibration(String name, double value, String unit){
        this.name = name;
        this.value=value;
        if (unit.endsWith("2")){
            unit = unit.replace("2","²");
        }
        if (unit.startsWith("um")){
            unit=unit.replace("u","µ");
        }
        this.unit = unit;
    }

    public static ArrayList<Calibration> getCalibrationFromFile(){
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt";
        ArrayList<Calibration> calibrations = new ArrayList<>();
        try{
            BufferedReader reader = new BufferedReader(new FileReader(calibration_filename));
            String currentLine;
            while ((currentLine=reader.readLine())!=null){
                if (!currentLine.startsWith("(")){
                    String[] calibration_values = currentLine.split(";");
                    if (calibration_values.length==3){
                        calibrations.add(new Calibration(calibration_values[0],calibration_values[1],calibration_values[2]));
                    } else {
                        IJ.error("The calibration information need to be separated by ';' " +
                                "and there should only be the name, value and unit");
                    }
                }

            }
            reader.close();
        } catch (FileNotFoundException e) {
            try {
                createCalibrationFile();
                return getCalibrationFromFile();
            } catch (IOException ex) {
                IJ.error(calibration_filename+" could not be found and could not be created." +
                        "Please verify rights of access or create it yourself."); /*TODO expert option for other directory ?*/
                ex.printStackTrace();
            }
        } catch (IOException e) {
            IJ.error("Could not read the file, please verify rights of access");
            e.printStackTrace();
        }
        return calibrations;
    }
    public static void addCalibrationToFile(String name,String value, String unit){
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt";
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(calibration_filename,true));
            output.newLine();
            output.append(name).append(";").append(value).append(";").append(unit);
            IJ.log("Added new calibration : name: "+ name +" value: " + value +" unit: "+ unit);
            output.close();
        } catch (IOException e) {
            IJ.error("Could not add new calibration to the file, please verify rights of access \n" + e.getMessage());
        }
    }

    public static Calibration[] getCalibrationArrayFromFile(){
        ArrayList<Calibration> calibrationsArrayList = getCalibrationFromFile();
        Calibration[] calibrationsArray = new Calibration[calibrationsArrayList.size()];
        for (int i = 0; i < calibrationsArrayList.size(); i++) {
            calibrationsArray[i]=calibrationsArrayList.get(i);
        }
        return calibrationsArray;
    }

    private static void createCalibrationFile() throws IOException {
        String calibration_filename = Prefs.getPrefsDir()+"\\IJ_Calibration.txt";
        File calibration_file = new File(calibration_filename);
        if (calibration_file.createNewFile()){
            IJ.log("Creation of calibration file in "+ Prefs.getPrefsDir());
        }else {
            IJ.log("A problem with file finding happened.");
        }
        FileWriter fileWriter = new FileWriter(calibration_file.getAbsoluteFile());
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
        bufferedWriter.write("(Name;Value;Measurement unit)\n" +
                "No calibration;1;pix");
        bufferedWriter.close();
    }

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

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name+"(x"+ d2s(value,4) +","+ unit+")";
    }

    public String getUnit() {
        return unit;
    }

    public String getName() {
        return name;
    }

    public static void main(String[] args) {
        ArrayList<Calibration> calibrations = getCalibrationFromFile();
        for (Calibration c: calibrations
             ) {
            IJ.log(c.toString());
        }
    }
}
