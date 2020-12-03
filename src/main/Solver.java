package main;

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

    static ErrorCounting ec = new ErrorCounting();

    public Solver(int mode) {
        logger.setLevel(Level.OFF);

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
            } while (!checkFeasible(neighbor));

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
        System.out.println(ec.toString());
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

    public static boolean checkFeasible(Planning planning) {

        int teller1 = 0;

        if (!isConsecutiveInNightShiftsPast(planning)) {
            System.out.println("isConsecutiveInNightShiftsPast");
            ec.isConsecutiveInNightShiftsPast++;
            return false;
        }

        for (int d = 0; d < Planning.getNumberOfDays(); d++) {

            List<String> setupTypes = new ArrayList<>();
            Map<String, Integer> setupMap = new HashMap<>();

            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {

                if (!checkNighShiftBlocksConstraints(b, d, planning)) {
                    ec.checkNighShiftBlocksConstraints++;
                    System.out.println("checkNighShiftBlocksConstraints");
                    return false;
                }

                if (!checkOvertimeConstraints(teller1, b, d, planning)) {
                    System.out.println("checkOvertimeConstraints");
                    ec.checkOvertimeConstraints++;
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
                        setupTypes.add(String.valueOf(i1.getId()) + String.valueOf(i2.getId()));
                        setupMap.put(String.valueOf(i1.getId()) + String.valueOf(i2.getId()), i1.getSetupTimeTo(i2));
                    }

                    // check that only 1 maintenance/large setup is scheduled at any block of a day
                    // -> no parallel maintenance/large setup
                    if (state instanceof LargeSetup || state instanceof Maintenance) {
                        parallelTeller++;
                        if (parallelTeller > 1) {
                            System.out.println("niet parallel largesetup maintenance");
                            return false;
                        }
                    }

                    if (d < Planning.getNumberOfDays() && b < Day.getNumberOfBlocksPerDay() - 1) {
                        if (!checkProductionConstraints(m, b, d, planning)) {
                            ec.checkProductionConstraints++;
                            System.out.println("checkProductionConstraints");
                            return false;
                        }
                    }
                }
            }
            if (!checkStockConstraints(planning.getDay(d), planning)) {
                ec.checkStockConstraints++;
                System.out.println("checkStockConstraints");
                return false;
            }

            if (!checkSetupTypeConstraint(setupTypes, setupMap)) {
                System.out.println("checkSetupTypeConstraint");
                ec.checkSetupTypeConstraint++;
                return false;
            }

/*if (!checkShippingDayConstraints(d, planning)) {
                System.out.println("checkShippingDayConstraints");
                ec.checkShippingDayConstraints++;
                return false;
            }
*/

        }

        for (Machine m : planning.getMachines()) {

            if (!checkSetupConstraint(m, planning)) {
                System.out.println("checkSetupConstraint");
                return false;
            }

            for (int d = 0; d < Planning.getNumberOfDays(); d++) {
                if (!checkChangeOverAndMaintenanceBoundaryConstraints(d, m, planning)) {
                    System.out.println("checkChangeOverAndMaintenanceBoundaryConstraints");
                    ec.checkChangeOverAndMaintenanceBoundaryConstraints++;
                    return false;
                }

                if (!checkMaintenanceConstraints(d, m, planning)) {
                    System.out.println("checkMaintenanceConstraints");
                    ec.checkMaintenanceConstraints++;
                    return false;
                }
            }
        }

        return true;
    }

    // check if the minimum amount of consecutive night shifts is fulfilled when
    // their are past consecutive days
    private static boolean isConsecutiveInNightShiftsPast(Planning planning) {
        if (planning.getPastConsecutiveDaysWithNightShift() > 0) {
            for (int i = 0; i < (Planning.getMinConsecutiveDaysWithNightShift()
                    - planning.getPastConsecutiveDaysWithNightShift()); i++) {
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
                if (planning.getDay(d).getBlock(b).getMachineState(m) instanceof LargeSetup
                        || planning.getDay(d).getBlock(b).getMachineState(m) instanceof Maintenance) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkOvertimeConstraints(int teller, int b, int d, Planning planning) {

        if (planning.getDay(d).hasNightShift()) {

            teller++; // TODO teller opgehoogd, maar niet meer gebruikt ?
            // check that the minimum amount of consecutive days with night shift is
            // fulfilled
            if (d > (Planning.getMinConsecutiveDaysWithNightShift()
                    - planning.getPastConsecutiveDaysWithNightShift())) {
                // for (int l = d; l < (d + Planning.getMinConsecutiveDaysWithNightShift());
                // l++) {
                for (int l = d; l < Planning.getNumberOfDays(); l++) {
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
                teller = 0; // TODO teller op null gezet maar niet meer gebruikt ?
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

    // check that for each day only 1 setup of a certain type is scheduled
    private static boolean checkSetupTypeConstraint(List<String> setupTypes, Map<String, Integer> setupMap) {

        Map<String, Integer> hm = new HashMap<>();

        // setup type is the key -> only 1 value possible
        // key = setup type
        // value = number of occurrences of set up types
        for (String setUpTime : setupTypes) {
            Integer j = hm.get(setUpTime);
            hm.put(setUpTime, (j == null) ? 1 : j + 1);
        }

        // if number of set up types > set up time -> return false
        for (Map.Entry<String, Integer> setupType : hm.entrySet()) {
            int setupTime = setupMap.get(setupType.getKey());
            int setupOccurrences = setupType.getValue();
            if (setupOccurrences > setupTime) {
                return false;
            }
        }

        return true;
    }

    // check that the production of an item is only possible after the right
    // configuration
    // only possible if previous block is changeover to item or previous block is
    // production of same item
    private static boolean checkProductionConstraints(Machine m, int b, int d, Planning planning) {

        MachineState state1 = planning.getDay(d).getBlock(b).getMachineState(m);
        MachineState state2 = planning.getDay(d).getBlock(b + 1).getMachineState(m);

        if (state1 instanceof Production && state2 instanceof Production) {
            if (((Production) state1).getItem().getId() != ((Production) state2).getItem().getId()) {
                return false;
            }
        }
        if (state1 instanceof Setup && state2 instanceof Production) {
            return ((Setup) state1).getTo().getId() == ((Production) state2).getItem().getId();
        }
        if (state2 instanceof Setup && state1 instanceof Production) {
            return ((Setup) state2).getFrom().getId() == ((Production) state1).getItem().getId();
        }
        return true;
    }

    private static boolean checkSetupConstraint(Machine m, Planning p) {

        Item currentItem = m.getInitialSetup();
        for (int d = 0; d < Planning.getNumberOfDays(); d++) {
            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {
                if (p.getDay(d).getBlock(b).getMachineState(m) instanceof Production) {
                    Production production = (Production) p.getDay(d).getBlock(b).getMachineState(m);
                    if (production.getItem().getId() != currentItem.getId()) {
                        return false;
                    }
                } else if (p.getDay(d).getBlock(b).getMachineState(m) instanceof Setup) {
                    Setup s = (Setup) p.getDay(d).getBlock(b).getMachineState(m);
                    Item to = s.getTo();
                    Item from = s.getFrom();
                    int lengthsetup = from.getSetupTimeTo(to);
                    if (s.getFrom().getId() != currentItem.getId()) {
                        return false;
                    } else {
                        currentItem = s.getTo();
                    }
                    b = b + lengthsetup;
                }
            }
        }

        return true;
    }

    private static boolean checkStockConstraints(Day day, Planning planning) {

        for (Item i : planning.getStock().getItems()) {
            int stockAmount = i.getStockAmount(day);
            // check that the stock level on day d is less than the max allowed of stock of
            // an item
            if (i.getStockAmount(day) > i.getMaxAllowedInStock()) {
                System.out.println(i.getStockAmount(day) + " " + i.getMaxAllowedInStock());
                return false;
            }

            // check that the stock level on day d is 0 when the stock level is below the
            // minimum amount
            /*
             * if (i.getStockAmount(day) < i.getMinAllowedInStock() && i.getStockAmount(day)
             * != 0) { return false; }
             */

            if (i.getStockAmount(day) < 0) {
                return false;
            }

        }

        return true;
    }

    // check that the minimum amount of maintenances are scheduled in the time
    // horizon
    private static boolean checkMaintenanceConstraints(int d, Machine m, Planning planning) {

        int init = m.getInitialDaysPastWithoutMaintenance();
        int max = m.getMaxDaysWithoutMaintenance();
        int now = max - init;
        int maintenanceBlockTeller = 0;
        int maintenanceDayTeller = 0;
        int duration = 0;
        if ((d - now) == 0 || ((d - now) % (max + 1)) == 0) {
            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {
                if (planning.getDay(d).getBlock(b).getMachineState(m) instanceof Maintenance) {
                    duration = m.getMaintenanceDurationInBlocks();
                    maintenanceBlockTeller++;
                    if (maintenanceBlockTeller == duration) {
                        return true;
                    }
                } else {
                    maintenanceBlockTeller = 0;
                }
            }
            return false;
        }

        return true;

    }

    // check that no production is done in between blocks of changeover and blocks
    // of maintenance
    public static boolean checkChangeOverAndMaintenanceBoundaryConstraints(int d, Machine m, Planning planning) {

        int setupTeller = 0;
        int maintenanceTeller = 0;
        int setupTime = 0;
        int maintenanceTime = 0;
        Item i1 = null;
        Item i2 = null;
        for (int j = 0; j < Day.getNumberOfBlocksPerDay(); j++) {
            MachineState state = planning.getDay(d).getBlock(j).getMachineState(m);

            // maintenance/idle is allowed in between setup blocks and setup/idle is allowed
            // in between maintenance blocks
            if (state instanceof Setup || state instanceof Idle || state instanceof Maintenance) {

                // machine state is in setup configuration
                if (state instanceof Setup) {
                    Setup setup = (Setup) planning.getDay(d).getBlock(j).getMachineState(m);

                    // check if the setup is still the same (S1_2 -> S2_3)
                    if (i1 == null
                            || !(i1.getId() == (setup.getFrom().getId()) || !(i2.getId() == setup.getFrom().getId()))) {

                        if (setupTeller > 0 && setupTeller <= setupTime) {
                            return false;
                        }

                        // get initial setup
                        i1 = setup.getFrom();
                        i2 = setup.getTo();
                        setupTime = i1.getSetupTimeTo(i2);
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

                // block is in production -> check if the block is not between setup's or
                // maintenance's
            } else if ((setupTeller > 0 && setupTeller <= setupTime)
                    || (maintenanceTeller > 0 && maintenanceTeller <= maintenanceTime)) {
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

    private static boolean checkShippingDayConstraints(int d, Planning planning) {
        // check every request whether the amount produced/in-stock is sufficient
        for (Request request : planning.getRequests()) {

            // if day d is a shipping day
            if (request.getShippingDay() != null) {
                if (request.getShippingDay().getId() == planning.getDay(d).getId()) {

                    // if day d not in possible shipping days
                    if (!request.getPossibleShippingDays().contains(planning.getDay(d))) {
                        return false;
                    }

                    // check if the amount for each item in the request is fulfilled
                    for (Item i : request.getItems()) {
                        int amount = 0;

                        // amount in the stock
                        amount = planning.getStock().getItem(i.getId()).getStockAmount(planning.getDay(d));

                        for (Machine m : planning.getMachines()) {
                            // check the amount produced on the last block in the day for every machine
                            int lastBlockOfProduction = 0;
                            for (int j = 0; j < Day.getNumberOfBlocksPerDay(); j++) {
                                if (planning.getDay(d).getBlock(j).getMachineState(m) instanceof Production) {
                                    lastBlockOfProduction = planning.getDay(d).getBlock(j).getId();
                                }
                            }
                            if (planning.getDay(d).getBlock(lastBlockOfProduction)
                                    .getMachineState(m) instanceof Production) {
                                Production production = (Production) planning.getDay(d).getBlock(lastBlockOfProduction)
                                        .getMachineState(m);
                                if (production.getItem().getId() == i.getId()) {
                                    amount += production.getItem().getStockAmount(planning.getDay(d));
                                }
                            }
                        }
                        if (amount < request.getAmountOfItem(i)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

}
