package main;

import feasibilitychecker.Counting;
import feasibilitychecker.FeasibiltyChecker;
import model.*;
import model.machinestate.Idle;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.LargeSetup;
import model.machinestate.setup.SmallSetup;
import solver.SimulatedAnnealingSolver;
import solver.Solver;
import solver.SteepestDescentSolver;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

import static main.EnumInputFile.*;

public class Main {
	private static final EnumInputFile inputFileName = Toy;
	private static String inputFile;
	private static String outputFile;
	private static final String outputPrefix = "SA3";
	private static final Logger logger = Logger.getLogger(Main.class.getName());
	private static final Validator validator = new Validator();
	public static double COST_OF_OVERTIME;
	public static double COST_OF_NIGHT_SHIFT;
	public static double COST_OF_PARALLEL_TASK;
	public static double COST_PER_ITEM_UNDER_MINIMUM_LEVEL;
	public static double initialCost;
	private static final String titlePrefix = "\t \t \t ****************************";
	public static final List<String> graphingOutput = new ArrayList<>();
	private static final FeasibiltyChecker feasibiltyChecker = new FeasibiltyChecker();
	public static long seed = -1; // -1 -> current timestamp, >=0 are valid seeds.
	public static Planning initialPlanning;
	public static Planning bestPlanning;

	/* -------------------------------- FOLDERS -------------------------------- */
	public static String graphingFolder = "GraphingOutput/";
	public static String costFolder = "Costs/";
	public static final String SAx_FOLDER = outputPrefix + "/";
	public static final String INSTANCE_FOLDER = "instances/";
	public static final String CSV_SEP = ",";

	/*
	 * -------------------------------- PARAMETERS --------------------------------
	 */
	public static final int temperature = 10000; // 1000
	public static final double cooling = 0.9995; // 0.9999
	public static final boolean tempReset = true; // true
	public static final double exponentialRegulator = 150; // 10 (>1 will accept more worse solutions)

	public static final int iterations = 100000; // BE SURE TO USE THE CORRECT PARAMETERS!

	public static void main(String[] args) throws IOException, InterruptedException {
		// logger.setLevel(Level.OFF);

		long timeLimit = 600;// in seconds 300=5min
		int nrOfThreads = 1; // for debugging = 1 //TODO @jonas increase seed for every thread
		if (args.length == 1) {
			inputFile = INSTANCE_FOLDER + args[0];
			outputFile = SAx_FOLDER + outputPrefix + "_" + args[0];
		} else if (args.length > 4) {
			inputFile = args[0];
			outputFile = args[1];
			seed = Long.parseLong(args[2]);
			timeLimit = Long.parseLong(args[3]);
			nrOfThreads = Integer.parseInt(args[4]);
		} else {
			inputFile = INSTANCE_FOLDER + inputFileName.toString();
			outputFile = SAx_FOLDER + outputPrefix + "_" + inputFileName.toString();
		}

		// 1. READ IN
		logger.info(titlePrefix + "1. Reading file " + inputFile);
		initialPlanning = readFileIn(inputFile);

		// 2. BUILD INITIAL SOLUTION
		logger.info(titlePrefix + "2. Build initial solution");
		initialPlanning = makeInitialPlanning(initialPlanning);

		if (!feasibiltyChecker.checkFeasible(initialPlanning)) {
			logger.severe("2. Initial planning is not feasible!");
			System.exit(2);
		}

		// 3. OPTIMIZE
		logger.info(titlePrefix + "3. Optimize");
		bestPlanning = new Planning(initialPlanning);
		resultFound(initialPlanning);

		ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(nrOfThreads);

		for (int i = 0; i < 1 * nrOfThreads; i++) { // aantal taken die aan de pool toegewezen worden. // voor debuggin
													// =1
			pool.submit(new Callable<Planning>() {

				@Override
				public Planning call() throws Exception {
					Solver solver;

					// solver = new SteepestDescentSolver(iterations, new FeasibiltyChecker());

					solver = new SimulatedAnnealingSolver(new FeasibiltyChecker(), temperature, cooling);

					Planning optimizedPlanning = solver.optimize(new Planning(initialPlanning));
					if (optimizedPlanning != null) {
						Main.resultFound(optimizedPlanning);
					}
					return optimizedPlanning;
				}

			});

		}
		Thread.sleep(timeLimit * 1000);
		pool.shutdownNow();
		System.exit(0);

	}

