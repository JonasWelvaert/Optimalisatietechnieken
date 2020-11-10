
import model.*;
import model.machinestate.Idle;

import java.io.*;
import java.util.Scanner;

public class Main {
    private static final String inputFileName = "toy_inst.txt";
    private static int MIN_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS;
    private static int PAST_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS;
    private static double COST_OF_OVERTIME;
    private static int COST_OF_NIGHTSHIFT;
    private static int COST_OF_PARALLEL_TASK;
    private static double PENALTY_PER_ITEM_UNDER_MINIMUM_LEVEL;
    private static Planning planning;
    private static final long SEED = 1000;
    private static final long TIME_LIMIT = 60;

    public static void main(String[] args) {
        // GIT test: de laatste verwijdert de hele git blok!
        System.out.println("#Verwijder jouw lijn als je kan bewerken op GIT.");
        System.out.println("\tElke Govaert");

        // 1. inputfile
        System.out.println("Starting reading of input file " + inputFileName);
        ReadFileIn();
        System.out.println("Finished reading of input file " + inputFileName);

        // 2. initial solution
        System.out.println("Starting making first feasible solution");

        // putting all machines all the time in idle.
        for (Machine m : planning.getMachines()) {
            for (Day d : planning.getDays()) {
                for (Block b : d.getBlocks()) {
                    b.setMachineState(m, new Idle());
                }
            }
        }

        // then stock will always be the initial stock.
        for (Item i : planning.getStock().getItems()) {
            for (Day d : planning.getDays()) {
                i.setStockAmount(d, i.getInitialQuantityInStock());
            }
        }

        // now we need to make sure all needed maintenances are planned && all nightshifts are fullfilled.
        // TODO JONAS

        // after that, we have a feasible solution??
        if (!Solver.checkFeasible(planning)) {
            System.err.println("2. Initial solution is not feasible!");
            System.exit(2);
        }
        System.out.println("Finished making of first feasible solution");

        // 3. optimalisation
        System.out.println("Starting optimalisation");
        // TODO
        Solver.optimize(SEED, TIME_LIMIT, planning);
        System.out.println("Finished optimalisation");

        // 4. output
        System.out.println("Starting writing outputfile");
        printOutputToConsole();
        printOutputToFile("output_" + inputFileName);    //TODO somehow this gives strange number representations...
        System.out.println("Finished writing outputfile");
    }

    private static void printOutputToFile(String filename) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
            bw.write("Instance_name: " + planning.getInstanceName() + System.lineSeparator());
            bw.write("Cost: " + Integer.MAX_VALUE + System.lineSeparator());
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
                        bw.write(r.getId());
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
            System.exit(4);
        }
    }

    private static void printOutputToConsole() {
        System.out.println("Instance_name: " + planning.getInstanceName());
        System.out.println("Cost: " + Integer.MAX_VALUE);
        for (Day d : planning.getDays()) {
            System.out.println("#Day " + d.getId());
            for (Block b : d) {
                System.out.print(b.getId());
                for (Machine m : planning.getMachines()) {
                    System.out.print(";" + b.getMachineState(m));
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

    private static void ReadFileIn() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(inputFileName));
        } catch (FileNotFoundException e) {
            System.err.println("1. Inputfile " + inputFileName + " not found.");
            System.exit(1);
        }

        int input_block = 0;
        int i = 0;

        String instanceName = "";
        int nrOfMachines = 0;
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
                Planning.setPastConsecutiveDaysWithNightShift(Integer.parseInt(inputDelen[1]));
            } else if (inputDelen[0].equals("Cost_of_overtime_p_o:")) {
                COST_OF_OVERTIME = Double.parseDouble(inputDelen[1]);
            } else if (inputDelen[0].equals("Cost_of_nightShift_p_n:")) {
                COST_OF_NIGHTSHIFT = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Cost_of_parallel_task_p_p:")) {
                COST_OF_PARALLEL_TASK = Integer.parseInt(inputDelen[1]);
            } else if (inputDelen[0].equals("Penalty_per_item_under_minimum_level_p_s:")) {
                PENALTY_PER_ITEM_UNDER_MINIMUM_LEVEL = Double.parseDouble(inputDelen[1]);
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
                planning.addMachine(new Machine(id, stock.getItem(lastItemIdProduced), daysPastWithoutMaintenance,
                        maxDaysWithoutMaintenance, maintenanceDurationInBlocks));
                i++;
            } else if (input_block == 2) {
                int id = Integer.parseInt(inputDelen[0]);
                double costPerItem = Double.parseDouble(inputDelen[1]);
                int quantityInStock = Integer.parseInt(inputDelen[2]);
                int minAllowedInStock = Integer.parseInt(inputDelen[3]);
                int maxAllowedInStock = Integer.parseInt(inputDelen[4]);
                Item item = stock.getItem(id);
                item.setCostPerItem(costPerItem);
                item.setInitialQuantityInStock(quantityInStock);
                item.setMaxAllowedInStock(maxAllowedInStock);
                item.setMinAllowedInStock(minAllowedInStock);
                i++;
            } else if (input_block == 3) {
                int id = Integer.parseInt(inputDelen[0]);
                for (int j = 1; j < nrOfMachines; j++) {
                    planning.getMachines().get(j).addEfficiency(stock.getItem(id), Integer.parseInt(inputDelen[j]));
                }
            } else if (input_block == 4) {
                Item item = stock.getItem(i);
                for (int j = 0; j < stock.getNrOfDifferentItems(); j++) {
                    if (i != j) {
                        item.setLargeSetup(stock.getItem(j), inputDelen[j].equals("1"));
                    }
                }
                i++;
            } else if (input_block == 5) {
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
                        request.addPossibleShippingDay(planning.getDay(j));
                    }
                }
                i++;
            } else if (input_block == 7) {
                Request request = requests.get(i);
                for (int j = 0; j < stock.getNrOfDifferentItems(); j++) {
                    if (!inputDelen[j].equals("0")) {
                        request.addItem(stock.getItem(j), Integer.parseInt(inputDelen[j]));
                    }
                }
                i++;
            } else {
                System.err.println("3. Een lijn is niet verwerkt bij het inlezen van de inputfile: ");
                System.err.println(inputLine);
                System.exit(3);
            }
        }
        scanner.close();
    }

}
