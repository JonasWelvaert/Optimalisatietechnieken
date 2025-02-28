package localsearch;

import main.Main;
import model.Block;
import model.Day;
import model.Machine;
import model.Planning;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;


// USES FACTORY METHOD PATTERN
public abstract class LocalSearchStep {
    protected Random random;
    protected final int maxTries;
    private static final Logger logger = Logger.getLogger(LocalSearchStep.class.getName());


    public LocalSearchStep(int maxTries) {
        this.random = new Random();
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
                        return solution.subList(0, setupNeeded.getSetupTime() );  //@IndexOutOfBounds
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
            return solution.subList(0, setupNeeded.getSetupTime() );  //@IndexOutOfBounds
        } else {
            return null;
        }
    }
}
