package localsearch.add;

import java.util.ArrayList;
import java.util.List;

import localsearch.LocalSearchStep;
import model.Block;
import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Production;
import model.machinestate.setup.Setup;

public class AddProductionAfterPlannedProduction extends LocalSearchStep {

    public AddProductionAfterPlannedProduction(int maxTries) {
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


        int t0 = 0;
        int t1 = block.getId();
        int t2 = Day.getNumberOfBlocksPerDay() - 1;  // @indexOutOfBounds


        while (iteration < numberOfSuccesorDays) {
            Day dayTemp = p.getDay(day.getId() + iteration);

            List<Block> possibleBlocks = dayTemp.getBlocksBetweenInclusive(t1, t2);

            Block productionBlock = null;
            
            for (Block b : possibleBlocks) {
                //FIND NEXT PRODUCTION BLOCK
                MachineState ms = b.getMachineState(machine);
                if (ms instanceof Production) {
                    if (productionBlock == null) {
                        productionBlock = b;
                        //TODO check if possible to produce more of item i
                        // else infeasible?!?
                    }else {
                    	//do nothing.
                    }

                } else if (productionBlock != null && ms instanceof Setup) {
                	//We konden geen productie plannen achter vorige productie
                    return false;
                } else if(productionBlock!= null && ms instanceof Idle) {
                	// we hebben al gecontroleert als productie mogelijk is
                	Item prodItem = ((Production) productionBlock.getMachineState(machine)).getItem();
                	block.setMachineState(machine, new Production(prodItem));
                    p.updateStockLevels(day, prodItem, machine.getEfficiency(prodItem));
                    return true;
                }

            }
            t1 = t0;
            iteration++;
        }
        return false;
    }
}