	public static synchronized void resultFound(Planning p) throws IOException {
		if (bestPlanning.getTotalCost() >= p.getTotalCost()) {
			bestPlanning = new Planning(p);

			logger.info(titlePrefix + "4A. Printing result to console");
			printOutputToConsole(bestPlanning);

			System.out.println(outputFile);
			logger.info(titlePrefix + "4B. Printing result to file");
			printOutputToFile(outputFile, bestPlanning);

			logger.info(titlePrefix + "5. Validate Solution");
			validator.validate(inputFile, outputFile);

			logger.info(titlePrefix + "6. Writing optimisation points to csv:");
			writingOptimisationPointsToCSV(bestPlanning);
			logCostToCSV(bestPlanning);

			logger.info(titlePrefix + "7. Resume:");
			logger.info("Initial cost: \t" + initialCost);
			logger.info("Total cost: \t" + bestPlanning.getTotalCost());
			logger.info("Validator valid: " + validator.isValid());
		}
	}

	/**
	 * Writes out a line to a csv line (separated by a ;) This CSV contains the
	 * improvement made by the algorithm
	 *
	 * @param p planning of which the optimization points needs to be written out
	 */
	private static void writingOptimisationPointsToCSV(Planning p) throws IOException {
		File optimisationFile = new File(graphingFolder + p.getInstanceName() + "_optimisation.csv");
		optimisationFile.createNewFile();

		FileWriter fw = new FileWriter(optimisationFile, true);
		PrintWriter out = new PrintWriter(fw);

		for (String s : graphingOutput) {
			out.println(s);
		}

		out.flush();
		out.close();
		fw.close();
	}

	private static void logCostToCSV(Planning p) throws IOException {
		File fileCost = new File(costFolder + "costs.csv");
		fileCost.createNewFile();

		FileWriter fw = new FileWriter(fileCost, true);
		PrintWriter out = new PrintWriter(fw);

		LocalDateTime now = LocalDateTime.now();
		String valid;
		if (validator.isValid()) {
			valid = "VALID";
		} else {
			valid = "IN-VALID";
		}

		out.println(now + "\t;" + p.getInstanceName() + "\t\t\t ;" + p.getTotalCost() + "\t;" + valid + "\t ;"
				+ validator.getShipments());

		out.flush();
		out.close();
		fw.close();
	}

	/**
	 * Makes all machines IDLE and no requests fullfilled except for the maintenance
	 * and nightshifts to ensure a feasible planning.
	 *
	 * @param p The planning returned by the main.Main.readFileIn method
	 * @return A initial feasible planning
	 */
	private static Planning makeInitialPlanning(Planning p) throws IOException {
		for (Day d : p.getDays()) {
			// putting all machines all the time in idle.
			for (Machine m : p.getMachines()) {
				for (Block b : d.getBlocks()) {
					b.setMachineState(m, new Idle());
				}
			}
			// then stock will always be the initial stock.
			for (Item i : p.getStock().getItems()) {
				i.setStockAmount(d, i.getInitialQuantityInStock());
			}
		}

		// now we need to make sure all nightshifts are fullfilled.
		if (p.getPastConsecutiveDaysWithNightShift() != 0) {
			for (int i = 0; i < p.getMinConsecutiveDaysWithNightShift()
					- p.getPastConsecutiveDaysWithNightShift(); i++) {
				p.getDay(i).setNightShift(true);
			}
		}

		// now we need to make sure all needed maintenances are planned
		for (Machine m : p.getMachines()) {
			int init = m.getInitialDaysPastWithoutMaintenance();
			int max = m.getMaxDaysWithoutMaintenance();
			for (int i = 0; i < Planning.getNumberOfDays(); i++) {
				if (Math.floorMod(i, max + 1) == max + 1 - (init + 1)) { // Romeo: die floorMod() snap ik niet goed
					// Jonas: ik ga ervanuit dat in een IDLE planning, de beide machines kunnen
					// onderhouden worden.
					int teller = 0;
					for (int j = Day.indexOfBlockE; j <= Day.indexOfBlockL; j++) {
						if (!p.getDay(i).getBlock(j).isInMaintenance()) {
							p.getDay(i).getBlock(j).setMachineState(m, new Maintenance());
							teller++;
							if (teller == m.getMaintenanceDurationInBlocks()) {
								break;
							}
						}
					}
				}
			}
		}

		p.calculateAllCosts();

		Planning ret = new Planning(p);
		for (int i = 0; i < 8; i++) {
			boolean useCheckedItems;
			boolean useEndOfBlockShipping;
			boolean useEndOfDayShipping;
			if (i < 4) {
				useCheckedItems = false;
			} else {
				useCheckedItems = true;
			}
			if (i % 4 < 2) {
				useEndOfBlockShipping = false;
			} else {
				useEndOfBlockShipping = true;
			}
			if (i % 2 < 1) {
				useEndOfDayShipping = false;
			} else {
				useEndOfDayShipping = true;
			}
			Planning planning = new Planning(p);
			produceAndShip(useCheckedItems, useEndOfBlockShipping, useEndOfDayShipping, planning);
			planning.calculateAllCosts();
			if (planning.getTotalCost() <= ret.getTotalCost()) {
				ret = planning;
			}
		}
		p = ret;
		initialCost = p.getTotalCost();
		printOutputToConsole(p);
		return p;
	}

