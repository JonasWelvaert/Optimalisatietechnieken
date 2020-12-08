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


    /**
     * @param p planning
     * @return true if a new request is shipped OR all request were already shipped, false if no new request could be shipped
     */
    @Override
    public boolean execute(Planning p) {
        Request request = p.getUnshippedRequest();

        // IF REQUEST IS NULL, ALL REQUEST ARE ALREADY SHIPPED
        if (request != null) {
            List<Day> possibleShippingDays = new ArrayList<>(request.getPossibleShippingDays());
            Collections.reverse(possibleShippingDays);

            // 1. CHECK FOR ALL POSSIBLE SHIPPING DAYS IF SHIPPING POSSIBLE FOR REQUEST
            for (Day sd : request.getPossibleShippingDays()) { // REVERSE LOOP OVER SHIPPING DAYS (START WITH LAST)
                if (tryToPlanShippingDay(sd, request, p)) {
                    return true;
                }
            }

            // 2. NO SHIPPING DAY COULD BE PLANNED
            for (Day sd : request.getPossibleShippingDays()) { // FORWARD LOOP OVER SHIPPING DAYS (START WITH FIRST)
                Map<Item, Integer> itemsNeeded = checkFutureStock(p, request, sd);
                boolean allItemsPlanned = true;

                if (itemsNeeded != null) {
                    // TRY TO PLAN NEEDED AMOUNT OF ITEMS FOR EVERY ITEM NEEDED
                    for (Map.Entry<Item, Integer> entry : itemsNeeded.entrySet()) {
                        boolean itemPlanned = false;
                        Item item = entry.getKey();
                        int amount = entry.getValue();

                        for (Machine m : p.getMachines()) {
                            if (addSpecificProductionForItem(m, p, item, amount)) {
                                itemPlanned = true;
                                break;
                            }
                        }
                        if (!itemPlanned)
                            allItemsPlanned = false; // IF 1 item could not be entirely planned, allItemsPlanned = false
                    }
                    // TRY TO PLAN SHIPPING ON DAY SD AGAIN
                }
                if (allItemsPlanned && tryToPlanShippingDay(sd, request, p)) {
                    return true;
                }
            }
        } else {
            logger.info("All request are already scheduled");
            return true;
        }
        return false;
    }

    /**
     * This method will check if stock constraint are not violated for shipping request on sd
     *
     * @param sd      shipping day on which we want to ship
     * @param request request we want to ship
     * @param p       planning
     * @return true if shipping is planned, false if shipping could not be planned
     */
    private boolean tryToPlanShippingDay(Day sd, Request request, Planning p) {
        Map<Item, Integer> itemsNeeded = checkFutureStock(p, request, sd);
        if (itemsNeeded == null) { // ... SO CHECK FOR SD
            request.setShippingDay(sd);
            for (Item i : request.getItemsKeySet()) {
                int delta = -1 * request.getAmountOfItem(i);
                p.updateStockLevels(sd, i, delta);
            }
            return true; //EOF method
        }
        return false;
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
                if (temp < 0) { //TODO check max stock
                    int temp2 = Math.abs(temp); //items te weinig
                    itemsNeeded.put(i, temp2);
                }
            }
        }
        if (itemsNeeded.isEmpty()) {
            itemsNeeded = null;
        }
        return itemsNeeded;
    }
}