package localsearch.add;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Production;
import model.machinestate.setup.Setup;

import java.util.List;

public class AddSingleProduction extends LocalSearchStep {

    public AddSingleProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        int count = 0;
        int randomDay, randomBlock, randomMachine, randomItem;

        while (count < maxTries) {
            randomDay = random.nextInt(Planning.getNumberOfDays());
            Day day = p.getDay(randomDay);
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            Block block = day.getBlock(randomBlock);
            randomMachine = random.nextInt(p.getMachines().size());
            Machine machine = p.getMachines().get(randomMachine);
            randomItem = random.nextInt(Stock.getNrOfDifferentItems());

            MachineState machineState = block.getMachineState(machine);
            if (machineState instanceof Idle) {
                Item pItem = machine.getPreviousItem(p, day, block);
                Item nItem = p.getStock().getItem(randomItem);

                boolean productionCanBePlanned;

                // STARTING FROM FEASIBLE STATE, SO IF NO BEFORE NEEDED, ALSO NO AFTER IS NEEDED
                if (!pItem.equals(nItem)) {
                    Setup setupBefore = pItem.getSetupTo(nItem);
                    Setup setupAfter = nItem.getSetupTo(pItem);
                    List<Block> beforeBlocks;
                    List<Block> afterBlocks;

                    beforeBlocks = getSetupBlockBeforeProduction(setupBefore, day, block, machine, p);
                    afterBlocks = getSetupBlocksAfterProduction(setupAfter, day, block, machine, p);
                    //PLAN THE SETUPS IF NOT NULL
                    if (beforeBlocks != null && afterBlocks != null) {
                        for (Block b : beforeBlocks) {
                            b.setMachineState(machine, setupBefore);
                        }
                        for (Block b : afterBlocks) {
                            b.setMachineState(machine, setupAfter);
                        }
                        productionCanBePlanned = true;
                    } else {
                        productionCanBePlanned = false;
                    }
                }
                //ITEM ARE THE SAME SO NO SETUP NEEDED
                else {
                    productionCanBePlanned = true;
                }
                //PLAN PRODUCTION
                if (productionCanBePlanned) {
                    block.setMachineState(machine, new Production(nItem));
                    p.updateStockLevels(day, nItem, machine.getEfficiency(nItem));
                }
            }
            count++;
        }
        return true;
    }
}
