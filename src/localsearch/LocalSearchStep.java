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
                        if (blocks.size() >= (productionBlocksNeeded + setupBeforeDuration + setupAfterDuration)) {

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