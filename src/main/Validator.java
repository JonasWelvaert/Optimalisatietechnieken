package main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Validator {
private boolean isValid;
    public Validator() {
    	
    }

    public void validate(String inputFileName, String outputFileName) {
        try {
        	int teller = 0;
            // Run a java app in a separate system process
            Process proc;
            proc = Runtime.getRuntime().exec("java -jar AspValidator.jar -i " + inputFileName + " -s " + outputFileName);
            // Then retreive the process output
            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            Scanner scanner = new Scanner(in);
            String line =  scanner.nextLine();
            while (scanner.hasNextLine()) {
            	teller++;
                System.out.println(line);
                if(teller==0) {
                	isValid = line.split(" ")[1].equals("VALID");
                }
            }
            scanner.close();
            scanner = new Scanner(err);
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
            }
            scanner.close();
        } catch (IOException e) {
            System.out.println("Verplaats de jar file naar de correcte map: de root map waarin onderandere src/ en en alle instancefiles zitten.");
        }
    }
    public boolean isValid() {
		return isValid;
	}
    
    
}
