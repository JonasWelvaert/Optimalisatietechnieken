import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import model.Block;
import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.Request;

public class Planner {
	private static final String inputFileName = "toy_inst.txt";
	private static int MIN_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS;
	private static int PAST_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS;
	private static double COST_OF_OVERTIME;
	private static int COST_OF_NIGHTSHIFT;
	private static int COST_OF_PARALLEL_TASK;
	private static double PENALTY_PER_ITEM_UNDER_MINIMUM_LEVEL;
	private static Planning planning;
	private static List<Request> requests;

	public static void main(String[] args) {
		// GIT test: de laatste verwijdert de hele git blok!
		System.out.println("#Verwijder jouw lijn als je kan bewerken op GIT.");
		System.out.println("\tNick Braeckman");
		System.out.println("\tElke Govaert");

		// 1. inputfile

		System.out.println("Starting reading of input file " + inputFileName);
		ReadFileIn();
		System.out.println("Finished reading of input file " + inputFileName);

		// 2. initial solution
		System.out.println("Starting making first feasible solution");
		// TODO
		optimize(planning);
		if (!checkFeasible(planning)) {
			System.err.println("2. Initial solution is not feasible!");
			System.exit(2);
		}
		System.out.println("Finished making of first feasible solution");

		// 3. optimalisation
		System.out.println("Starting optimalisation");
		// TODO
		// metaheuristik();
		boolean feasible = checkFeasible(planning);
		if (feasible) {
			evaluate(new ArrayList<Day>());
		}
		System.out.println("Finished optimalisation");

		// 4. output
		System.out.println("Starting writing outputfile");
		// TODO

		System.out.println("Finished writing outputfile");
	}

	private static void optimize(Planning planning) {

	}

	private static boolean checkFeasible(Planning planning) {

		return true;
	}

	private static void evaluate(List<Day> solution) {

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
		String instanceName = "";
		int nrOfMachines = 0;
		int[][] largeSetupMatrix = null;
		int[][] machineSetupDuration = null;
		int i = 0;
		while (scanner.hasNextLine()) {
			String inputLine = scanner.nextLine();
			String[] inputDelen = inputLine.split(" ");
			if (inputDelen[0].equals("Instance_name:")) {
				instanceName = inputDelen[1];
			} else if (inputDelen[0].equals("Number_of_machines:")) {
				nrOfMachines = Integer.parseInt(inputDelen[1]);
				planning = new Planning(instanceName, nrOfMachines);
			} else if (inputDelen[0].equals("Number_of_different_items:")) {
				Item.NUMBER_OF_DIFFERENT_ITEMS = Integer.parseInt(inputDelen[1]);
				largeSetupMatrix = new int[Item.NUMBER_OF_DIFFERENT_ITEMS][Item.NUMBER_OF_DIFFERENT_ITEMS];
				machineSetupDuration = new int[Item.NUMBER_OF_DIFFERENT_ITEMS][Item.NUMBER_OF_DIFFERENT_ITEMS];
			} else if (inputDelen[0].equals("Number_of_days:")) {
				Planning.NR_OF_DAYS = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Number_of_requests:")) {
				requests = new ArrayList<>(Integer.parseInt(inputDelen[1]));
			} else if (inputDelen[0].equals("Number_of_blocks_per_day:")) {
				Day.NUMBER_OF_BLOCKS_PER_DAY = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_e:")) {
				Day.INDEX_OF_BLOCK_E = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_l:")) {
				Day.INDEX_OF_BLOCK_L = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_s:")) {
				Day.INDEX_OF_BLOCK_S = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_o:")) {
				Day.INDEX_OF_BLOCK_O = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Min_consecutive_days_with_night_shifts:")) {
				MIN_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Past_consecutive_days_with_night_shifts:")) {
				PAST_CONSECUTIVE_DAYS_WITH_NIGHT_SHIFTS = Integer.parseInt(inputDelen[1]);
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
			} else if (inputLine.startsWith("#Items data")) {
				input_block = 2;
			} else if (inputLine.startsWith("#Machine efficiency per item")) {
				input_block = 3;
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
				// TODO Machine machine = new Machine(id);
			} else if (input_block == 2) {
				int id = Integer.parseInt(inputDelen[0]);
				double costPerItem = Double.parseDouble(inputDelen[1]);
				int quantityInStock = Integer.parseInt(inputDelen[2]);
				int minAllowedInStock = Integer.parseInt(inputDelen[3]);
				int maxAllowedInStock = Integer.parseInt(inputDelen[4]);
				// TODO
			} else if (input_block == 3) {
				int id = Integer.parseInt(inputDelen[0]);
				int[] productionInMachinePerBlock = Arrays.stream(Arrays.copyOfRange(inputDelen, 1, nrOfMachines + 1))
						.mapToInt(Integer::parseInt).toArray();
				int productionInMachine0PerBlock = Integer.parseInt(inputDelen[1]);
				int productionInMachine1PerBlock = Integer.parseInt(inputDelen[2]);
				// TODO
			} else if (input_block == 4) {
				largeSetupMatrix[i] = Arrays.stream(inputDelen).mapToInt(Integer::parseInt).toArray();
				i++;
				if (i == Item.NUMBER_OF_DIFFERENT_ITEMS) {
					// TODO
				}
			} else if (input_block == 5) {
				machineSetupDuration[i] = Arrays.stream(inputDelen).mapToInt(Integer::parseInt).toArray();
				i++;
				if (i == Item.NUMBER_OF_DIFFERENT_ITEMS) {
					// TODO
				}
			} else if (input_block == 6) {
				int[] shippingDays = Arrays.stream(inputDelen).mapToInt(Integer::parseInt).toArray();
				i++;
				// TODO
			} else if (input_block == 7) {
				int[] requestedItems = Arrays.stream(inputDelen).mapToInt(Integer::parseInt).toArray();
				i++;
				// TODO
			} else {
				System.err.println("3. Een lijn is niet verwerkt bij het inlezen van de inputfile: ");
				System.err.println(inputLine);
				System.exit(3);
			}
		}
		scanner.close();
	}

	private static void metaHeuristic() {
		int solution = 0;
		int teller = 0;
		while (teller < 1000) {
			int value = localSearch();
			if (solution < value) {
				solution = value;
				teller = 0;
			} else {
				teller++;
			}
		}
	}

	private static int localSearch() {
		// some localsearch thing.
		return 1;/*
					 * dns × pn + to × po + X r∈V X i∈I (q i r × ci) + X d∈D (ud × ps) + dp × pp
					 */
	}

}
