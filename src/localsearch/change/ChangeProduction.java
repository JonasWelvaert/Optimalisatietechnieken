package localsearch.change;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.Production;

public class ChangeProduction extends LocalSearchStep {
    public ChangeProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        // nieuwe productie op machine starten
        int count = 0;
        int randomDay, randomBlock, randomMachine;

        // zoek idle blok
        Item newItem = null;
        while (count < maxTries) {

            // random
            randomDay = random.nextInt(Planning.getNumberOfDays());
            randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
            randomMachine = random.nextInt(p.getMachines().size());
            Machine randMachine = p.getMachines().get(randomMachine);
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
                    return false;
                }
            }
            count++;
        }
        return false;
    }
}
