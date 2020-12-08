package localsearch.add;

import localsearch.LocalSearchStep;
import model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddShippingDay extends LocalSearchStep {

    public AddShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        int randomRequest = random.nextInt(p.getRequests().getRequests().size());
        Request request = p.getRequests().get(randomRequest);

        // TODO while loop ? while not shipped ?
        if (request.getShippingDay() == null) {
            // CHECK FOR ALL POSSIBLE SHIPPING DAYS
            for (Day sd : request.getPossibleShippingDays()) {

                boolean isPossible = true;

                // TODO isPossible = checkFeasibilityChecker...

               /*
                successorDays:
                for (Day d : p.getSuccessorDaysInclusive(sd)) {
                    for (Item i : request.getItemsKeySet()) {
                        if (i.getStockAmount(d) - request.getAmountOfItem(i) < 0) {
                            isPossible = false;
                            break successorDays;
                        }
                    }
                }*/

                Map<Item, Integer> itemsNeeded = checkFutureStock(p, request, sd);
                isPossible = itemsNeeded != null;

                // IF STOCK NOT VIOLATED, PLAN SHIPMENT
                if (isPossible) {
                    //plan shipping day in
                    request.setShippingDay(sd);

                    for (Item i : request.getItemsKeySet()) {
                        int delta = -1 * request.getAmountOfItem(i);
                        p.updateStockLevels(sd, i, delta);
                    }
                    return true;
                }
                // IF STOCK VIOLATED, SO PLAN EXTRA PRODUCTION (preferrably before current SD)
                else {
                    // ITEMS NEEDED != null

                    assert itemsNeeded != null;
                    for (Map.Entry<Item, Integer> entry : itemsNeeded.entrySet()) {

                        Item i = entry.getKey();
                        int amountNeeded = entry.getValue();
                        addProductionForItem(i, amountNeeded);
                    }


                    // IF EVERTHING COULD BE PLANNED BEFORE SD, ISPOSSIBLE BECOMES TRUE;
                    isPossible = checkFutureStock(p, request, sd) != null;
                    if (isPossible) {
                        request.setShippingDay(sd);
                        for (Item i : request.getItemsKeySet()) {
                            int delta = -1 * request.getAmountOfItem(i);
                            p.updateStockLevels(sd, i, delta);
                        }
                    }

                    // addProduction van items zodat het wel mogelijk wordt
                    // isPossibele ?
                    // true: schedule shipping day
                    // false: return
                }

            }
        }
        return false;
    }

    /**
     * @param i            type of item which has to be produced more
     * @param amountNeeded amount of items that needs to be planned of item i
     * @return true if total amount could be planned
     */
    private boolean addProductionForItem(Item i, int amountNeeded) {




        return false;
    }


    private Map<Item, Integer> checkFutureStock(Planning p, Request request, Day sd) {

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
}
