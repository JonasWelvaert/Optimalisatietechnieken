package localsearch.move;

import localsearch.LocalSearchStep;
import model.Day;
import model.Machine;
import model.Planning;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.setup.Setup;

public class MoveMaintenance extends LocalSearchStep {

    public MoveMaintenance(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        int count = 0;
        while (count < maxTries) {
            // van
            int randomDay1 = random.nextInt(Planning.getNumberOfDays());
            int randomBlock1 = random.nextInt(Day.getIndexOfBlockL() - Day.getIndexOfBlockE()) + Day.getIndexOfBlockE(); // enkel
            // overdag
            int randMachineInt1 = random.nextInt(p.getMachines().size());
            Machine randMachine1 = p.getMachines().get(randMachineInt1);
            MachineState ms1 = p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1);

            // naar
            int randomDay2 = random.nextInt(Planning.getNumberOfDays());
            int randomBlock2 = random.nextInt(Day.getIndexOfBlockL() - Day.getIndexOfBlockE()) + Day.getIndexOfBlockE(); // enkel
            // overdag
            int randMachineInt2 = random.nextInt(p.getMachines().size());
            Machine randMachine2 = p.getMachines().get(randMachineInt2);

            // als 1 = maintenance => verplaats naar 2
            if (p.getDay(randomDay1).getBlock(randomBlock1).getMachineState(randMachine1) instanceof Maintenance) {
                // kijken ofdat op de nieuwe plek geen setup of andere maintenance staat
                boolean verplaatsbaar = true;
                for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                    if (p.getDay(randomDay2).getBlock(randomBlock2 + i).getMachineState(randMachine1) instanceof Setup
                            || p.getDay(randomDay2).getBlock(randomBlock2 + i)
                            .getMachineState(randMachine1) instanceof Maintenance) {
                        verplaatsbaar = false;
                    }
                }

                // find first block of maintenance
                boolean firstBlockFound = false;
                int firstBlock = randomBlock1;
                while (!firstBlockFound) {
                    if (p.getDay(randomDay1).getBlock(firstBlock - 1)
                            .getMachineState(randMachine1) instanceof Maintenance) {
                        firstBlock--;
                    } else {
                        firstBlockFound = true;
                    }
                }

                // verplaats volledige maintenance sequence
                if (verplaatsbaar) {
                    for (int i = 0; i < p.getMachines().get(randMachineInt1).getMaintenanceDurationInBlocks(); i++) {
                        // zet op maintenance op idle
                        p.getDay(randomDay1).getBlock(firstBlock + i).setMachineState(randMachine1, new Idle());

                        // zet op nieuwe blok op maintenance
                        p.getDay(randomDay2).getBlock(randomBlock2 + i).setMachineState(randMachine2, ms1);

                    }
                    return false;
                }
                count++;
            }
        }
        return false;
    }
}
