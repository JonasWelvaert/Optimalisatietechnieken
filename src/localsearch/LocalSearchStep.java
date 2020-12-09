package localsearch;

import main.Main;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.util.*;
import java.util.logging.Logger;


// USES FACTORY METHOD PATTERN
public abstract class LocalSearchStep {
    protected Random random;
    protected final int maxTries;
    protected static final Logger logger = Logger.getLogger(LocalSearchStep.class.getName());


    public LocalSearchStep(int maxTries) {
        this.random = (Main.seed < 0) ? new Random() : new Random(Main.seed + 1);
        this.maxTries = maxTries;
    }

    public abstract boolean execute(Planning p);

    /**
     * @param setupNeeded The needed setup
     * @param day         The day in which the production of newItem will be
     * @param block       The block in which the production of newItem will be
     * @param machine     The machine on which the production will be scheduled
     * @param p           The planning on which everything is called
     * @return List of block in which setups will be planned otherwise null
     */
    protected List<Block> getSetupBlockBeforeProduction(Setup setupNeeded, Day day, Block block, Machine machine, Planning p) {
        int numberOfPredecessorDays = day.getId();  // @indexOutOfBounds
        int iteration = 0;
        // SET TIME WINDOWS IN WHICH SETUPS CAN HAPPEN
        int t0, t1, t2;
        t1 = block.getId() - 1;

        if (setupNeeded instanceof SmallSetup) {
            t0 = 0;
            t2 = Day.getNumberOfBlocksPerDay() - 1;  // @indexOutOfBounds
        } else {
            t0 = Day.getIndexOfBlockE();
            t2 = Day.getIndexOfBlockL();
            if (t1 < t0) {
                iteration++; // BEGIN CHECK YESTERDAY (do not check for today)
                t1 = t2;
            }
        }
        List<Block> solution = new ArrayList<>();

        while (iteration <= numberOfPredecessorDays) {
            Day dayTemp = p.getDay(day.getId() - iteration);
            List<Block> possibleBlocks = dayTemp.getBlocksBetweenInclusive(t0, t1);
            Collections.reverse(possibleBlocks);
            for (Block b : possibleBlocks) {
                if (b.getMachineState(machine) instanceof Production || b.getMachineState(machine) instanceof Setup) {
                    return null;
                }
                if (!(b.getMachineState(machine) instanceof Maintenance)) {
                    solution.add(b);

                    if (solution.size() >= setupNeeded.getSetupTime()) {
                        return solution;
                    }
                }
            }
            t1 = t2;
            iteration++;
        }
        return null;
    }

    /**
     * @param setupNeeded The needed setup
     * @param day         The day in which the production of newItem will be
     * @param block       The block in which the production of newItem will be
     * @param machine     The machine on which the production will be scheduled
     * @param p           The planning on which everything is called
     * @return List of block in which setups will be planned otherwise null
     */
    protected List<Block> getSetupBlocksAfterProduction(Setup setupNeeded, Day day, Block block, Machine machine, Planning p) {
        int numberOfSuccesorDays = Planning.getNumberOfDays() - day.getId();   // @indexOutOfBounds
        int iteration = 0;
        // SET TIME WINDOWS IN WHICH SETUPS CAN HAPPEN
        int t0, t1, t2;
        t1 = block.getId() + 1;

        if (setupNeeded instanceof SmallSetup) {
            t0 = 0;
            t2 = Day.getNumberOfBlocksPerDay() - 1;  // @indexOutOfBounds
        } else {
            t0 = Day.getIndexOfBlockE();
            t2 = Day.getIndexOfBlockL();
            if (t1 < t0) {
                iteration++; // BEGIN CHECK YESTERDAY (do not check for today)
                t1 = t2;
            }
        }
        List<Block> solution = new ArrayList<>();
        while (iteration < numberOfSuccesorDays) {
            Day dayTemp = p.getDay(day.getId() + iteration);

            List<Block> possibleBlocks = dayTemp.getBlocksBetweenInclusive(t1, t2);
            for (Block b : possibleBlocks) {
                if (b.getMachineState(machine) instanceof Production || b.getMachineState(machine) instanceof Setup) {
                    if (solution.size() >= setupNeeded.getSetupTime()) {
                        Collections.reverse(solution);
                        return solution.subList(0, setupNeeded.getSetupTime());  //@IndexOutOfBounds
                    } else {
                        return null;
                    }
                }
                if (!(b.getMachineState(machine) instanceof Maintenance)) {
                    solution.add(b);
                }
            }
            t1 = t0;
            iteration++;
        }
        if (solution.size() >= setupNeeded.getSetupTime()) {
            Collections.reverse(solution);
            return solution.subList(0, setupNeeded.getSetupTime());  //@IndexOutOfBounds
        } else {
            return null;
        }
    }

