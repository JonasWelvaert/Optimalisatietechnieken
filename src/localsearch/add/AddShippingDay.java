package localsearch.add;

import localsearch.LocalSearchStep;
import model.Day;
import model.Item;
import model.Planning;
import model.Request;

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

                isPossible = checkFutureStock(p, request, sd);

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

                    // 0. HOEVEEL ITEMS ZIJN ER TEKORT ?

                    // 1. PLAN ALLE ITEMS DIE TE KORT ZIJN ZO VROEG MOGELIJK

                    // 2a. alles is gepland voor huidige SD --> plan SD
                    if (checkFutureStock(p, request, sd)) {
                        //plan huidige SD
                    } else {
                        //niets
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


    private boolean checkFutureStock(Planning p, Request request, Day sd) {
        // FOR SD CHECK IF IN FUTURE STOCK IS NOT VIOLATED
        for (Day d : p.getSuccessorDaysInclusive(sd)) {
            for (Item i : request.getItemsKeySet()) {
                if (i.getStockAmount(d) - request.getAmountOfItem(i) < 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
