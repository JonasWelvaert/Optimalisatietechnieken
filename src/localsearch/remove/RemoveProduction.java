package localsearch.remove;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Production;

public class RemoveProduction extends LocalSearchStep {

    public RemoveProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {

        int count = 0;
        int randomDay, randomBlock, randomMachine;

        tries:
        while (count < maxTries) {
            count++;
            randomDay = random.nextInt(Planning.getNumberOfDays());
            Day day = p.getDay(randomDay);

            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            Block block = day.getBlock(randomBlock);

            randomMachine = random.nextInt(p.getMachines().size());
            Machine machine = p.getMachines().get(randomMachine);

            MachineState state = block.getMachineState(machine);


            if (!(state instanceof Production)) {
                continue tries;
            }

            Item cItem = ((Production) state).getItem();
            Day temp = p.getLastPlannedShippingDayForItem(cItem);

            if (temp ==null || day.getId() < temp.getId()) {
                continue tries;
            }

            for (Day d : p.getSuccessorDaysInclusive(day)) {
                if (cItem.getStockAmount(d) - machine.getEfficiency(cItem) < 0) {
                    continue tries;
                }
            }

            int machineEfficiency = -1 * machine.getEfficiency(cItem);
            block.setMachineState(machine, new Idle());
            p.updateStockLevels(day, cItem, machineEfficiency);

            return true;

            /*
            int count = 0;
            while (count < maxTries) {
                int randomDay = random.nextInt(Planning.getNumberOfDays());
                int randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
                int randMachineInt = random.nextInt(p.getMachines().size());
                Machine randMachine = p.getMachines().get(randMachineInt);
                Block b = p.getDay(randomDay).getBlock(randomBlock);

                MachineState ms = b.getMachineState(randMachine);

                if (ms instanceof Production) {
                    Production prod = (Production) b.getMachineState(randMachine);

                    b.setMachineState(randMachine, new Idle());
                    return false;
                }
                count++;
                // Elke controle voor eventuele overbodige setup te verwijderen?
            }
          */

        }
        return false;
    }
}

