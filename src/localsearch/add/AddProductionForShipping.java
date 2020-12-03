package localsearch.add;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.Production;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddProductionForShipping extends LocalSearchStep {

    public AddProductionForShipping(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        // alle requesten met shipping day
        List<Request> requests = p.getRequests().getRequests();
        List<Request> requestsWithShippingDay = new ArrayList<>();
        for (Request r : requests) {
            if (r.hasShippingDay()) {
                requestsWithShippingDay.add(r);
            }
        }

        if (!requestsWithShippingDay.isEmpty()) {
            // random request die shipping day heeft
            Request randomRequest = requestsWithShippingDay.get(random.nextInt(requestsWithShippingDay.size()));
            Day shippingDay = randomRequest.getShippingDay();
            Map<Item, Integer> items = randomRequest.getMap();
            boolean foundRandomItem = false;
            Item randomItem = null;
            while (!foundRandomItem) {
                randomItem = p.getStock().getItem(random.nextInt(p.getStock().getNrOfDifferentItems()));
                if (items.containsKey(randomItem)) {
                    foundRandomItem = true;
                }
            }

            boolean itemPlaced = false;
            int count = 0;
            int startPart;
            int stopPart;

            int currentBlock;
            int currentDay;
            Machine currentMachine;

            // eerst voor overdag, dan voor overtime en dan voor nighshifts
            for (int i = 0; i < 4; i++) {
                if (i == 0) {
                    // overdag
                    startPart = 0;
                    stopPart = Day.indexOfBlockS;
                } else if (i == 1) {
                    // nightshift waar er al nachtshift is
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = p.getDay(0).getBlocks().size() - 1;

                } else if (i == 2) {
                    // overtime
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = Day.indexOfBlockO;
                } else {
                    // nieuwe nachtshift nodig
                    startPart = Day.indexOfBlockS + 1;
                    stopPart = p.getDay(0).getBlocks().size() - 1;
                }

                currentBlock = stopPart;
                currentDay = shippingDay.getId();
                while (!itemPlaced && count < MAX_ADDSHIPPINGPROD_TRIES) {
                    if (i == 0 || (i == 1 && p.getDay(currentDay).hasNightShift())
                            || (i == 2 && !p.getDay(currentDay).hasNightShift())
                            || (i == 3 && !p.getDay(currentDay).hasNightShift())) {
                        // ofwel overdag || ofwel is er nighshift || ofwel overtime als er geen
                        // nightshift is
                        if (currentBlock == startPart - 1) {
                            if (currentDay == 0) {
                                break; // begin van planning
                            } else {
                                currentBlock = stopPart;
                                currentDay--; // begin van dag
                            }
                        }

                        for (int m = 0; m < p.getMachines().size(); m++) { // alle machines afgaan
                            currentMachine = p.getMachines().get(m);

                            if (p.getDay(currentDay).getBlock(currentBlock)
                                    .getMachineState(currentMachine) instanceof Idle) {
                                Item previousItem = currentMachine.getPreviousItem(p, currentDay, currentBlock);
                                if (previousItem.getId() != randomItem.getId()) {
                                    if (setupBeforeNewItem(previousItem, randomItem, currentDay, currentBlock,
                                            currentMachine, p, 1, false)) {
                                        p.getDay(currentDay).getBlock(currentBlock).setMachineState(currentMachine,
                                                new Production(randomItem));
                                        randomItem.updateItem(p.getDay(currentDay), currentMachine);
                                        itemPlaced = true;
                                        i = 99999999;
                                        break;
                                    }
                                }

                            }
                        }
                        currentBlock--;
                    }

                    if (i == 3 && !p.getDay(currentDay).hasNightShift()) {
                        controlNewNightShift(p, currentDay, currentBlock);
                    }

                    count++;

                }
            }
        }
        return false;
    }
}
