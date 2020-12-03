package main;

import model.*;
import model.machinestate.Idle;
import model.machinestate.Maintenance;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import static graphing.OptimalisationGraphing.*;

public class Main {
    private static final InputFile inputFileName = InputFile.D40_R100_B60;
    private static final String outputPrefix = "SA2";
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static final Validator validator = new Validator();
    public static double COST_OF_OVERTIME;
    public static double COST_OF_NIGHT_SHIFT;
    public static double COST_OF_PARALLEL_TASK;
    public static double COST_PER_ITEM_UNDER_MINIMUM_LEVEL;

    public static List<String> graphingOutput = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        //logger.setLevel(Level.OFF);

        // 1. inputfile
        logger.info("| Starting reading of input file " + inputFileName);
        Planning initialPlanning = readFileIn("instances/" + inputFileName.toString());


        // 2. initial solution
        logger.info("| Starting making first feasible solution");
        initialPlanning = makeInitialPlanning(initialPlanning);
        logger.log(Level.INFO, "--------------- TOTAL COST = " + initialPlanning.getTotalCost());

        if (!Solver.checkFeasible(initialPlanning)) {
            logger.severe("2. Initial planning is not feasible!");
            System.exit(2);
        }

        // 3. optimalisation
        logger.info("| Starting optimalisation");
        Solver solver = new Solver(Solver.SIMULATED_ANEALING);
        solver.setSimulatedAnealingFactors(10000, 0.995);

        Planning optimizedPlanning = solver.optimize(initialPlanning);
        if (!Solver.checkFeasible(optimizedPlanning)) {
            logger.severe("5. optimalized planning is not feasible!");
            System.exit(5);
        }

        // 4. output
        logger.info("| Starting writing outputfile");
        printOutputToConsole(optimizedPlanning);
        File file = new File(outputPrefix);
        file.mkdir();
        printOutputToFile(outputPrefix + "/" + outputPrefix + "_" + inputFileName, optimizedPlanning);

        validator.validate("instances/" + inputFileName.toString(), outputPrefix + "/" + outputPrefix + "_" + inputFileName);

        //5. print out csv
        printCSV(optimizedPlanning);