    /**
     * @param m                   machine to use
     * @param p                   planning
     * @param nItem               item to be produced
     * @param amountOfItemsNeeded amount of items to be produced (NOT amount of blocks !!!)
     * @return true if production was planned, false if no production was planned (machine efficiency could be 0)
     */
    public boolean addSpecificProductionForItem(Machine m, Planning p, Item nItem, int amountOfItemsNeeded) {
        //ONLY DO ALL THIS STUFF IF THE EFFICIENCY IS NOT ZERO !!!
        if (m.getEfficiency(nItem) != 0) {

            int productionBlocksNeeded = amountOfItemsNeeded / m.getEfficiency(nItem);
            productionBlocksNeeded++;

            //GET LIST OF CONSECUTIVE IDLE BLOCKS ON MACHINE
            List<Block> blocks = new ArrayList<>();
            for (Day day : p.getDays()) {
                for (Block block : day.getBlocks()) {
                    Item pItem = m.getPreviousItem(p, day, block); // returns same value as long as all blocks are Idle or Maintenance
                    Setup setupBefore = null;
                    Setup setupAfter = null;
                    int setupBeforeDuration = 0;
                    int setupAfterDuration = 0;

                    if (!pItem.equals(nItem)) {
                        setupBefore = pItem.getSetupTo(nItem);
                        setupBeforeDuration += setupBefore.getSetupTime();
                        setupAfter = nItem.getSetupTo(pItem);
                        setupAfterDuration += setupAfter.getSetupTime();
                    }

                    MachineState ms = block.getMachineState(m);
                    if (ms instanceof Idle) {
                        blocks.add(block);
                    } else if (!(ms instanceof Maintenance)) {
                        // IF STATE IS NOT IDLE AND ALSO NOT MAINTENANCE, CHECK IF SIZE IS SUFFICIENT
                        if (blocks.size() >= (productionBlocksNeeded + setupBeforeDuration + setupAfterDuration)) {  //5 blocks needed     // size = 7

                            // PLAN SETUP BEFORE
                            for (int i = 0; i < setupBeforeDuration; i++) {
                                blocks.get(i).setMachineState(m, setupBefore);
                            }
                            // PLAN PRODUCTION
                            for (int i = setupBeforeDuration; i < (setupAfterDuration + productionBlocksNeeded); i++) {
                                blocks.get(i).setMachineState(m, new Production(nItem));
                                p.updateStockLevels(day, nItem, m.getEfficiency(nItem));
                            }
                            // PLAN SETUP AFTER
                            for (int i = (setupAfterDuration + productionBlocksNeeded); i < (setupAfterDuration + productionBlocksNeeded + setupAfterDuration); i++) {
                                blocks.get(i).setMachineState(m, setupAfter);

                            }


                            return true;
                        } else {
                            blocks.clear();
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }


}

/*    private static void removeMultipleProduction(Planning p) {
        if (items.containsKey(randomItem)) {
            int count = 0;
            foundRandomItem = true;
            int removedProductions = 0;
        }

    }
		while (count < MAX_REMOVE_PROD_TRIES) {

        int randomDay = random.nextInt(Planning.getNumberOfDays());
        boolean itemPlaced = false;
        int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
        int count = 0;
        int randMachineInt = random.nextInt(p.getMachines().size());
        int startPart;
        Machine randMachine = p.getMachines().get(randMachineInt);
        int stopPart;


        MachineState ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine);
        int currentBlock;
        int amountOfRemovedProductionBlocks = random.nextInt(maxLengthRemovingBlocks);
        int currentDay;
        if (ms instanceof Production) {
        Machine currentMachine;
        while (removedProductions < amountOfRemovedProductionBlocks

						&& randomBlock < Day.getNumberOfBlocksPerDay() && p.getDay(randomDay).getBlock(randomBlock)
        // eerst voor overdag, dan voor overtime en dan voor nighshifts
        .getMachineState(randMachine) instanceof Production) {
        for (int i = 0; i < 4; i++) {
        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Idle());
        if (i == 0) {
        removedProductions++;
        // overdag
        randomBlock++;
        startPart = 0;
        }
        stopPart = Day.indexOfBlockS;
        }
        } else if (i == 1) {
        count++;
        // nightshift waar er al nachtshift is
        // TODO: Elke controle voor eventuele overbodige setup te verwijderen?
        startPart = Day.indexOfBlockS + 1;
        }
        stopPart = p.getDay(0).getBlocks().size() - 1;
        }


        } else if (i == 2) {
private static void changeMultipleProduction(Planning p) {
        // overtime
        int count = 0;
        startPart = Day.indexOfBlockS + 1;
        int newProductions = 0;
        stopPart = Day.indexOfBlockO;

        } else {
        // random
        // nieuwe nachtshift nodig
        int randomDay = NotFound;
        startPart = Day.indexOfBlockS + 1;
        int randomBlock = NotFound;
        stopPart = p.getDay(0).getBlocks().size() - 1;
        int randMachineInt = NotFound;
        }


        // zoek idle blok
        currentBlock = stopPart;
        Item newItem = null;
        currentDay = shippingDay.getId();
        while (count < MAX_CHANGE_PROD_TRIES) {
        while (!itemPlaced && count < MAX_ADDSHIPPINGPROD_TRIES) {

        if (i == 0 || (i == 1 && p.getDay(currentDay).hasNightShift())
        // random
        || (i == 2 && !p.getDay(currentDay).hasNightShift())
        randomDay = random.nextInt(Planning.getNumberOfDays());
        || (i == 3 && !p.getDay(currentDay).hasNightShift())) {
        randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
        // ofwel overdag || ofwel is er nighshift || ofwel overtime als er geen
        randMachineInt = random.nextInt(p.getMachines().size());
        // nightshift is
        Machine randMachine = p.getMachines().get(randMachineInt);
        if (currentBlock == startPart - 1) {
        Block b = p.getDay(randomDay).getBlock(randomBlock);
        if (currentDay == 0) {

        break; // begin van planning
        if (b.getMachineState(randMachine) instanceof Idle) {
        } else {
        Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock); // TODO
        currentBlock = stopPart;
        int aantalItems = p.getStock().getNrOfDifferentItems();
        currentDay--; // begin van dag
        boolean newItemNotPrevItem = false;
        }
        // random item uit lijst die niet vorige item is
        }
        while (!newItemNotPrevItem) {

        newItem = p.getStock().getItem(random.nextInt(aantalItems));
        for (int m = 0; m < p.getMachines().size(); m++) { // alle machines afgaan
        if (newItem.getId() != previousItem.getId()) {
        currentMachine = p.getMachines().get(m);
        newItemNotPrevItem = true;

        }
        if (p.getDay(currentDay).getBlock(currentBlock)
        }
        .getMachineState(currentMachine) instanceof Idle) {

        Item previousItem = currentMachine.getPreviousItem(p, currentDay, currentBlock);
        int amountOfNewProductionBlocks = random.nextInt(maxLengthNewBlocks - 2) + 2;
        if (previousItem.getId() != randomItem.getId()) {
        if (setupBeforeNewItem(previousItem, newItem, randomDay, randomBlock, randMachine, p,
        if (setupBeforeNewItem(previousItem, randomItem, currentDay, currentBlock,
        amountOfNewProductionBlocks, false)) {
        currentMachine, p, 1, false)) {
        while (newProductions < amountOfNewProductionBlocks && randomBlock < Day.getNumberOfBlocksPerDay()
        p.getDay(currentDay).getBlock(currentBlock).setMachineState(currentMachine,
        && p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
        new Production(randomItem));
        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Production(newItem));
        randomItem.updateItem(p.getDay(currentDay), currentMachine);
        newItem.updateItem(p.getDay(randomDay), randMachine);
        itemPlaced = true;
        newProductions++;
        i = 99999999;
        randomBlock++;
        break;
        }
        }

        }
        b.setMachineState(randMachine, new Production(newItem));

        newItem.updateItem(p.getDay(randomDay), randMachine);
        }
        controlNewNightShift(p, randomDay, randomBlock);
        }
        return;
        currentBlock--;
        }
        }
        }

        count++;
        if (i == 3 && !p.getDay(currentDay).hasNightShift()) {
        }
        controlNewNightShift(p, currentDay, currentBlock);

        }
        }


        count++;
private static void addMultipleProduction(Planning p) {

        int count = 0;
        }
        int newProductions = 0;
        }

        }
        // random
        }
        int randomDay = NotFound;

        int randomBlock = NotFound;
private static void removeMultipleProduction(Planning p) {
        int randMachineInt = NotFound;
        int count = 0;

        int removedProductions = 0;
        // zoek idle blok

        boolean stop = false;
        while (count < MAX_REMOVE_PROD_TRIES) {
        while (!stop && count < MAX_ADD_PROD_TRIES) {
        int randomDay = random.nextInt(Planning.getNumberOfDays());

        int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
        // random
        int randMachineInt = random.nextInt(p.getMachines().size());
        randomDay = random.nextInt(Planning.getNumberOfDays());
        Machine randMachine = p.getMachines().get(randMachineInt);
        randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());

        randMachineInt = random.nextInt(p.getMachines().size());
        MachineState ms = p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine);
        Machine randMachine = p.getMachines().get(randMachineInt);
        int amountOfRemovedProductionBlocks = random.nextInt(maxLengthRemovingBlocks);

        if (ms instanceof Production) {
        int amountOfNewProductionBlocks = random.nextInt(maxLengthNewBlocks - 2) + 2;
        while (removedProductions < amountOfRemovedProductionBlocks
			if (p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
                    && randomBlock < Day.getNumberOfBlocksPerDay() && p.getDay(randomDay).getBlock(randomBlock)
        Item previousItem = randMachine.getPreviousItem(p, randomDay, randomBlock);
        .getMachineState(randMachine) instanceof Production) {
        if (setupAfterNewItem(previousItem, randomDay, randomBlock, randMachine, p,
        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine, new Idle());
        amountOfNewProductionBlocks)) {
        removedProductions++;
        while (newProductions < amountOfNewProductionBlocks && randomBlock < Day.getNumberOfBlocksPerDay()
        randomBlock++;
        && p.getDay(randomDay).getBlock(randomBlock).getMachineState(randMachine) instanceof Idle) {
        }
        p.getDay(randomDay).getBlock(randomBlock).setMachineState(randMachine,
        }
        new Production(previousItem));
        count++;
        previousItem.updateItem(p.getDay(randomDay), randMachine);
        // TODO: Elke controle voor eventuele overbodige setup te verwijderen?
        newProductions++;
        }
        randomBlock++;
        }
        }

        stop = true;
private static void changeMultipleProduction(Planning p) {
        }
        int count = 0;
        }
        int newProductions = 0;
        count++;

        }
        // random
        controlNewNightShift(p, randomDay, randomBlock);
        int randomDay = NotFound;
        }*/
