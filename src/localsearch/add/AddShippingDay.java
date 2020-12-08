package localsearch.add;

import localsearch.LocalSearchStep;
import main.Main;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.setup.Setup;

import java.util.*;

public class AddShippingDay extends LocalSearchStep {


    public AddShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        int randomRequest = random.nextInt(p.getRequests().getRequests().size());
        Request request = p.getRequests().get(randomRequest);

        if (request.getShippingDay() == null) {
            List<Day> possibleShippingDays = new ArrayList<>(request.getPossibleShippingDays());
            Collections.reverse(possibleShippingDays);

            // 1. CHECK FOR ALL POSSIBLE SHIPPING DAYS IF SHIPPING POSSIBLE FOR REQUEST

            for (Day sd : request.getPossibleShippingDays()) { // REVERSE LOOP OVER SHIPPING DAYS (START WITH LAST)
                Map<Item, Integer> itemsNeeded = checkFutureStock(p, request, sd);
                if (itemsNeeded == null) {
                    //TODO inplannen
                    return true;
                }
            }

            // 2. NO SHIPPING DAY COULD BE PLANNED
            for (Day sd : request.getPossibleShippingDays()) { // FORWARD LOOP OVER SHIPPING DAYS (START WITH FIRST)
                Map<Item, Integer> itemsNeeded = checkFutureStock(p, request, sd);
                boolean allItemsPlanned = true;
                // TRY TO PLAN NEEDED AMOUNT OF ITEMS FOR EVERY ITEM NEEDED
                for (Map.Entry<Item, Integer> entry : itemsNeeded.entrySet()) {
                    boolean itemPlanned = false;
                    Item item = entry.getKey();
                    double amountNeeded = entry.getValue(); // item amount

                    for (Machine m : p.getMachines()) {
                        int efficiency = m.getEfficiency(item);
                        if (efficiency != 0) {
                            int blocksNeeded = (int) Math.ceil(amountNeeded / m.getEfficiency(item));
                            List<Block> blocks = m.getConsecutiveBlocks(p, blocksNeeded);

                            if (blocks != null) {
                                //inplannen van setups, en production


                                itemPlanned = true;
                                break;
                            }
                        }
                    }
                    if (!itemPlanned) allItemsPlanned = false;
                }

                // ALL ITEMS COULD BE PLANNED ...
                if (allItemsPlanned) {
                    // ... BUT COULD BE PLANNED AFTER SD ...
                    if (checkFutureStock(p, request, sd) != null) { // ... SO CHECK FOR SD
                        request.setShippingDay(sd);
                        for (Item i : request.getItemsKeySet()) {
                            int delta = -1 * request.getAmountOfItem(i);
                            p.updateStockLevels(sd, i, delta);
                        }
                        return true; //EOF method
                    }
                }

            } //EOF for (Day sd : request.getPossibleShippingDays())
        }// EOF   if (request.getShippingDay() == null)
        return false; //EOF    public boolean execute(Planning p)
    }


    /**
     * @param p       is planning
     * @param request request to be scheduled
     * @param sd      shipping day to be evaluated for stock levels
     * @return null if stock levels not violated, else Map of needed amount per item
     */
    private Map<Item, Integer> checkFutureStock(Planning p, Request request, Day sd) {
        // TODO isPossible = checkFeasibilityChecker...
        Map<Item, Integer> itemsNeeded = new HashMap<>();
        // FOR SD CHECK IF IN FUTURE STOCK IS NOT VIOLATED
        for (Day d : p.getSuccessorDaysInclusive(sd)) {
            for (Item i : request.getItemsKeySet()) {
                int temp = i.getStockAmount(d) - request.getAmountOfItem(i);
                if (temp < 0) {
                    int temp2 = Math.abs(temp); //items te weinig
                    itemsNeeded.put(i, temp2);
//                    return false;
                }
            }
        }
        if (itemsNeeded.isEmpty()) {
            itemsNeeded = null;
        }
        return itemsNeeded;

    }
    //EOF class
}