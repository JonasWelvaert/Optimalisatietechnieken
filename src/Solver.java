import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.machinestate.*;

public class Solver {


    public static Planning localSearch(Planning optimizedPlanning) {
        // TODO Elke
        // some localsearch thing.

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
            Planning newPlanning = localSearch(optimizedPlanning);
            if (optimizedPlanning.getCost() > newPlanning.getCost()) {
                optimizedPlanning = newPlanning;
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


        if (planning.getDay(k).hasNightShift()) {
            // check that the night shift blocks are consecutive
            if ((i > Day.indexOfBlockS) && i < (Day.getNumberOfBlocksPerDay() - 1)) {
                if (!isConsecutive(i, k, planning)) return false;
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
                if (!isConsecutive(i, k, planning)) return false;
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

    private static boolean isConsecutive(int i, int k, Planning planning) {

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

}
