import model.*;
import model.machinestate.*;

import java.util.*;

public class Solver {
    public static final int SIMULATED_ANEALING = 100;
    private double SATemperature = 1000;
    private double SACoolingFactor = 0.995;
    private int mode;
    private static Random random = new Random();
    private static int maxAantalDagenTussenVerlengingNightshift = 3; //dit is een variabele die kan gewijzigd worden adhv het algoritme

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

    public static Planning localSearch(Planning optimizedPlanning) {
        int randomInt = random.nextInt(16);
        if (randomInt == 1)  // willen niet constant maintenance zitten verplaatsen
            moveMaintenance(optimizedPlanning);
        else if (randomInt < 5)
            addProduction(optimizedPlanning, null);
        else if (randomInt < 9)
            removeProduction(optimizedPlanning);
        else if (randomInt < 13)
            changeProduction(optimizedPlanning, null);
        else
            moveProduction(optimizedPlanning);

        // TODO: hier al de cost wijzigen per wijziging

        return optimizedPlanning;/*
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
        while (true) {
            //van
            int randomDay1 = random.nextInt(Planning.getNumberOfDays());
            int randomBlock1 = random.nextInt(p.getDay(0).getIndexOfBlockL() - p.getDay(0).getIndexOfBlockE()) + p.getDay(0).getIndexOfBlockE(); // enkel overdag
            int randMachineInt1 = random.nextInt(p.getMachines().size());
            Machine randMachine1 = p.getMachines().get(randMachineInt1);
            MachineState ms1 = p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1);
            String msString1 = ms1.toString();

            // naar
            int randomDay2 = random.nextInt(Planning.getNumberOfDays());
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

    private static void addProduction(Planning p, Item item) { // van hetzelfde product meer maken, geen setup nodig
        // random
        int randomDay = random.nextInt(Planning.getNumberOfDays());
        int randomBlock = random.nextInt(p.getDay(randomDay).getNumberOfBlocksPerDay());
        int randMachineInt = random.nextInt(p.getMachines().size());
        Machine randMachine = p.getMachines().get(randMachineInt);

        // zoek idle blok
        boolean stop = false;
        while (stop) {
            String ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine).toString();
            if (ms.equals("IDLE")) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
                if (item == null) {
                    p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(previousItem));
                    stop = true;
                } else if (previousItem.getId() == item.getId()) {
                    p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(item));
                    stop = true;
                } else {
                    changeProduction(p, item);
                    stop = true;
                }
            }
        }
        controlNewNightShift(p, randomDay, randomBlock);
    }

    private static void changeProduction(Planning p, Item item) { // nieuwe productie op machine starten
        // random
        int randomDay = random.nextInt(Planning.getNumberOfDays());
        int randomBlock = random.nextInt(p.getDay(randomDay).getNumberOfBlocksPerDay());
        int randMachineInt = random.nextInt(p.getMachines().size());
        Machine randMachine = p.getMachines().get(randMachineInt);
        Block b = p.getDay(randomDay).getBlock(randomBlock);

        // zoek idle blok
        Item newItem;
        while (true) {
            String ms = b.getMachineState(randMachine).toString();
            if (ms.equals("IDLE")) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
                if (item == null) {
                    int aantalItems = previousItem.getAantalItems();
                    newItem = p.getItemById(random.nextInt(aantalItems)); // ofwel random item uit lijst
                } else
                    newItem = item; // ofwel voor methode moveItem

                boolean possibleSetup = setupNewItem(previousItem, newItem, randomDay, randomBlock, randMachine, p);
                if (possibleSetup)
                    b.setMachineState(randMachine, new Production(newItem));
                else
                    return;

                break;
            }
        }
        controlNewNightShift(p, randomDay, randomBlock);
    }

    // TODO: setup proberen toevoegen en terug geven of het mogelijk is
    private static boolean setupNewItem(Item previousItem, Item newItem, int day, int block, Machine machine, Planning p) {
        // check genoeg tijd voor setup + overdag
        int tijdSetup = previousItem.getLengthSetup(newItem);
        Item nextItem = machine.getNextItem(p, day, block); // fout, niet nextitem maar nextmachinestate die niet idle is
        int tijdBeschikbaar = 0;

        // TODO: kijken ofdat er niet overbodige setup komt hierdoor
        return false;
    }

    private static Item removeProduction(Planning p) {
        while (true) {
            int randomDay = random.nextInt(Planning.getNumberOfDays());
            int randomBlock = random.nextInt(p.getDay(randomDay).getNumberOfBlocksPerDay());
            int randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);
            Block b = p.getDay(randomDay).getBlock(randomBlock);

            String s = b.getMachineState(randMachine).toString();
            if (s.contains("I_")) {
                b.setMachineState(randMachine, new Idle());
                Production prod = (Production) b.getMachineState(randMachine);
                return prod.getItem();
            }
            // TODO: controle voor eventuele overbodige setup te verwijderen?
        }
    }

    private static int closestNightshift(Planning p, String when, int day) {
        if (when.equals("before")) {
            int lastNightShiftDay = -1;
            for (int i = 0; i < p.getDays().size(); i++) {
                if (p.getDay(i).hasNightShift() && i < day) { // controleren tot als laatste dag van nightshiftreeks
                    lastNightShiftDay = i;
                } else {
                    break;
                }
            }
            return lastNightShiftDay;
        } else if (when.equals("after")) {
            int firstNightShiftDay = -1;
            for (int i = day+1; i < p.getDays().size(); i++) {
                if (p.getDay(i).hasNightShift()) { // controleren tot eerste dag van nieuwe nightshift reeks
                    firstNightShiftDay = i;
                    break;
                }
            }
            return firstNightShiftDay;
        }
        return -1;
    }

    private static void controlNewNightShift(Planning p, int randomDay, int randomBlock) {
        Day d = p.getDay(randomDay);
        if (!d.hasNightShift()) { // als er al nightshift is valt er niks te controleren
            if (randomBlock>d.getIndexOfBlockO() ) { // niet overtime, wel nachtshift
                int nightshiftBefore = closestNightshift(p, "before", randomDay);
                int nightshiftAfter = closestNightshift(p, "after", randomDay);
                // als nightshift niet lang geleden => beter verlengen, dan nieuwe starten
                if (nightshiftBefore < nightshiftAfter) { // als dichtste nightshift ervoor ligt
                    if (nightshiftBefore <= maxAantalDagenTussenVerlengingNightshift) {
                        // alle dagen ervoor ook nighshift maken
                        for (int i = randomDay; i > randomDay - nightshiftBefore; i--) {
                            p.getDay(i).setNightShift(true);
                        }
                    } else {
                        // nieuwe nightshift reeks
                        int amountOfNightShifts = Planning.getMinConsecutiveDaysWithNightShift();
                        for (int i = randomDay; i < p.getDays().size(); i++) {
                            if (amountOfNightShifts == 0) {
                                break;
                            } else {
                                p.getDay(i).setNightShift(true);
                                amountOfNightShifts--;
                            }
                        }
                    }
                } else { // als dichtste nighshift erna ligt of ze zijn gelijk
                    if (nightshiftAfter <= maxAantalDagenTussenVerlengingNightshift) {
                        // alle dagen erna ook nightshift maken
                        for (int i = randomDay; i < randomDay + nightshiftAfter; i++) {
                            p.getDay(i).setNightShift(true);
                        }
                    } else {
                        // nieuwe nightshift reeks
                        int amountOfNightShifts = Planning.getMinConsecutiveDaysWithNightShift();
                        for (int i = randomDay; i < p.getDays().size(); i++) {
                            if (amountOfNightShifts == 0) {
                                break;
                            } else {
                                p.getDay(i).setNightShift(true);
                                amountOfNightShifts--;
                            }
                        }
                    }
                }
            }
        }
    }

    private static void moveProduction(Planning optimizedPlanning) {
        Item removedItem = removeProduction(optimizedPlanning);
        addProduction(optimizedPlanning, removedItem);
    }
}