	private static void produceAndShip(boolean useCheckedItems, boolean useEndOfBlockShipping,
			boolean useEndOfDayShipping, Planning p) {
		Set<Item> checkedItems = new HashSet<>();
		machine: for (Machine m : p.getMachines()) {
			Item item = m.getInitialSetup();
			checkedItems.add(item);
			boolean changeItem = false;
			int nrOfBlocks = 0;

			int iteration = 0;

			int t1 = 0;
			int t2 = Day.getNumberOfBlocksPerDay() - 1; // @indexOutOfBounds

			while (iteration < Planning.getNumberOfDays()) {
				Day day = p.getDay(iteration);
				List<Block> possibleBlocks = day.getIdleBlocksWithoutOvertimeButWithNighshiftBetweenInclusive(t1, t2,
						m);
				block: for (Block b : possibleBlocks) {
					if (b.getMachineState(m) instanceof Idle) {

						int efficiency = m.getEfficiency(item);
						if (efficiency == 0) {
							changeItem = true;
						} else {
							int maxStock = item.getMaxAllowedInStock();
							int maxNeeded = p.getRequests().amountOfItemNeeded(item);
							checkstock: for (Day d : p.getSuccessorDaysInclusive(day)) {
								int inStock = item.getStockAmount(d);
								if (maxNeeded > inStock) {
									if (inStock + efficiency <= maxStock) {
										changeItem = false;
									} else {
										changeItem = true;
										break checkstock;
									}
								} else {
									changeItem = true;
									break checkstock;
								}
							}
						}
						if (!changeItem) {
							b.setMachineState(m, new Production(item));
							p.updateStockLevels(day, item, m.getEfficiency(item));
							if (useEndOfBlockShipping) {
								// WE GAAN SHIPPINGS PLANNEN!
								request: for (Request request : p.getRequests()) {
									if (request.getShippingDay() == null) {
										Day sd;
										if (request.getPossibleShippingDays().contains(day)) {
											sd = day;
										} else {
											continue request;
										}

										// FOR SD CHECK IF IN FUTURE STOCK IS NOT VIOLATED
										for (Day d : p.getSuccessorDaysInclusive(sd)) {
											for (Item i : request.getItemsKeySet()) {
												if (i.getStockAmount(d) - request.getAmountOfItem(i) < 0) {
													continue request;
												}
											}
										}

										// IF STOCK NOT VIOLATED, PLAN SHIPMENT
										request.setShippingDay(sd);

										for (Item i : request.getItemsKeySet()) {
											int delta = -1 * request.getAmountOfItem(i);
											p.updateStockLevels(sd, i, delta);
										}
									}
								}
							}
							continue block;
						} else {
							// CHECK IF LARGE setup POSSIBLE
							boolean isLargePossible = true;
							if (Day.indexOfBlockE <= b.getId() && b.getId() <= Day.indexOfBlockL) {
								nrOfBlocks = Day.indexOfBlockL - b.getId() + 1;
							} else {
								isLargePossible = false;
							}
							if (isLargePossible) {
								// LARGE SETUP, nrOfBlocks al berekend
								// CHECK MAX DURATION
								int realNrOfPossibleBlcks = 0;
								machineTest: for (Machine m1 : p.getMachines()) {
									if (m1 == m) {
										continue machineTest;

									} else {
										// CHECK FOR NO PARALLEL
										for (int i = 0; i < nrOfBlocks; i++) {
											Block b0 = day.getBlock(b.getId() + i);
											if (b0.getMachineState(m1) instanceof Maintenance
													|| b0.getMachineState(m1) instanceof LargeSetup) {
												break machineTest;
											} else {
												realNrOfPossibleBlcks++;
											}
										}
									}
								}
								if (realNrOfPossibleBlcks <= 0) {
									isLargePossible = false;
								} else {
									// large setup met teller blokken
									for (Item i0 : p.getStock().getItems()) {

										if (!checkedItems.contains(i0) && useCheckedItems) {
											if (m.getEfficiency(i0) != 0) {
												boolean gaVerder = true;
												int efficiency0 = m.getEfficiency(i0);
												int maxStock = i0.getMaxAllowedInStock();
												int maxNeeded = p.getRequests().amountOfItemNeeded(i0);
												checkstock: for (Day d : p.getSuccessorDaysInclusive(day)) {
													int inStock = item.getStockAmount(d);
													if (maxNeeded > inStock) {
														if (inStock + efficiency0 <= maxStock) {
															gaVerder = true;
														} else {
															gaVerder = false;
															break checkstock;
														}
													} else {
														gaVerder = false;
														break checkstock;
													}
												}
												if (gaVerder) {
													if (item.isLargeSetup(i0)) {
														if (item.getSetupTimeTo(i0) <= realNrOfPossibleBlcks) {
															for (int i = 0; i < item.getSetupTimeTo(i0); i++) {
																Block b0 = day.getBlock(b.getId() + i);
																b0.setMachineState(m, new LargeSetup(item, i0));
															}
															item = i0;
															checkedItems.add(i0);
															continue block;
														}
													}
												}
											}
										}
									}

								}
							}
							// DO SMALL SETUP
							for (Item i0 : p.getStock().getItems()) {

								if (!checkedItems.contains(i0) && useCheckedItems) {
									if (m.getEfficiency(i0) != 0) {
										boolean gaVerder = true;
										int efficiency0 = m.getEfficiency(i0);
										int maxStock = i0.getMaxAllowedInStock();
										int maxNeeded = p.getRequests().amountOfItemNeeded(i0);
										checkstock: for (Day d : p.getSuccessorDaysInclusive(day)) {
											int inStock = item.getStockAmount(d);
											if (maxNeeded > inStock) {
												if (inStock + efficiency0 <= maxStock) {
													gaVerder = true;
												} else {
													gaVerder = false;
													break checkstock;
												}
											} else {
												gaVerder = false;
												break checkstock;
											}
										}
										if (gaVerder) {
											if (!item.isLargeSetup(i0)) {
												int blockId = b.getId();
												List<Block> blocks = day
														.getConsecutiveIdleBlocksWithoutOvertimeButWithNighshiftBetweenInclusive(
																blockId, Day.getNumberOfBlocksPerDay() - 1, m);
												if (blocks.size() < item.getSetupTimeTo(i0)) {
													if (day.getId() + 1 < Planning.getNumberOfDays()) {
														int amount = item.getSetupTimeTo(i0) - blocks.size();
														blocks.addAll(p.getDay(day.getId() + 1)
																.getConsecutiveIdleBlocksWithoutOvertimeButWithNighshiftBetweenInclusive(
																		0, amount - 1, m));
													} else {
														continue machine;
													}
												}
												for (int i = 0; i < item.getSetupTimeTo(i0); i++) {
													Block b0 = blocks.get(i);
													b0.setMachineState(m, new SmallSetup(item, i0));
												}
												item = i0;
												checkedItems.add(i0);
												continue block;
											}
										}
									}
								}
							}
						}
					}
				}
				if (useEndOfDayShipping) {
					// WE GAAN SHIPPINGS PLANNEN!
					request: for (Request request : p.getRequests()) {
						if (request.getShippingDay() == null) {
							Day sd;
							if (request.getPossibleShippingDays().contains(day)) {
								sd = day;
							} else {
								continue request;
							}

							// FOR SD CHECK IF IN FUTURE STOCK IS NOT VIOLATED
							for (Day d : p.getSuccessorDaysInclusive(sd)) {
								for (Item i : request.getItemsKeySet()) {
									if (i.getStockAmount(d) - request.getAmountOfItem(i) < 0) {
										continue request;
									}
								}
							}

							// IF STOCK NOT VIOLATED, PLAN SHIPMENT
							request.setShippingDay(sd);

							for (Item i : request.getItemsKeySet()) {
								int delta = -1 * request.getAmountOfItem(i);
								p.updateStockLevels(sd, i, delta);
							}
						}
					}
				}
				// NEXT DAY
				iteration++;

			}
		}
	}

