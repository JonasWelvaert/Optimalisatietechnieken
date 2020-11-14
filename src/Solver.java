import model.*;
import model.machinestate.*;

import java.util.*;

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

        int teller1 = 0;

        if (!isConsecutiveInNightShiftsPast(planning)) {
            return false;
        }

        for (int d = 0; d < Planning.getNumberOfDays(); d++) {

            List<Integer> setupTypes = new ArrayList<>();
            Map<Integer, Integer> setupMap = new HashMap<>();

            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {

                if (!checkNighShiftBlocksConstraints(b, d, planning)) {
                    return false;
                }

                if (!checkOvertimeConstraints(teller1, b, d, planning)) {
                    return false;
                }

                int parallelTeller = 0;

                for (Machine m : planning.getMachines()) {

                    MachineState state = planning.getDay(d).getBlock(b).getMachineState(m);

                    // adding setup types to a list -> duplicates are allowed!
                    // the list will be used for counting the occurrences of a setup type
                    if (state instanceof Setup) {
                        Item i1 = ((Setup) state).getFrom();
                        Item i2 = ((Setup) state).getTo();
                        setupTypes.add(i1.getId() + i2.getId());
                        setupMap.put(i1.getId() + i2.getId(), i1.getLengthSetup(i2));
                    }

                    if (!checkParallelConstraints(m, parallelTeller, b, d, planning)) {
                        return false;
                    }

                    if (d < Planning.getNumberOfDays() && b < Day.getNumberOfBlocksPerDay() - 1) {
                        if (!checkProductionConstraints(m, b, d, planning)) {
                            return false;
                        }
                    }
                }

            }
            if (!checkStockConstraints(planning.getDay(d), planning)) {
                return false;
            }

            if (!checkSetupTypeConstraint(setupTypes, setupMap)) {
                return false;
            }

        }

        for (Machine m : planning.getMachines()) {
            for (int d = 0; d < Planning.getNumberOfDays(); d++) {
                if (!checkChangeOverAndMaintenanceBoundaryConstraints(d, m, planning)) {
                    return false;
                }

                if (!checkMaintenanceConstraints(d, m, planning)) {
                    return false;
                }
            }
        }

        return true;
    }

    // check if the minimum amount of consecutive night shifts is fulfilled when their are past consecutive days
    private static boolean isConsecutiveInNightShiftsPast(Planning planning) {
        if (planning.getPastConsecutiveDaysWithNightShift() > 0) {
            for (int i = 0; i < (Planning.getMinConsecutiveDaysWithNightShift() - planning.getPastConsecutiveDaysWithNightShift()); i++) {
                if (!planning.getDay(i).hasNightShift()) {
                    return false;
                }
            }
        }
        return true;
    }

    // check that their is no maintenance/large setup between 0-b_e and b_l-b_n
    private static boolean checkNighShiftBlocksConstraints(int b, int d, Planning planning) {
        if (b < Day.indexOfBlockE || b > Day.indexOfBlockL) {
            for (Machine m : planning.getMachines()) {
                if (planning.getDay(d).getBlock(b).getMachineState(m) instanceof LargeSetup || planning.getDay(d).getBlock(b).getMachineState(m) instanceof Maintenance) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkOvertimeConstraints(int teller, int b, int d, Planning planning) {

        if (planning.getDay(d).hasNightShift()) {

            teller++;
            // check that the minimum amount of consecutive days with night shift is
            // fulfilled
            if (d > (Planning.getMinConsecutiveDaysWithNightShift()
                    - planning.getPastConsecutiveDaysWithNightShift())) {
                for (int l = d; l < (d + Planning.getMinConsecutiveDaysWithNightShift()); l++) {
                    if (!planning.getDay(l).hasNightShift()) {
                        return false;
                    }
                }
            }
        } else {

            // check that the night shift days are consecutive
            if (teller > 0 && teller < Planning.getMinConsecutiveDaysWithNightShift()) {
                return false;
            } else {
                teller = 0;
            }

            // check that the overtime blocks are consecutive
            if (b > Day.indexOfBlockS && b < Day.indexOfBlockO && !isConsecutiveInOvertime(b, d, planning)) {
                return false;
            }

            // check that their is no overtime after block b_o for all machines
            if (b > Day.indexOfBlockO) {
                for (Machine m : planning.getMachines())
                    if (!(planning.getDay(d).getBlock(b).getMachineState(m) instanceof Idle)) {
                        return false;
                    }
            }
        }
        return true;
    }

    private static boolean isConsecutiveInOvertime(int b, int d, Planning planning) {

        for (Machine m : planning.getMachines())
            if (planning.getDay(d).getBlock(b + 1).getMachineState(m) instanceof Production
                    || planning.getDay(d).getBlock(b + 1).getMachineState(m) instanceof SmallSetup) {
                if (!(planning.getDay(d).getBlock(b).getMachineState(m) instanceof Production
                        || planning.getDay(d).getBlock(b).getMachineState(m) instanceof SmallSetup)) {
                    return false;
                }
            }
        return true;
    }

    // check that only 1 maintenance/large setup is scheduled at any block of a day -> no parallel maintenance/large setup
    private static boolean checkParallelConstraints(Machine m, int parallelTeller, int b, int d, Planning planning) {
        MachineState state = planning.getDay(d).getBlock(b).getMachineState(m);

        if (state instanceof LargeSetup || state instanceof Maintenance) {
            parallelTeller++;
            if (parallelTeller > 1) {
                return false;
            }
        }
        return true;
    }

    // check that for each day only 1 setup of a certain type is scheduled
    private static boolean checkSetupTypeConstraint(List<Integer> setupTypes, Map<Integer, Integer> setupMap) {

        Map<Integer, Integer> hm = new HashMap<>();

        // setup type is the key -> only 1 value possible
        // key = setup type
        // value = number of occurrences of set up types
        for (int setUpTime : setupTypes) {
            Integer j = hm.get(setUpTime);
            hm.put(setUpTime, (j == null) ? 1 : j + 1);
        }

        // if number of set up types > set up time -> return false
        for (Map.Entry<Integer, Integer> setupType : hm.entrySet()) {
            int setupTime = setupMap.get(setupType.getKey());
            int setupOccurrences = setupType.getValue();
            if (setupOccurrences > setupTime) {
                return false;
            }
        }

        return true;
    }

    // check that the production of an item is only possible after the right configuration
    // only possible if previous block is changeover to item or previous block is production of same item
    private static boolean checkProductionConstraints(Machine m, int b, int d, Planning planning) {

        MachineState state1 = planning.getDay(d).getBlock(b).getMachineState(m);
        MachineState state2 = planning.getDay(d).getBlock(b + 1).getMachineState(m);
        if (state1 instanceof Production && state2 instanceof Production) {
            if (((Production) state1).getItem().getId() != ((Production) state2).getItem().getId()) {
                return false;
            }
        }
        if (state1 instanceof Setup && state2 instanceof Production) {
            if (((Setup) state1).getTo().getId() != ((Production) state2).getItem().getId()) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkStockConstraints(Day day, Planning planning) {

        for (Item i : planning.getStock().getItems()) {

            // check that the stock level on day d is less than the max allowed of stock of an item
            if (i.getStockAmount(day) > i.getMaxAllowedInStock()) {
                return false;
            }

            // check that the stock level on day d is 0 when the stock level is below the minimum amount
            if (i.getStockAmount(day) < i.getMinAllowedInStock() && i.getStockAmount(day) != 0) {
                return false;
            }

        }

        return true;
    }


    // check that the minimum amount of maintenances are scheduled in the time horizon
    private static boolean checkMaintenanceConstraints(int d, Machine m, Planning planning) {

        int init = m.getInitialDaysPastWithoutMaintenance();
        int max = m.getMaxDaysWithoutMaintenance();
        int now = max - init;

        int maintenanceTeller = 0;
        if (d - now == 0 || (d - now) % (max + 1) == 0) {
            for (Block b : planning.getDay(d)) {
                if (b.getMachineState(m) instanceof Maintenance) {
                    maintenanceTeller++;
                }
            }
            if (maintenanceTeller == 0) {
                return false;
            }
        }

        return true;

    }

    // check that no production is done in between blocks of changeover and blocks of maintenance
    public static boolean checkChangeOverAndMaintenanceBoundaryConstraints(int d, Machine m, Planning planning) {

        int setupTeller = 0;
        int maintenanceTeller = 0;
        int setupTime = 0;
        int maintenanceTime = 0;
        Item i1 = null;
        Item i2 = null;
        for (int j = 0; j < Day.getNumberOfBlocksPerDay(); j++) {
            MachineState state = planning.getDay(d).getBlock(j).getMachineState(m);

            // maintenance/idle is allowed in between setup blocks and setup/idle is allowed in between maintenance blocks
            if (state instanceof Setup || state instanceof Idle || state instanceof Maintenance) {

                // machine state is in setup configuration
                if (state instanceof Setup) {
                    Setup setup = (Setup) planning.getDay(d).getBlock(j).getMachineState(m);

                    // check if the setup is still the same (S1_2 -> S2_3)
                    if (i1 == null || !(i1.getId() == (setup.getFrom().getId()) || !(i2.getId() == setup.getFrom().getId()))) {

                        if (setupTeller > 0 && setupTeller <= setupTime) {
                            return false;
                        }

                        // get initial setup
                        i1 = setup.getFrom();
                        i2 = setup.getTo();
                        setupTime = i1.getLengthSetup(i2);
                        setupTeller++;

                    } else {
                        // setup is still the same -> teller increasing
                        setupTeller++;

                        // setup time is exceeded -> restart
                        if (setupTeller == setupTime) {
                            setupTeller = 0;
                            setupTime = 0;
                            i1 = null;
                            i2 = null;
                        }
                    }
                    // machine state is in maintenance configuration
                } else if (state instanceof Maintenance) {

                    Maintenance maintenance = (Maintenance) planning.getDay(d).getBlock(j).getMachineState(m);

                    // first occurrence of maintenance
                    if (maintenanceTeller == 0) {
                        maintenanceTime = m.getMaintenanceDurationInBlocks();
                    }

                    maintenanceTeller++;

                    // stop counting if maintenance time is exceeded
                    if (maintenanceTeller == maintenanceTime) {
                        maintenanceTeller = 0;
                        maintenanceTime = 0;
                    }
                }

                // block is in production -> check if the block is not between setup's or maintenance's
            } else if ((setupTeller > 0 && setupTeller <= setupTime) || (maintenanceTeller > 0 && maintenanceTeller <= maintenanceTime)) {
                return false;

                // restart
            } else {
                setupTeller = 0;
                setupTime = 0;
                maintenanceTeller = 0;
                maintenanceTime = 0;

            }
        }

        return true;

    }

    private static void moveMaintenance(Planning p) {
        Random random = new Random();

        while (true) {
            //van
            int randomDay1 = random.nextInt(p.getNumberOfDays());
            int randomBlock1 = random.nextInt(p.getDay(0).getIndexOfBlockL() - p.getDay(0).getIndexOfBlockE()) + p.getDay(0).getIndexOfBlockE(); // enkel overdag
            int randMachineInt1 = random.nextInt(p.getMachines().size());
            Machine randMachine1 = p.getMachines().get(randMachineInt1);
            MachineState ms1 = p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1);
            String msString1 = ms1.toString();

            // naar
            int randomDay2 = random.nextInt(p.getNumberOfDays());
            int randomBlock2 = random.nextInt(p.getDay(0).getIndexOfBlockL() - p.getDay(0).getIndexOfBlockE()) + p.getDay(0).getIndexOfBlockE(); // enkel overdag
            int randMachineInt2 = random.nextInt(p.getMachines().size());
            Machine randMachine2 = p.getMachines().get(randMachineInt2);

            // als 1 = maintenance => verplaats naar 2
            if (msString1.equals("M")) {
                // verplaats volledige maintenance sequence
                for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                    // zet op idle
                    p.getDay(randomDay1).getBlock(randomBlock1 + i).setMachineState(randMachine1, new Idle());

                    // zet op maintenance
                    p.getDay(randomDay2).getBlock(randomBlock2 + i).setMachineState(randMachine2, ms1);
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
