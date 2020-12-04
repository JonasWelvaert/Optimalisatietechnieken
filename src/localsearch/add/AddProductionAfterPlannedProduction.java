package localsearch.add;

import localsearch.LocalSearchStep;
import model.Planning;

public class AddProductionAfterPlannedProduction extends LocalSearchStep {

    public AddProductionAfterPlannedProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
     /*   int iteration = 0;
        int randomDay, randomBlock, randomMachine;



        List<Block> productionWithIdleAfter = new ArrayList<>();

        for(Day d: p.getDays()){
            for(Block b: d.getBlocks()){
                for(Machine m : p.getMachines()){
                    if(b.getMachineState(m) instanceof Production){
                        // if next block IDLE

                        productionWithIdleAfter.add(b);
                    }
                }
            }
        }

        for(Block b: productionWithIdleAfter){



        }




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
                        Counting.JoinSingleNeighbouringSetup++;
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
                    Counting.JoinSingleNeighbouringSetup++;
                    return true; // EOF function
                }


            }
            t1 = t0;
            iteration++;
        }*/
        return false;
    }
}