	/**
	 * This function writes the wished output of a planning to a file
	 *
	 * @param filename The filename where to write the output
	 * @param planning The planning which has to be written to the console.
	 */
	public static void printOutputToFile(String filename, Planning planning) {

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
				Item item = m.getPreviousItem(planning, d, d.getBlock(0));
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
		System.out.println("#JoinSingleNeighbouringSetup actually done: " + Counting.JoinSingleNeighbouringSetup);
		System.out.println("Error counting" + feasibiltyChecker.getEc());
	}

	/**
	 * This static function reads in all information from the inputfile and returns
	 * a new Planning object with all this information.
	 * <p>
	 * It's recommended to use main.Main.makeInitialPlanning after this operation to
	 * make a fully feasible planning.
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
		Planning p = null;
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
				Planning.setNumberOfDays(Integer.parseInt(inputDelen[1])); // TODO
			} else if (inputDelen[0].equals("Number_of_requests:")) {
				requests = new Requests();
				for (int j = 0; j < Integer.parseInt(inputDelen[1]); j++) {
					requests.add(new Request(j));
				}
			} else if (inputDelen[0].equals("Number_of_blocks_per_day:")) {
				Day.setNumberOfBlocksPerDay(Integer.parseInt(inputDelen[1]));
				p = new Planning(instanceName, nrOfMachines);
				p.setStock(stock);
				p.setRequests(requests);
			} else if (inputDelen[0].equals("Index_of_block_e:")) {
				Day.indexOfBlockE = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_l:")) {
				Day.indexOfBlockL = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_s:")) {
				Day.indexOfBlockS = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Index_of_block_o:")) {
				Day.indexOfBlockO = Integer.parseInt(inputDelen[1]);
			} else if (inputDelen[0].equals("Min_consecutive_days_with_night_shifts:")) {
				p.setMinConsecutiveDaysWithNightShift(Integer.parseInt(inputDelen[1]));
			} else if (inputDelen[0].equals("Past_consecutive_days_with_night_shifts:")) {
				assert p != null;
				p.setPastConsecutiveDaysWithNightShift(Integer.parseInt(inputDelen[1]));
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
				p.addMachine(new Machine(id, stock.getItem(lastItemIdProduced), daysPastWithoutMaintenance,
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
				for (int j = 1; j < nrOfMachines + 1; j++) {
					p.getMachines().get(j - 1).addEfficiency(stock.getItem(id), Integer.parseInt(inputDelen[j]));
				}
			} else if (input_block == 4) {
				Item item = stock.getItem(i);
				for (int j = 0; j < Stock.getNrOfDifferentItems(); j++) {
					if (i != j) {
						item.setLargeSetup(stock.getItem(j), inputDelen[j].equals("1"));
					}
				}
				i++;
			} else if (input_block == 5) {
				Item item = stock.getItem(i);
				for (int j = 0; j < Stock.getNrOfDifferentItems(); j++) {
					if (i != j) {
						item.setSetupTime(stock.getItem(j), Integer.parseInt(inputDelen[j]));
					}
				}
				i++;
			} else if (input_block == 6) {
				Request request = requests.get(i);
				for (int j = 0; j < Planning.getNumberOfDays(); j++) {
					if (inputDelen[j].equals("1")) {
						request.addPossibleShippingDay(p.getDay(j));
					}
				}
				i++;
			} else if (input_block == 7) {
				Request request = requests.get(i);
				for (int j = 0; j < Stock.getNrOfDifferentItems(); j++) {
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
		return p;
	}
}