        // 6. The end
        logger.info("Total cost: " + optimizedPlanning.getTotalCost());
        logger.info("| Finished execution");
        optimizedPlanning.logAllCosts();
    }

    private static void printCSV(Planning optimizedPlanning) throws IOException {
        FileWriter fw = new FileWriter(csvFile);
        PrintWriter out = new PrintWriter(fw);

        for (String s : graphingOutput) {
            out.println(s);
        }

        out.flush();
        out.close();
        fw.close();
    }

    /**
     * Makes all machines IDLE and no requests fullfilled except for the maintenance
     * and nightshifts to ensure a feasible planning.
     *
     * @param planning The planning returned by the main.Main.readFileIn method
     * @return A initial feasible planning
     */
    private static Planning makeInitialPlanning(Planning planning) throws IOException {
        for (Day d : planning.getDays()) {
            // putting all machines all the time in idle.
            for (Machine m : planning.getMachines()) {
                for (Block b : d.getBlocks()) {
                    b.setMachineState(m, new Idle());
                }
            }
            // then stock will always be the initial stock.
            for (Item i : planning.getStock().getItems()) {
                i.setStockAmount(d, i.getInitialQuantityInStock());
            }
        }

        // now we need to make sure all nightshifts are fullfilled.
        if (planning.getPastConsecutiveDaysWithNightShift() != 0) {
            for (int i = 0; i < Planning.getMinConsecutiveDaysWithNightShift()
                    - planning.getPastConsecutiveDaysWithNightShift(); i++) {
                planning.getDay(i).setNightShift(true);
            }
        }

        // now we need to make sure all needed maintenances are planned
        for (Machine m : planning.getMachines()) {
            int init = m.getInitialDaysPastWithoutMaintenance();
            int max = m.getMaxDaysWithoutMaintenance();
            for (int i = 0; i < Planning.getNumberOfDays(); i++) {
                if (Math.floorMod(i, max + 1) == max + 1 - (init + 1)) { //Romeo: die floorMod() snap ik niet goed
                    // Jonas: ik ga ervanuit dat in een IDLE planning, de beide machines kunnen
                    // onderhouden worden.
                    int teller = 0;
                    for (int j = Day.indexOfBlockE; j <= Day.indexOfBlockL; j++) {
                        if (!planning.getDay(i).getBlock(j).isInMaintenance()) {
                            planning.getDay(i).getBlock(j).setMachineState(m, new Maintenance());
                            teller++;
                            if (teller == m.getMaintenanceDurationInBlocks()) {
                                break;
                            }
                        }
                    }
                }
            }
        }
        planning.calculateAllCosts();
        return planning;
    }

    /**
     * This function writes the wished output of a planning to a file
     *
     * @param filename The filename where to write the output
     * @param planning The planning which has to be written to the console.
     */
    private static void printOutputToFile(String filename, Planning planning) {
        try {
            File file = new File(filename);
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write("Instance_name: " + planning.getInstanceName() + System.lineSeparator());
            bw.write("Cost: " + String.format("%.2f", planning.getTotalCost()) + System.lineSeparator());
            for (Day d : planning.getDays()) {
                bw.write("#Day " + d.getId() + System.lineSeparator());
                for (Block b : d) {
                    bw.write(String.valueOf(b.getId()));
                    for (Machine m : planning.getMachines()) {
                        bw.write(";" + b.getMachineState(m).toString());
                    }
                    bw.write(System.lineSeparator());
                }
                bw.write("#Shipped request ids" + System.lineSeparator());
                int teller = 0;
                for (Request r : planning.getRequests()) {
                    if (r.getShippingDay() != null && r.getShippingDay().equals(d)) {
                        if (teller != 0) {
                            bw.write(";");
                        }
                        bw.write(Integer.toString(r.getId()));
                        teller++;
                    }
                }
                if (teller == 0) {
                    bw.write("-1");
                }
                bw.write(System.lineSeparator());
                bw.write("#Night shift" + System.lineSeparator());
                bw.write((d.hasNightShift() ? 1 : 0) + System.lineSeparator());
            }
            bw.flush();
            bw.close();
        } catch (IOException e) {
            System.err.println("4. IOException");
            e.printStackTrace();
            System.exit(4);
        }
    }

    /**
     * This function writes the wished output of a planning to the console
     *
     * @param planning The planning which has to be written to the console.
     */
    public static void printOutputToConsole(Planning planning) {
        System.out.println("Instance_name: " + planning.getInstanceName());
        System.out.println("Cost: " + String.format("%.2f", planning.getTotalCost()));
        for (Day d : planning.getDays()) {
            System.out.println("#Day " + d.getId());
            System.out.print("Previous items: ");
            for (Machine m : planning.getMachines()) {
                Item item = m.getPreviousItem(planning, d.getId(), 0);
                System.out.print(item.getId() + ";");
            }
            System.out.println();

            for (Block b : d) {
                System.out.print(b.getId());
                for (Machine m : planning.getMachines()) {
                    System.out.print("\t \t ;" + b.getMachineState(m));
                }
                System.out.println();
            }
            System.out.println("#Shipped request ids");
            int teller = 0;
            for (Request r : planning.getRequests()) {
                if (r.getShippingDay() != null && r.getShippingDay().equals(d)) {
                    if (teller != 0) {
                        System.out.print(";");
                    }
                    System.out.print(r.getId());
                    teller++;
                }
            }
            if (teller == 0) {
                System.out.print("-1");
            }
            System.out.println();
            System.out.println("#Night shift");
            System.out.println(d.hasNightShift() ? 1 : 0);
        }
    }

    /**
     * This static function reads in all information from the inputfile and returns
     * a new Planning object with all this information.
     * <p>
     * It's recommended to use main.Main.makeInitialPlanning after this operation to make
     * a fully feasible planning.
     *
     * @param fileName name of the inputfile
     * @return new Planning with all inputfile restrictions
     */
    private static Planning readFileIn(String fileName) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            logger.warning("1. Inputfile " + fileName + " not found.");
            System.exit(1);
        }

        int input_block = 0;
        int i = 0;

        String instanceName = "";
        int nrOfMachines = 0;
        Planning planning = null;
        Stock stock = null;
        Requests requests = null;

        while (scanner.hasNextLine()) {
            String inputLine = scanner.nextLine();
            String[] inputDelen = inputLine.split(" ");
            if (inputDelen[0].equals("Instance_name:")) {
                instanceName = inputDelen[1];
            } else if (inputDelen[0].equals("Number_of_machines:")) {
                nrOfMachines = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Number_of_different_items:")) {
                int nrOfDifferentItems = Integer.parseInt(inputDelen[1]);
                stock = new Stock(nrOfDifferentItems);
            } else if (inputDelen[0].equals("Number_of_days:")) {
                Planning.setNumberOfDays(Integer.parseInt(inputDelen[1]));
            } else if (inputDelen[0].equals("Number_of_requests:")) {
                requests = new Requests();
                for (int j = 0; j < Integer.parseInt(inputDelen[1]); j++) {
                    requests.add(new Request(j));
                }
            } else if (inputDelen[0].equals("Number_of_blocks_per_day:")) {
                Day.setNumberOfBlocksPerDay(Integer.parseInt(inputDelen[1]));
                planning = new Planning(instanceName, nrOfMachines);
                planning.setStock(stock);
                planning.setRequests(requests);
            } else if (inputDelen[0].equals("Index_of_block_e:")) {
                Day.indexOfBlockE = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Index_of_block_l:")) {
                Day.indexOfBlockL = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Index_of_block_s:")) {
                Day.indexOfBlockS = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Index_of_block_o:")) {
                Day.indexOfBlockO = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Min_consecutive_days_with_night_shifts:")) {
                Planning.setMinConsecutiveDaysWithNightShift(Integer.parseInt(inputDelen[1]));
            } else if (inputDelen[0].equals("Past_consecutive_days_with_night_shifts:")) {
                assert planning != null;
                planning.setPastConsecutiveDaysWithNightShift(Integer.parseInt(inputDelen[1]));
            } else if (inputDelen[0].equals("Cost_of_overtime_p_o:")) {
                COST_OF_OVERTIME = Double.parseDouble(inputDelen[1]);
            } else if (inputDelen[0].equals("Cost_of_nightShift_p_n:")) {
                COST_OF_NIGHT_SHIFT = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Cost_of_parallel_task_p_p:")) {
                COST_OF_PARALLEL_TASK = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Penalty_per_item_under_minimum_level_p_s:")) {
                COST_PER_ITEM_UNDER_MINIMUM_LEVEL = Double.parseDouble(inputDelen[1]);
            } else if (inputLine.startsWith("#Machines data")) {
                input_block = 1;
                i = 0;
            } else if (inputLine.startsWith("#Items data")) {
                input_block = 2;
                i = 0;
            } else if (inputLine.startsWith("#Machine efficiency per item")) {
                input_block = 3;
                i = 0;
            } else if (inputLine.startsWith("#Large setup description matrix")) {
                input_block = 4;
                i = 0;
            } else if (inputLine.startsWith("#Machine setup duration in blocks")) {
                input_block = 5;
                i = 0;
            } else if (inputLine.startsWith("#Shipping day matrix")) {
                input_block = 6;
                i = 0;
            } else if (inputLine.startsWith("#Requested items matrix")) {
                input_block = 7;
                i = 0;
            } else if (input_block == 1) {
                int id = Integer.parseInt(inputDelen[0]);
                int lastItemIdProduced = Integer.parseInt(inputDelen[1]);
                int daysPastWithoutMaintenance = Integer.parseInt(inputDelen[2]);
                int maxDaysWithoutMaintenance = Integer.parseInt(inputDelen[3]);
                int maintenanceDurationInBlocks = Integer.parseInt(inputDelen[4]);
                //TODO assert planning != null;
                //TODO assert stock != null;
                planning.addMachine(new Machine(id, stock.getItem(lastItemIdProduced), daysPastWithoutMaintenance,
                        maxDaysWithoutMaintenance, maintenanceDurationInBlocks));
                i++;
            } else if (input_block == 2) {
                int id = Integer.parseInt(inputDelen[0]);
                double costPerItem = Double.parseDouble(inputDelen[1]);
                int quantityInStock = Integer.parseInt(inputDelen[2]);
                int minAllowedInStock = Integer.parseInt(inputDelen[3]);
                int maxAllowedInStock = Integer.parseInt(inputDelen[4]);
                //TODO assert stock != null;
                Item item = stock.getItem(id);
                item.setCostPerItem(costPerItem);
                item.setInitialQuantityInStock(quantityInStock);
                item.setMaxAllowedInStock(maxAllowedInStock);
                item.setMinAllowedInStock(minAllowedInStock);
                //TODO assert planning != null;
                i++;
            } else if (input_block == 3) {
                int id = Integer.parseInt(inputDelen[0]);
                for (int j = 1; j < nrOfMachines + 1; j++) {
                    planning.getMachines().get(j - 1).addEfficiency(stock.getItem(id), Integer.parseInt(inputDelen[j]));
                }
            } else if (input_block == 4) {
                //TODO assert stock != null;
                Item item = stock.getItem(i);
                for (int j = 0; j < stock.getNrOfDifferentItems(); j++) {
                    if (i != j) {
                        item.setLargeSetup(stock.getItem(j), inputDelen[j].equals("1"));
                    }
                }
                i++;
            } else if (input_block == 5) {
                //TODO assert stock != null;
                Item item = stock.getItem(i);
                for (int j = 0; j < stock.getNrOfDifferentItems(); j++) {
                    if (i != j) {
                        item.setSetupTime(stock.getItem(j), Integer.parseInt(inputDelen[j]));
                    }
                }
                i++;
            } else if (input_block == 6) {
                Request request = requests.get(i);
                for (int j = 0; j < Planning.getNumberOfDays(); j++) {
                    if (inputDelen[j].equals("1")) {
                        //TODO assert planning != null;
                        request.addPossibleShippingDay(planning.getDay(j));
                    }
                }
                i++;
            } else if (input_block == 7) {
                //TODO assert requests != null;
                Request request = requests.get(i);
                for (int j = 0; j < stock.getNrOfDifferentItems(); j++) { //TODO getNrOfDifferentItems() can return null
                    if (!inputDelen[j].equals("0")) {
                        request.addItem(stock.getItem(j), Integer.parseInt(inputDelen[j]));
                    }
                }
                i++;
            } else {
                logger.warning("3. Een lijn is niet verwerkt bij het inlezen van de inputfile: ");
                logger.warning(inputLine);
                System.exit(3);
            }
        }
        scanner.close();
        return planning;
    }

}
