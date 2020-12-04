package localsearch.other;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Will convert next planning p1 to p2
 * p1: 1    1   1   1   1   s12     2   2   2   s23     s31      1   1   1
 * p2: 1    1   1   1   1   s12     2   2   2   s31     IDLE     1   1   1
 */
public class JoinSingleNeighbouringSetups extends LocalSearchStep {

    public JoinSingleNeighbouringSetups(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {

        int iteration = 0;
        int randomDay, randomBlock, randomMachine;

        randomDay = random.nextInt(Planning.getNumberOfDays());
        Day day = p.getDay(randomDay);
        randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
        Block block = day.getBlock(randomBlock);
        randomMachine = random.nextInt(p.getMachines().size());
        Machine machine = p.getMachines().get(randomMachine);

        int numberOfSuccesorDays = Planning.getNumberOfDays() - day.getId();   // @indexOutOfBounds


        // SET TIME WINDOWS IN WHICH SETUPS CAN HAPPEN
        int t0 = 0;
        int t1 = block.getId();
        int t2 = Day.getNumberOfBlocksPerDay() - 1;  // @indexOutOfBounds


        int setupDuration = -1;

        List<Block> foundBlocks = new ArrayList<>();

        boolean secondSetupFound = false;

        while (iteration < numberOfSuccesorDays) {
            Day dayTemp = p.getDay(day.getId() + iteration);

            List<Block> possibleBlocks = dayTemp.getBlocksBetweenInclusive(t1, t2);


            for (Block b : possibleBlocks) {
                //FIND NEXT SETUP BLOCK
                MachineState ms = b.getMachineState(machine);
                if (ms instanceof Setup) {
                    if (foundBlocks.size() == 0) {
                        setupDuration = ((Setup) ms).getSetupTime();  // kan in volgende dag overlopen
                    }

                    if (foundBlocks.size() == setupDuration) {
                        // foundBlocks: bevat alle setups bloks van huidige setup
                        // b is block van volgende setup
                        secondSetupFound = true; //controleer of er geen derde soort setup grenst aan de tweede
                        setupDuration += ((Setup) ms).getSetupTime();

                    }
                    foundBlocks.add(b);

                } else if (ms instanceof Production && foundBlocks.size() != 0) {
                    return false;
                }

                if (secondSetupFound && foundBlocks.size() == setupDuration) {
                    // FOUND BLOCKS CONTAINS ALL BLOCKS OF 2 SETUPS
                    // S12   S23     // from =1    // to = 3

                    Item from = ((Setup) foundBlocks.get(0).getMachineState(machine)).getFrom();
                    Item to = ((Setup) foundBlocks.get(foundBlocks.size() - 1).getMachineState(machine)).getTo();

                    if (from == to) {
                        for (Block bTemp : foundBlocks) {
                            bTemp.setMachineState(machine, new Idle());
                        }
                        return true;
                    }


                    int newSetupTime = from.getSetupTimeTo(to);
                    int previousSetupTime = foundBlocks.size();

                    if (newSetupTime <= previousSetupTime) {
                        Collections.reverse(foundBlocks);

                        for (int i = 0; i < newSetupTime; i++) {
                            Setup newSetup = from.getSetupTo(to);
                            Block bTemp = foundBlocks.get(i);
                            bTemp.setMachineState(machine, newSetup);
                        }
                        for (int i = newSetupTime; i < foundBlocks.size(); i++) {
                            Block bTemp = foundBlocks.get(i);
                            bTemp.setMachineState(machine, new Idle());
                        }
                    } else {
                        return false;
                    }
                    return true; // EOF function
                }


            }
            t1 = t0;
            iteration++;
        }
        return false;
    }
}
