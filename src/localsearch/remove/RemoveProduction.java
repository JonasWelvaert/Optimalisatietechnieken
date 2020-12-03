package localsearch.remove;

import localsearch.LocalSearchStep;
import model.Block;
import model.Day;
import model.Machine;
import model.Planning;
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
        while (count < maxTries) {
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


        return false;
    }
}
