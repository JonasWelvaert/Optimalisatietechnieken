package localsearch.move;

import localsearch.LocalSearchStep;
import model.Day;
import model.Item;
import model.Machine;
import model.Planning;
import model.machinestate.Idle;
import model.machinestate.Production;

public class MoveProduction extends LocalSearchStep {

    public MoveProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {

       /* Item removedItem = removeProduction(p);
        if (removedItem == null) {
            return false;
        }
        int count = 0;

        // zoek idle blok
        boolean stop = false;
        while (!stop || count < maxTries) {
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
        }*/

        return false;
    }
}
