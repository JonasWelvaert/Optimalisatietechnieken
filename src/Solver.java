import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.machinestate.*;
import java.util.Random;

public class Solver {
	public static final int SIMULATED_ANEALING = 100;
	private double SATemperature = 1000;
	private double SACoolingFactor = 0.995;
	private int mode;

	public Solver(int mode) {
		if (mode >= 100 && mode <= 100) {
			this.mode = mode;
		} else {
			throw new RuntimeException("Optimize mode not found in Solver constructor");
		}
	}

	public void setSimulatedAnealingFactors(double SATemperature, double SACoolingFactor) {
		this.SACoolingFactor = SACoolingFactor;
		this.SATemperature = SATemperature;
	}

	public Planning optimize(Planning initialPlanning) {
		if (mode == SIMULATED_ANEALING) {
			return optimizeUsingSimulatedAnealing(initialPlanning, SATemperature, SACoolingFactor);
		}
		// hier kunnen andere optimalisaties toegevoegd worden als deze niet goed
		// blijkt.
		throw new RuntimeException("Optimize mode not found in Solver.optimize()");
	}

	private Planning optimizeUsingSimulatedAnealing(Planning initialPlanning, double temperature,
			double coolingFactor) {
		Planning current = new Planning(initialPlanning);
		Planning best = initialPlanning;

		for (double t = temperature; t > 1; t *= coolingFactor) {
			Planning neighbor;
			do {
				neighbor = new Planning(current);
				neighbor = localSearch(neighbor);
			} while (!checkFeasible(neighbor));

			int neighborCost = neighbor.getCost();
			int currentCost = current.getCost();

			double probability;
			if (neighborCost < currentCost) {
				probability = 1;
			} else {
				probability = Math.exp((currentCost - neighborCost) / t);
			}
			if (Math.random() < probability) {
				current = new Planning(neighbor);
			}
			if (current.getCost() < best.getCost()) {
				best = new Planning(current);
			}
		}
		return best;
	}

    public static Planning localSearch(Planning optimizedPlanning) { // TODO Elke
        Random random = new Random();
        int randomInt = random.nextInt(12);
        if (randomInt == 1)  // willen niet constant maintenance zitten verplaatsen
            moveMaintenance(optimizedPlanning);
        else if (randomInt < 5)
            addProduction(optimizedPlanning);
        else if (randomInt < 9)
            removeProduction(optimizedPlanning);
        else
            changeProduction(optimizedPlanning);

        // TODO: hier al de cost wijzigen per wijziging

        return null;/*
         * dns × pn + to × po + SOM r∈V SOM i∈I (q i r × ci) + SOM d∈D (ud × ps) + dp ×
         * pp
         */
    }

    public static boolean checkFeasible(Planning planning) {
		// TODO Nick
		return checkProductionConstraints(planning);
	}

	private static boolean checkProductionConstraints(Planning planning) {
    	//TODO make echte code
    	/*for(Day d: planning.getDays()) {
    		if(d.hasNightShift()) {
    			teller++
    		}
    		else {
    			if(teller>0 en teller<minAMountofNightShifts) {
    				return false;
    			}else{
    			teller=0
    			}
    		}
    	}*/

        // check if the minimum amount of consecutive night shifts is fulfilled when their are past consecutive days
        if (planning.getPastConsecutiveDaysWithNightShift() > 0) {
            for (int i = 0; i < (Planning.getMinConsecutiveDaysWithNightShift() - planning.getPastConsecutiveDaysWithNightShift()); i++) {
                if (!planning.getDay(i).hasNightShift()) {
                    return false;
                }
            }
        }

        for (int k = 0; k < Planning.getNumberOfDays(); k++) {
            for (int i = 0; i < Day.getNumberOfBlocksPerDay(); i++) {

                // check that their is no maintenance/large setup between 0-b_e and b_l-b_n
                if (i < Day.indexOfBlockE || i > Day.indexOfBlockL) {
                    for (Machine m : planning.getMachines()) {
                        if (planning.getDay(k).getBlock(i).getMachineState(m) instanceof LargeSetup || planning.getDay(k).getBlock(i).getMachineState(m) instanceof Maintenance) {
                            return false;
                        }
                    }
                }

                checkOvertimeConstraints(i, k, planning);
            }
            checkStockConstraints(planning.getDay(k), planning);
        }

        return true;
    }

	private static boolean checkOvertimeConstraints(int i, int k, Planning planning) {

		if (planning.getDay(k).hasNightShift()) { // TODO nick verwijderen
			// check that the night shift blocks are consecutive
			if ((i > Day.indexOfBlockS) && i < (Day.getNumberOfBlocksPerDay() - 1)) {
				if (!isConsecutiveInOvertime(i, k, planning))
					return false;
			}

			// check that the minimum amount of consecutive days with night shift is
			// fulfilled
			if (k > (Planning.getMinConsecutiveDaysWithNightShift()
					- planning.getPastConsecutiveDaysWithNightShift())) {
				for (int l = k; l < (k + Planning.getMinConsecutiveDaysWithNightShift()); l++) {
					if (!planning.getDay(l).hasNightShift()) {
						return false;
					}
				}
			}
		} else {
			// check that the overtime blocks are consecutive
			if (i > Day.indexOfBlockS && i < Day.indexOfBlockO) {
				if (!isConsecutiveInOvertime(i, k, planning))
					return false;
			}

			// check that their is no overtime after block b_o for all machines
			if (i > Day.indexOfBlockO) {
				for (Machine m : planning.getMachines())
					if (!(planning.getDay(k).getBlock(i).getMachineState(m) instanceof Idle)) {
						return false;
					}
			}
		}

		return true;
	}

