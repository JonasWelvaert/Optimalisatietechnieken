import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.machinestate.*;
import java.util.Random;

public class Solver {
	public static final int SIMULATED_ANEALING = 100;
	public static double SIMULATED_ANEALING_TEMPERATURE = 1000;
	public static double SIMULATED_ANEALING_COOLINGFACTOR = 0.995;

    public static Planning localSearch(Planning optimizedPlanning) { // TODO Elke
        Random random = new Random();
        int randomInt = random.nextInt(4);
        switch (randomInt) {
            case 0:
                moveMaintenance(optimizedPlanning);
                break;
            case 1:
                moveProduction(optimizedPlanning);
                break;
            case 2:
                removeProduction(optimizedPlanning);
                break;
            case 3:
                addProduction(optimizedPlanning);
                break;

        }

        // TODO: hier al de cost wijzigen per wijziging

        return null;/*
         * dns × pn + to × po + SOM r∈V SOM i∈I (q i r × ci) + SOM d∈D (ud × ps) + dp ×
         * pp
         */
    }

    public static boolean checkFeasible(Planning planning) {
        //TODO Nick
        return checkProductionConstraints(planning);
    }

    private static int evaluate(Planning planning) {
        return 0;
    }

    public static Planning optimize(long seed, long timeLimit, Planning initialPlanning) {

        Planning optimizedPlanning = new Planning(initialPlanning);
        //int previousCost = initialPlanning.getCost();

        int cost;
        int solution = 0;
        int teller = 0;
        while (teller < 1000) {
        	Planning nextPlanning;
        	do {
        		nextPlanning = localSearch(optimizedPlanning);
        	}while(!checkFeasible(nextPlanning));
            if (optimizedPlanning.getCost() > nextPlanning.getCost()) {
                optimizedPlanning = nextPlanning;
                teller = 0;
            } else {
                teller++;
            }
        }
        boolean feasible = checkFeasible(optimizedPlanning);
        if (feasible) {
            cost = evaluate(optimizedPlanning);
        }

        return optimizedPlanning;
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
    	

        if (planning.getDay(k).hasNightShift()) {	//TODO nick verwijderen
            // check that the night shift blocks are consecutive
            if ((i > Day.indexOfBlockS) && i < (Day.getNumberOfBlocksPerDay() - 1)) {
                if (!isConsecutiveInOvertime(i, k, planning)) return false;
            }

            // check that the minimum amount of consecutive days with night shift is fulfilled
            if (k > (Planning.getMinConsecutiveDaysWithNightShift() - planning.getPastConsecutiveDaysWithNightShift())) {
                for (int l = k; l < (k + Planning.getMinConsecutiveDaysWithNightShift()); l++) {
                    if (!planning.getDay(l).hasNightShift()) {
                        return false;
                    }
                }
            }
        } else {
            // check that the overtime blocks are consecutive
            if (i > Day.indexOfBlockS && i < Day.indexOfBlockO) {
                if (!isConsecutiveInOvertime(i, k, planning)) return false;
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
            if (planning.getDay(k).getBlock(i + 1).getMachineState(m) instanceof Production || planning.getDay(k).getBlock(i + 1).getMachineState(m) instanceof SmallSetup) {
                if (!(planning.getDay(k).getBlock(i).getMachineState(m) instanceof Production || planning.getDay(k).getBlock(i).getMachineState(m) instanceof SmallSetup)) {
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
        //TODO Nick
        for (Machine m : planning.getMachines()) {
            int init = m.getInitialDaysPastWithoutMaintenance();
            int max = m.getMaxDaysWithoutMaintenance();
            int now = max - init;
            int teller = 0;
            if (k < now) {
                for (int i = 0; i < Day.getNumberOfBlocksPerDay(); i++) {
                    if (planning.getDay(k).getBlock(i).isInMaintenance()){
                        if( ! isConsecutiveMaintenance(planning.getDay(k), planning)) return false;
                    } else {
                        return false;
                    }
                }
            }
        }
        return true;

    }

    private static boolean isConsecutiveMaintenance(Day day,Planning planning){
        //TODO Nick
        return true;
    }

    private static void moveMaintenance(Planning planning) {
        // random
        Planning newPlanning = new Planning (planning);
        Random random = new Random();
        int randomDay = random.nextInt(newPlanning.getNumberOfDays());
        int randomBlock = random.nextInt(newPlanning.getDay(0).getIndexOfBlockL()-newPlanning.getDay(0).getIndexOfBlockE()) + newPlanning.getDay(0).getIndexOfBlockE(); // enkel overdag
        int randMachineInt = random.nextInt(newPlanning.getMachines().size());
        Machine randMachine = newPlanning.getMachines().get(randMachineInt);

        for (int day = 0; day < newPlanning.getDays().size(); day++) {
            // moet tussen block E, en nog voor block L, maar maintenance begint sws vragen want moet afgeraken binnen block E en block L
            // dus we zoeken van block E tot Block L - hoelang t duurt voor maintenance
            for (int block = newPlanning.getDay(day).getIndexOfBlockE(); block < newPlanning.getDay(day).getIndexOfBlockL()-newPlanning.getMachines().get(0).getMaintenanceDurationInBlocks(); block++) {
                for (int machine = 0; machine < newPlanning.getMachines().size(); machine ++) {
                    Machine m = newPlanning.getMachines().get(machine);
                    String msString = newPlanning.getDay(day).getBlock(block).getMachineState(m).toString();
                    if (msString.equals("M") ) {
                        // verplaats volledige maintenance sequence
                        for (int i = 0; i < newPlanning.getMachines().get(machine).getMaintenanceDurationInBlocks(); i++) {
                            // get de machinestates
                            Maintenance maintenance = (Maintenance) newPlanning.getDay(day).getBlock(block+i).getMachineState(m);
                            MachineState ms = newPlanning.getDay(randomDay).getBlock(randomBlock+i).getMachineState(randMachine);

                            // wissel de machinestates
                            newPlanning.getDay(randomDay).getBlock(randomBlock+i).setMachineState(randMachine, maintenance);
                            newPlanning.getDay(day).getBlock(block+i).setMachineState(m, ms);
                        }
                        // klaar dus keer terug (voor romeo aaah skaaan xD)
                        return;
                    }
                }
            }
        }
    }

    private static void addProduction(Planning optimizedPlanning) {
    }

    private static void removeProduction(Planning optimizedPlanning) {
    }

    private static void moveProduction(Planning optimizedPlanning) {
    }

}
