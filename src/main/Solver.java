package main;

import feasibilitychecker.FeasibiltyChecker;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.LargeSetup;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Solver {
    private static final Logger logger = Logger.getLogger(Solver.class.getName());
    public static final int SIMULATED_ANEALING = 100;
    private double SATemperature = 1000;
    private double SACoolingFactor = 0.995;
    private int mode;
    private static final int MAX_REMOVE_PROD_TRIES = 1000;
    private static final int MAX_ADD_PROD_TRIES = 1000;
    private static final int MAX_CHANGE_PROD_TRIES = 1000;
    private static final int MAX_MAINTENANCE_TRIES = 1000;
    private static final int MAX_NEWSETUP_TRIES = 1000;
    private static final int MAX_MOVEITEM_TRIES = 1000;
    private static final int MAX_ADDSHIPPINGPROD_TRIES = 1000;
    private static final int MAX_NEW_SHIPPINGDAY_TRIES = 1000;
    private static final Random random = new Random();
    private static final int maxAmountDaysBetweenExtendingNightshift = 3; // dit is een variabele die kan gewijzigd
    // worden adhv het algoritme
    private static final int maxLengthNewBlocks = 10;
    private static final int maxLengthRemovingBlocks = 10;
    private static final int NotFound = 99999;
    private static int localSearchUpperBound = 99999;

    private final FeasibiltyChecker feasibiltyChecker;

    public Solver(int mode, FeasibiltyChecker feasibiltyChecker) {
        logger.setLevel(Level.OFF);
        this.feasibiltyChecker = feasibiltyChecker;

        if (mode == 100) {
            this.mode = mode;
        } else {
            logger.warning("Optimize mode " + this.mode + " not found");
            throw new RuntimeException("Optimize mode not found in main.Solver constructor");
        }
    }

    public void setSimulatedAnealingFactors(double SATemperature, double SACoolingFactor) {
        this.SACoolingFactor = SACoolingFactor;
        this.SATemperature = SATemperature;
    }

    public Planning optimize(Planning initialPlanning) throws IOException {
        if (mode == SIMULATED_ANEALING) {
            return optimizeUsingSimulatedAnealing(initialPlanning, SATemperature, SACoolingFactor);
        }
        // hier kunnen andere optimalisaties toegevoegd worden als deze niet goed
        // blijkt.
        throw new RuntimeException("Optimize mode not found in main.Solver.optimize()");
    }

    private Planning optimizeUsingSimulatedAnealing(Planning initialPlanning, double temperature, double coolingFactor)
            throws IOException {
        Planning current = new Planning(initialPlanning);
        Planning best = initialPlanning;
        int stockRiseLevel = 0;


        for (double t = temperature; t > 1; t *= coolingFactor) {
//            logger.info("t=" + t);

            Planning neighbor;
            do {
                neighbor = new Planning(current);
                localSearch(neighbor);
            } while (!feasibiltyChecker.checkFeasible(neighbor));

            double neighborCost = neighbor.getTotalCost();
            double currentCost = current.getTotalCost();

            double probability;
            if (neighborCost < currentCost) {
                probability = 1;
            } else {
                probability = Math.exp((currentCost - neighborCost) / t);
            }
            // ACCEPT SOMETIMES EVEN IF COST IS WORSE
            if (Math.random() < probability) {
                current = new Planning(neighbor);
            }
            // ALSO ACCEPT SOLUTION IF STOKE ROSE MORE THAN stockRiseLevel
            else if (neighbor.getStockAmount() - stockRiseLevel > current.getStockAmount()) {
                current = new Planning(neighbor);
            }
            // OVERWRITE BEST IF COST IS IMPROVED
            if (current.getTotalCost() < best.getTotalCost()) {
                best = new Planning(current);
            }

        }
        System.out.println(feasibiltyChecker.getEc().toString());
        return best;
    }

    public static Planning localSearch(Planning optimizedPlanning) throws IOException {
        /* ------------------------ ENKEL GETALLEN AANPASSEN VOOR GEWICHTEN AAN TE PASSEN ------------------------ */

        int randomInt = random.nextInt(localSearchUpperBound);
        int teller = 0;
        if (randomInt < (teller += 1)) {
            moveMaintenance(optimizedPlanning);

        } else if (randomInt < (teller += 50)) {
            addSingleRandomProduction(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            addProductionForShipping(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            changeProduction(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            removeProduction(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            moveProduction(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            moveShippingDay(optimizedPlanning);
        } else if (randomInt < (teller += 0)) {
            tryToPlanShippingDay(optimizedPlanning);
        } else {
            logger.info("The upperbound for the localsearch is set.");
            localSearchUpperBound = teller;
        }

        tryToPlanShippingDay(optimizedPlanning);


        optimizedPlanning.calculateAllCosts();
        return optimizedPlanning;
    }


    private static void moveMaintenance(Planning p) {
        int count = 0;
        while (count < MAX_MAINTENANCE_TRIES) {
            // van
            int randomDay1 = random.nextInt(Planning.getNumberOfDays());
            int randomBlock1 = random.nextInt(Day.getIndexOfBlockL() - Day.getIndexOfBlockE()) + Day.getIndexOfBlockE(); // enkel
            // overdag
            int randMachineInt1 = random.nextInt(p.getMachines().size());
            Machine randMachine1 = p.getMachines().get(randMachineInt1);
            MachineState ms1 = p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1);

            // naar
            int randomDay2 = random.nextInt(Planning.getNumberOfDays());
            int randomBlock2 = random.nextInt(Day.getIndexOfBlockL() - Day.getIndexOfBlockE()) + Day.getIndexOfBlockE(); // enkel
            // overdag
            int randMachineInt2 = random.nextInt(p.getMachines().size());
            Machine randMachine2 = p.getMachines().get(randMachineInt2);

            // als 1 = maintenance => verplaats naar 2
            if (p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1) instanceof Maintenance) {
                // kijken ofdat op de nieuwe plek geen setup of andere maintenance staat
                boolean verplaatsbaar = true;
                for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                    if (p.getDay(randomDay2).getBlock(randomBlock2 + i).getMachineState(randMachine1) instanceof Setup
                            || p.getDay(randomDay2).getBlock(randomBlock2 + i)
                            .getMachineState(randMachine1) instanceof Maintenance) {
                        verplaatsbaar = false;
                    }
                }

                // find first block of maintenance
                boolean firstBlockFound = false;
                int firstBlock = randomBlock1;
                while (!firstBlockFound) {
                    if (p.getDay(randomDay1).getBlock(firstBlock - 1)
                            .getMachineState(randMachine1) instanceof Maintenance) {
                        firstBlock--;
                    } else {
                        firstBlockFound = true;
                    }
                }

                // verplaats volledige maintenance sequence
                if (verplaatsbaar) {
                    for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                        // zet op maintenance op idle
                        p.getDay(randomDay1).getBlock(firstBlock + i).setMachineState(randMachine1, new Idle());

                        // zet op nieuwe blok op maintenance
                        p.getDay(randomDay2).getBlock(randomBlock2 + i).setMachineState(randMachine2, ms1);

                    }
                    return;
                }
                count++;
            }
        }
    }

    private static void addSingleRandomProduction(Planning p) { // van hetzelfde product meer maken, geen setup nodig voor het item,
        // mss wel na het item
        int count = 0;

        // random
        int randomDay = NotFound;
        int randomBlock = NotFound;
        int randMachineInt = NotFound;

        // zoek idle blok
        boolean stop = false;
        while (!stop && count < MAX_ADD_PROD_TRIES) {

            // random
            randomDay = random.nextInt(Planning.getNumberOfDays());
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);

            if (p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
//                if (setupAfterNewItem(previousItem, randomDay, randomBlock, randMachine, p, 1)) {
                p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine,
                        new Production(previousItem));
                previousItem.updateItem(p.getDay(randomDay), randMachine);
                stop = true;
//                }
            }
            count++;
        }
        controlNewNightShift(p, randomDay, randomBlock);

    }

    private static void changeProduction(Planning p) { // nieuwe productie op machine starten
        int count = 0;

        // random
        int randomDay = NotFound;
        int randomBlock = NotFound;
        int randMachineInt = NotFound;

        // zoek idle blok
        Item newItem = null;
        while (count < MAX_CHANGE_PROD_TRIES) {

            // random
            randomDay = random.nextInt(Planning.getNumberOfDays());
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);
            Block b = p.getDay(randomDay).getBlock(randomBlock);

            if (b.getMachineState(randMachine) instanceof Idle) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
                int aantalItems = p.getStock().getNrOfDifferentItems();
                boolean newItemNotPrevItem = false;
                // random item uit lijst die niet vorige item is
                while (!newItemNotPrevItem) {
                    newItem = p.getStock().getItem(random.nextInt(aantalItems));
                    if (newItem.getId() != previousItem.getId()) {
                        newItemNotPrevItem = true;
                    }
                }

                if (setupBeforeNewItem(previousItem, newItem, randomDay, randomBlock, randMachine, p, 1, false)) {
                    b.setMachineState(randMachine, new Production(newItem));
                    newItem.updateItem(p.getDay(randomDay), randMachine);
                    controlNewNightShift(p, randomDay, randomBlock);
                    return;
                }
            }
            count++;
        }
    }

    /*
     * true als niet nodig, of als al in orde false als niet mogelijk
     */
    private static boolean setupAfterNewItem(Item newItem, int day, int block, Machine machine, Planning p,
                                             int sizeProductions) {
//        int currentBlock = block;
//        int day = day;

        /* CONTROLE OP FEIT DAT JE SETUP KAN DOEN NA IN TE PLANNEN PRODUCTIE */
        if (sizeProductions == 1) {
            // Als laatste blok is ...
            if (block == Day.getNumberOfBlocksPerDay() - 1) {
                // Als laatste dag van planning is
                if (day == Planning.getNumberOfDays() - 1) {
                    return false;
                } else {
                    // Kijk naar eerste blok van volgende dag
                    block = 0;
                    day++;
                }

            }
            // kijk naar volgende blok
            else {
                block++;
            }
        } else {
            block = block + sizeProductions;
            // Als laatst ingeplande blok komt na het einde van deze dag ...
            if (block >= Day.getNumberOfBlocksPerDay()) { // controle op voorlaatste blok.
                if (day == Planning.getNumberOfDays() - 1) {
                    return false;
                } else {
                    block = block - Day.getNumberOfBlocksPerDay() + 1; // TODO mss fout
                    day++;
                }
            }
            // Laatst ingeplande blok komt voor einde van de dag
            else {
                block++;
            }
        }

        int dayNextItem = 0;
        int blockNextItem = 0;

        Item nextItem = null;
        boolean foundItem = false;
        // get next production block
        for (int d = day; d < p.getDays().size(); d++) {
            for (int b = block; b < Day.getNumberOfBlocksPerDay(); b++) {

                if (p.getDay(d).getBlock(b).getMachineState(machine) instanceof Production) {
                    Production production = (Production) p.getDay(d).getBlock(b).getMachineState(machine);
                    nextItem = production.getItem();
                    dayNextItem = d;
                    blockNextItem = b;
                    foundItem = true;

                } else if (p.getDay(d).getBlock(b).getMachineState(machine) instanceof Setup) {
                    Setup setup = (Setup) p.getDay(d).getBlock(b).getMachineState(machine);
                    nextItem = setup.getFrom();
                    blockNextItem = b;
                    dayNextItem = d;
                    foundItem = true;
                }
                if (foundItem)
                    break;
            }
            block = 0;
            if (foundItem)
                break;
        }

        if (nextItem == null) {
//            Main.printOutputToConsole(p);
            return true;
        }

        return setupBeforeNewItem(newItem, nextItem, dayNextItem, blockNextItem, machine, p, sizeProductions, true);

    }

    private static Map<Integer, List<Block>> getSetupBeforeOptions(Item newItem, int day, int block, Machine machine, Planning p) {
        Item previousItem = machine.getPreviousItem(p, day, block);
        // CHECK IF ITEM TO ADD DOES NOT NEED A SETUP
        if (previousItem.equals(newItem)) return null;  // TODO check for null in calling part

        //INIT VARIABLES AFTER CHECK !!!
        Setup setupNeeded = previousItem.getSetupTo(newItem);
        int numberOfPredecessorDays = Planning.getNumberOfDays() - day;  //TODO indexOutOfBounds ?
        Map<Integer, List<Block>> setupBlocks = new HashMap<>();
        int acquiredTime = 0;
        int iteration = 0;

        // SET TIME WINDOWS IN WHICH SETUPS CAN HAPPEN
        int t0 = -1, t1, t2 = -1;
        t1 = block - 1;

        if (setupNeeded instanceof SmallSetup) {         // can happen anytime
            t0 = 0;
            t1 = block - 1;
            t2 = Day.getNumberOfBlocksPerDay() - 1;  //TODO indexOutOfBounds ?
        } else if (setupNeeded instanceof LargeSetup) {
            t0 = Day.getIndexOfBlockE();
            t1 = block - 1;
            t2 = Day.getIndexOfBlockL();
            if (t1 < t0) {
                iteration++; // BEGIN CHECK YESTERDAY (do not check for today)
                t1 = t2;
            }
        } else {
            return setupBlocks;
        }

        while (iteration < numberOfPredecessorDays) {
            List<Block> possibleBlocks = p.getDay(day - iteration).getBlocksBetweenInclusive(t0, t1);
            possibleBlocks.removeIf(b -> !(b.getMachineState(machine) instanceof Idle)); // remove blocks that are not Idle
            setupBlocks.put(day - iteration, possibleBlocks); //ADD possible blocks to solution
            acquiredTime += possibleBlocks.size();

            if (acquiredTime >= setupNeeded.getSetupTime()) {
                return setupBlocks;
            }
            t1 = t2;
            iteration++;
        }

        setupBlocks.clear();
        return setupBlocks; // als leeg: niet mogelijk
    }

    private static Map<Integer, List<Block>> getSetupAfterOptions(Item newItem, int day, int block, Machine machine, Planning p) {
        Item previousItem = machine.getPreviousItem(p, day, block);
        // CHECK IF ITEM TO ADD DOES NOT NEED A SETUP
        if (previousItem.equals(newItem)) return null;  // TODO check for null in calling part

        //INIT VARIABLES AFTER CHECK !!!
        Setup setupNeeded = previousItem.getSetupTo(newItem);
        int numberOfPredecessorDays = Planning.getNumberOfDays() - day;  //TODO indexOutOfBounds ?
        Map<Integer, List<Block>> setupBlocks = new HashMap<>();
        int acquiredTime = 0;
        int iteration = 0;

        // SET TIME WINDOWS IN WHICH SETUPS CAN HAPPEN
        int t0 = -1, t1, t2 = -1;
        t1 = block - 1;

        if (setupNeeded instanceof SmallSetup) {         // can happen anytime
            t0 = 0;
            t1 = block - 1;
            t2 = Day.getNumberOfBlocksPerDay() - 1;  //TODO indexOutOfBounds ?
        } else if (setupNeeded instanceof LargeSetup) {
            t0 = Day.getIndexOfBlockE();
            t1 = block - 1;
            t2 = Day.getIndexOfBlockL();
            if (t1 < t0) {
                iteration++; // BEGIN CHECK YESTERDAY (do not check for today)
                t1 = t2;
            }
        } else {
            return setupBlocks;
        }

        while (iteration < numberOfPredecessorDays) {
            List<Block> possibleBlocks = p.getDay(day - iteration).getBlocksBetweenInclusive(t0, t1);
            //TODO niet aan het werken op een copy !!!
            possibleBlocks.removeIf(b -> !(b.getMachineState(machine) instanceof Idle)); // remove blocks that are not Idle
            setupBlocks.put(day - iteration, possibleBlocks); //ADD possible blocks to solution
            acquiredTime += possibleBlocks.size();

            if (acquiredTime >= setupNeeded.getSetupTime()) {
                return setupBlocks;
            }
            t1 = t2;
            iteration++;
        }

        setupBlocks.clear();
        return setupBlocks; // als leeg: niet mogelijk
    }

    private static boolean setupBeforeNewItem(Item previousItem, Item newItem, int day, int block, Machine machine, Planning p, int sp, boolean afterAlreadyChecked) {
        if (!afterAlreadyChecked) {
            if (!setupAfterNewItem(newItem, day, block, machine, p, sp)) {
                return false;
            }
        }

        // controle niet hetzelfde item
        if (previousItem.getId() == newItem.getId()) {
            return true;
        }

        int currentBlock = block - 1; // TODO hier

        // check genoeg tijd voor setup + overdag
        int tijdSetup = previousItem.getSetupTimeTo(newItem);
        int tijdBeschikbaar = 0;
        int earliestMomentBlock = 0;

        int count = 0;

        // hoeveel tijd ervoor + controle na b_l
        while (count < MAX_NEWSETUP_TRIES) {
            // snachts is false
            if (currentBlock - count < Day.indexOfBlockE) {
                return false;
                // als ander product is false
            } else if (p.getDay(day).getBlock(currentBlock - count).getMachineState(machine) instanceof Production) {
                return false;
                // als iets in de weg ma nog nie opt einde = tijd terug op nul
            } else if (p.getDay(day).getBlock(currentBlock - count).getMachineState(machine) instanceof Maintenance) {
                tijdBeschikbaar = 0;
            } else if (p.getDay(day).getBlock(currentBlock - count).getMachineState(machine) instanceof Setup) {
                return false;
                // dubbele controle ofda het idle is
            } else if (p.getDay(day).getBlock(currentBlock - count).getMachineState(machine) instanceof Idle) {
                tijdBeschikbaar++;
                if (tijdBeschikbaar == tijdSetup) {
                    earliestMomentBlock = currentBlock - count;
                    break;
                }
            }
            count++;
        }

        // tijd genoeg?
        for (int i = 0; i < tijdSetup; i++) {
            if (previousItem.isLargeSetup(newItem)) { // large setup
                p.getDay(day).getBlock(earliestMomentBlock + i).setMachineState(machine, new LargeSetup(previousItem, newItem));
            } else { // small setup
                p.getDay(day).getBlock(earliestMomentBlock + i).setMachineState(machine, new SmallSetup(previousItem, newItem));
            }
        }

        return true;

    }

    private static Item removeProduction(Planning p) {
        int count = 0;
        while (count < MAX_REMOVE_PROD_TRIES) {
            int randomDay = random.nextInt(Planning.getNumberOfDays());
            int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            int randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);
            Block b = p.getDay(randomDay).getBlock(randomBlock);

            MachineState ms = b.getMachineState(randMachine);

            if (ms instanceof Production) {
                Production prod = (Production) b.getMachineState(randMachine);

                b.setMachineState(randMachine, new Idle()); // change returnen TODO romeo
                return prod.getItem();
            }
            count++;
            // TODO: Elke controle voor eventuele overbodige setup te verwijderen?
        }
        return null;
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
            for (int i = day + 1; i < p.getDays().size(); i++) {
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
            if (randomBlock > Day.getIndexOfBlockO()) { // niet overtime, wel nachtshift
                int nightshiftBefore = closestNightshift(p, "before", randomDay);
                int nightshiftAfter = closestNightshift(p, "after", randomDay);
                // als nightshift niet lang geleden => beter verlengen, dan nieuwe starten
                /*
                 * if (nightshiftAfter == -1 && nightshiftBefore == -1) { // nieuwe nightshift
                 * reeks int amountOfNightShifts =
                 * Planning.getMinConsecutiveDaysWithNightShift(); for (int i = randomDay; i <
                 * p.getDays().size(); i++) { if (amountOfNightShifts == 0) { break; } else {
                 * p.getDay(i).setNightShift(true); amountOfNightShifts--; } } } else
                 */
                if (nightshiftBefore < nightshiftAfter) { // als dichtste nightshift ervoor ligt
                    if (nightshiftBefore <= maxAmountDaysBetweenExtendingNightshift) {
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
                    if (nightshiftAfter <= maxAmountDaysBetweenExtendingNightshift) {
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

    /*
     * verwijdert item en plaats het ergens anders op een lege blok
     */
    private static void moveProduction(Planning p) {
        Item removedItem = removeProduction(p);
        if (removedItem == null) {
            return;
        }
        int count = 0;

        // zoek idle blok
        boolean stop = false;
        while (!stop || count < MAX_MOVEITEM_TRIES) {
            // random
            int randomDay = random.nextInt(Planning.getNumberOfDays());
            int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            int randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);

            if (p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
                if (previousItem.getId() == removedItem.getId()) {
                    p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(removedItem));
                    removedItem.updateItem(p.getDay(randomDay), randMachine);
                    stop = true;
                    controlNewNightShift(p, randomDay, randomBlock);
                } else {
                    if (setupBeforeNewItem(previousItem, removedItem, randomDay, randomBlock, randMachine, p, 1,
                            false)) {
                        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine,
                                new Production(removedItem));
                        removedItem.updateItem(p.getDay(randomDay), randMachine);
                        stop = true;
                        controlNewNightShift(p, randomDay, randomBlock);
                    }
                }
            }
            count++;
        }
    }

    private static boolean addShippingDay(Planning p) { // random shipping day toevoegen
        int count = 0;
        boolean newShippingDay = false;

        while (!newShippingDay && count < MAX_NEW_SHIPPINGDAY_TRIES) {
            // random request
            Requests requests = p.getRequests();
            int randomRequest = random.nextInt(requests.getRequests().size());
            Request request = requests.get(randomRequest);

            boolean amountWrong = false;

            // random shipping day voor random request
            if (!request.hasShippingDay()) {
                List<Day> possibleDays = request.getPossibleShippingDays();
                int randomShippingDay = random.nextInt(possibleDays.size());
                request.setShippingDay(possibleDays.get(randomShippingDay));
                for (int i = 0; i < p.getStock().getNrOfDifferentItems(); i++) {
                    Item item = p.getStock().getItem(i);
                    if (request.containsItem(item)) {
                        int itemsNeeded = request.getAmountOfItem(item);
                        for (int d = randomShippingDay; d < p.getDays().size(); d++) {
                            int stockAmount = item.getStockAmount(p.getDay(d));
                            int newStockAmount = stockAmount - itemsNeeded;
                            if (newStockAmount < 0) {
                                amountWrong = true;
                                break;
                            }
                        }
                        if (amountWrong) {
                            break;
                        } else {
                            for (int d = randomShippingDay; d < p.getDays().size(); d++) {
                                int stockAmount = item.getStockAmount(p.getDay(d));
                                int newStockAmount = stockAmount - itemsNeeded;
                                item.replace(p.getDay(d), newStockAmount);
                            }
                            newShippingDay = true;
                        }
                    }
                }
            }
            count++;
        }
        return newShippingDay;
    }

    public static void tryToPlanShippingDay(Planning p) {
        for (Request request : p.getRequests().getRequests()) {
            if (request.getShippingDay() == null) {
                boolean containsAllItems = true;

                // CHECK FOR ALL POSSIBLE SHIPPING DAYS
                for (Day sd : request.getPossibleShippingDays()) {
                    for (Item i : request.getItems()) {
                        if (i.getStockAmount(sd) - request.getAmountOfItem(i) < 0) {
                            containsAllItems = false;
                        }
                    }
                    if (containsAllItems) {
                        //plan shipping day in
                        request.setShippingDay(sd);
                        for (Item i : request.getItems()) {

                            for (int d = sd.getId(); d < Planning.getNumberOfDays(); d++) {
                                Day day = p.getDay(d);
                                int newStockAmount = i.getStockAmount(day) - request.getAmountOfItem(i);
                                i.setStockAmount(day, newStockAmount);
                            }
                        }
                        return;
                        //break; //TODO also possible
                    }
                }
            }
        }
    }

    private static void moveShippingDay(Planning p) {
        // alle requesten met shipping day
        List<Request> requests = p.getRequests().getRequests();
        List<Request> requestsWithShippingDay = new ArrayList<>();
        for (Request r : requests) {
            if (r.hasShippingDay()) {
                requestsWithShippingDay.add(r);
            }
        }

        if (!requestsWithShippingDay.isEmpty()) {
            // random request die shipping day heeft
            Request randomRequest = requestsWithShippingDay.get(random.nextInt(requestsWithShippingDay.size()));
            // als maar 1 shipping day niet wijzigen
            if (randomRequest.getPossibleShippingDays().size() >= 2) {
                // random day van alle mogelijke shippingdagen
                int randomPossibleShippingDay = random.nextInt(randomRequest.getPossibleShippingDays().size());
                Day newShippingDay = randomRequest.getPossibleShippingDays().get(randomPossibleShippingDay);
                // shipping day verwijderen
                randomRequest.removeShippingDay();
                // nieuwe shipping day toewijzen
                randomRequest.setShippingDay(newShippingDay);
            }
        } else {
            tryToPlanShippingDay(p);
        }
    }

    private static void addProductionForShipping(Planning p) {
        // alle requesten met shipping day
        List<Request> requests = p.getRequests().getRequests();
        List<Request> requestsWithShippingDay = new ArrayList<>();
        for (Request r : requests) {
            if (r.hasShippingDay()) {
                requestsWithShippingDay.add(r);
            }
        }

        if (!requestsWithShippingDay.isEmpty()) {
            // random request die shipping day heeft
            Request randomRequest = requestsWithShippingDay.get(random.nextInt(requestsWithShippingDay.size()));
            Day shippingDay = randomRequest.getShippingDay();
            Map<Item, Integer> items = randomRequest.getMap();
            boolean foundRandomItem = false;
            Item randomItem = null;
            while (!foundRandomItem) {
                randomItem = p.getStock().getItem(random.nextInt(p.getStock().getNrOfDifferentItems()));
                if (items.containsKey(randomItem)) {
                    foundRandomItem = true;
                }
            }

            boolean itemPlaced = false;
            int count = 0;
            int startPart;
            int stopPart;

            int currentBlock;
            int currentDay;
            Machine currentMachine;

            // eerst voor overdag, dan voor overtime en dan voor nighshifts
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    // overdag
                    startPart = 0;
                    stopPart = Day.indexOfBlockS;
                } else if (i == 1) {
                    // nightshift waar er al nachtshift is
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = p.getDay(0).getBlocks().size() - 1;

                } else if (i == 2) {
                    // overtime
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = Day.indexOfBlockO;
                } else {
                    // nieuwe nachtshift nodig
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = p.getDay(0).getBlocks().size() - 1;
                }

                currentBlock = stopPart;
                currentDay = shippingDay.getId();
                while (!itemPlaced && count < MAX_ADDSHIPPINGPROD_TRIES) {
                    if (i == 0 || (i == 1 && p.getDay(currentDay).hasNightShift())
                            || (i == 2 && !p.getDay(currentDay).hasNightShift())
                            || (i == 3 && !p.getDay(currentDay).hasNightShift())) {
                        // ofwel overdag || ofwel is er nighshift || ofwel overtime als er geen
                        // nightshift is
                        if (currentBlock == startPart - 1) {
                            if (currentDay == 0) {
                                break; // begin van planning
                            } else {
                                currentBlock = stopPart;
                                currentDay--; // begin van dag
                            }
                        }

                        for (int m = 0; m < p.getMachines().size(); m++) { // alle machines afgaan
                            currentMachine = p.getMachines().get(m);

                            if (p.getDay(currentDay).getBlock(currentBlock)
                                    .getMachineState(currentMachine) instanceof Idle) {
                                Item previousItem = currentMachine.getPreviousItem(p, currentDay, currentBlock);
                                if (previousItem.getId() != randomItem.getId()) {
                                    if (setupBeforeNewItem(previousItem, randomItem, currentDay, currentBlock,
                                            currentMachine, p, 1, false)) {
                                        p.getDay(currentDay).getBlock(currentBlock).setMachineState(currentMachine,
                                                new Production(randomItem));
                                        randomItem.updateItem(p.getDay(currentDay), currentMachine);
                                        itemPlaced = true;
                                        i = 99999999;
                                        break;
                                    }
                                }

                            }
                        }
                        currentBlock--;
                    }

                    if (i == 3 && !p.getDay(currentDay).hasNightShift()) {
                        controlNewNightShift(p, currentDay, currentBlock);
                    }

                    count++;

                }
            }
        }
    }

    private static void removeMultipleProduction(Planning p) {
        int count = 0;
        int removedProductions = 0;

        while (count < MAX_REMOVE_PROD_TRIES) {
            int randomDay = random.nextInt(Planning.getNumberOfDays());
            int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            int randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);

            MachineState ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine);
            int amountOfRemovedProductionBlocks = random.nextInt(maxLengthRemovingBlocks);
            if (ms instanceof Production) {
                while (removedProductions < amountOfRemovedProductionBlocks
                        && randomBlock < Day.getNumberOfBlocksPerDay() && p.getDay(randomDay).getBlock(randomBlock)
                        .getMachineState(randMachine) instanceof Production) {
                    p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Idle());
                    removedProductions++;
                    randomBlock++;
                }
            }
            count++;
            // TODO: Elke controle voor eventuele overbodige setup te verwijderen?
        }
    }

    private static void changeMultipleProduction(Planning p) {
        int count = 0;
        int newProductions = 0;

        // random
        int randomDay = NotFound;
        int randomBlock = NotFound;
        int randMachineInt = NotFound;

        // zoek idle blok
        Item newItem = null;
        while (count < MAX_CHANGE_PROD_TRIES) {

            // random
            randomDay = random.nextInt(Planning.getNumberOfDays());
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);
            Block b = p.getDay(randomDay).getBlock(randomBlock);

            if (b.getMachineState(randMachine) instanceof Idle) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock); // TODO
                int aantalItems = p.getStock().getNrOfDifferentItems();
                boolean newItemNotPrevItem = false;
                // random item uit lijst die niet vorige item is
                while (!newItemNotPrevItem) {
                    newItem = p.getStock().getItem(random.nextInt(aantalItems));
                    if (newItem.getId() != previousItem.getId()) {
                        newItemNotPrevItem = true;
                    }
                }

                int amountOfNewProductionBlocks = random.nextInt(maxLengthNewBlocks - 2) + 2;
                if (setupBeforeNewItem(previousItem, newItem, randomDay, randomBlock, randMachine, p,
                        amountOfNewProductionBlocks, false)) {
                    while (newProductions < amountOfNewProductionBlocks && randomBlock < Day.getNumberOfBlocksPerDay()
                            && p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(newItem));
                        newItem.updateItem(p.getDay(randomDay), randMachine);
                        newProductions++;
                        randomBlock++;
                    }

                    b.setMachineState(randMachine, new Production(newItem));
                    newItem.updateItem(p.getDay(randomDay), randMachine);
                    controlNewNightShift(p, randomDay, randomBlock);
                    return;
                }
            }
            count++;
        }

    }

    private static void addMultipleProduction(Planning p) {
        int count = 0;
        int newProductions = 0;

        // random
        int randomDay = NotFound;
        int randomBlock = NotFound;
        int randMachineInt = NotFound;

        // zoek idle blok
        boolean stop = false;
        while (!stop && count < MAX_ADD_PROD_TRIES) {

            // random
            randomDay = random.nextInt(Planning.getNumberOfDays());
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            randMachineInt = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randMachineInt);

            int amountOfNewProductionBlocks = random.nextInt(maxLengthNewBlocks - 2) + 2;
            if (p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
                if (setupAfterNewItem(previousItem, randomDay, randomBlock, randMachine, p,
                        amountOfNewProductionBlocks)) {
                    while (newProductions < amountOfNewProductionBlocks && randomBlock < Day.getNumberOfBlocksPerDay()
                            && p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine,
                                new Production(previousItem));
                        previousItem.updateItem(p.getDay(randomDay), randMachine);
                        newProductions++;
                        randomBlock++;
                    }
                    stop = true;
                }
            }
            count++;
        }
        controlNewNightShift(p, randomDay, randomBlock);
    }


}