	private static boolean isConsecutiveInOvertime(int i, int k, Planning planning) {

		for (Machine m : planning.getMachines())
			if (planning.getDay(k).getBlock(i + 1).getMachineState(m) instanceof Production
					|| planning.getDay(k).getBlock(i + 1).getMachineState(m) instanceof SmallSetup) {
				if (!(planning.getDay(k).getBlock(i).getMachineState(m) instanceof Production
						|| planning.getDay(k).getBlock(i).getMachineState(m) instanceof SmallSetup)) {
					return false;
				}
			}
		return true;
	}

	private static boolean checkStockConstraints(Day day, Planning planning) {

		for (Item i : planning.getStock().getItems()) {
			if (i.getStockAmount(day) > i.getMaxAllowedInStock()) {
				return false;
			}
		}

		return true;
	}

	private static boolean checkMaintenanceConstraints(int k, Planning planning) {
		// TODO Nick
		for (Machine m : planning.getMachines()) {
			int init = m.getInitialDaysPastWithoutMaintenance();
			int max = m.getMaxDaysWithoutMaintenance();
			int now = max - init;
			int teller = 0;
			if (k < now) {
				for (int i = 0; i < Day.getNumberOfBlocksPerDay(); i++) {
					if (planning.getDay(k).getBlock(i).isInMaintenance()) {
						if (!isConsecutiveMaintenance(planning.getDay(k), planning))
							return false;
					} else {
						return false;
					}
				}
			}
		}
		return true;

	}

	private static boolean isConsecutiveMaintenance(Day day, Planning planning) {
		// TODO Nick
		return true;
	}

    private static void moveMaintenance(Planning p) {
        Random random = new Random();

        while (true) {
            //van
            int randomDay1 = random.nextInt(p.getNumberOfDays());
            int randomBlock1 = random.nextInt(p.getDay(0).getIndexOfBlockL()-p.getDay(0).getIndexOfBlockE()) + p.getDay(0).getIndexOfBlockE(); // enkel overdag
            int randMachineInt1 = random.nextInt(p.getMachines().size());
            Machine randMachine1 = p.getMachines().get(randMachineInt1);
            MachineState ms1 = p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1);
            String msString1 = ms1.toString();

            // naar
            int randomDay2 = random.nextInt(p.getNumberOfDays());
            int randomBlock2 = random.nextInt(p.getDay(0).getIndexOfBlockL()-p.getDay(0).getIndexOfBlockE()) + p.getDay(0).getIndexOfBlockE(); // enkel overdag
            int randMachineInt2 = random.nextInt(p.getMachines().size());
            Machine randMachine2 = p.getMachines().get(randMachineInt2);

            // als 1 = maintenance => verplaats naar 2
            if (msString1.equals("M") ) {
                // verplaats volledige maintenance sequence
                for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                    // zet op idle
                    p.getDay(randomDay1).getBlock(randomBlock1+i).setMachineState(randMachine1, new Idle());

                    // zet op maintenance
                    p.getDay(randomDay2).getBlock(randomBlock2+i).setMachineState(randMachine2, ms1);
                }
                // klaar dus keer terug (voor romeo aaah skaaan xD)
                return;
            }
        }
    }

    private static void addProduction(Planning p) { // van hetzelfde product meer maken, geen setup nodig
        // random
        Random random = new Random();
        int randomDay = random.nextInt(p.getNumberOfDays());
        int randomBlock = random.nextInt(p.getDay(randomDay).getBlocks().size());
        int randMachineInt = random.nextInt(p.getMachines().size());
        Machine randMachine = p.getMachines().get(randMachineInt);

        // zoek idle blok
        while (true) {
            String ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine).toString();
            if (ms.equals("IDLE")) {
                Item previousItem = randMachine.getInitialSetup();
                p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(previousItem));
            }
        }

        // TODO: controle nieuwe nightshift
    }

    private static void changeProduction(Planning p) { // nieuwe productie op machine starten
        // random
        Random random = new Random();
        int randomDay = random.nextInt(p.getNumberOfDays());
        int randomBlock = random.nextInt(p.getDay(randomDay).getNumberOfBlocksPerDay());
        int randMachineInt = random.nextInt(p.getMachines().size());
        Machine randMachine = p.getMachines().get(randMachineInt);

        // zoek idle blok
        while (true) {
            String ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine).toString();
            if (ms.equals("IDLE")) {
                Item previousItem = randMachine.getInitialSetup();
                // TODO: eerst nog setup, dan nieuw item
                p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(previousItem));
            }
        }

        // TODO: controle nieuwe nightshift
    }

    private static void removeProduction(Planning p) {
        // random
        Random random = new Random();
        while (true) {
            int randomDay = random.nextInt(p.getNumberOfDays());
            int randomBlock = random.nextInt(p.getDay(randomDay).getNumberOfBlocksPerDay());
            int randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);

            String s = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine).toString();
            if (s.contains("I_")) {
                p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Idle());
                return;
            }
            // TODO: controle voor eventuele overbodige setup te verwijderen?
        }
    }
}
