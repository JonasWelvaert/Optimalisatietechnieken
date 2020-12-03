package main;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

public class Validator {

    public Validator() {
    }

    public void validate(String inputFileName, String outputFileName) {
        try {
            // Run a java app in a separate system process
            Process proc;
            proc = Runtime.getRuntime().exec("java -jar AspValidator.jar -i " + inputFileName + " -s " + outputFileName);
            // Then retreive the process output
            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            Scanner scanner = new Scanner(in);
            while (scanner.hasNextLine()) {
                System.out.println(scanner.nextLine());
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
}
