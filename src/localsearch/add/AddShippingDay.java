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
    public int counter =0;

    @Override
    public boolean execute(Planning p) {
        int randomRequest = random.nextInt(p.getRequests().getRequests().size());
        Request request = p.getRequests().get(randomRequest);



        if (request.getShippingDay() == null) {
            // CHECK FOR ALL POSSIBLE SHIPPING DAYS
            for (Day sd : request.getPossibleShippingDays()) {

                boolean isPossible=true;
                // FOR SD CHECK IF IN FUTURE STOCK IS NOT VIOLATED
                successorDays: for (Day d : p.getSuccessorDaysInclusive(sd)) {
                    for (Item i : request.getItemsKeySet()) {
                        if (i.getStockAmount(d) - request.getAmountOfItem(i) < 0) {
                            isPossible = false;
                            break successorDays;
                        }
                    }
                }
                // IF STOCK NOT VIOLATED, PLAN SHIPMENT
                if (isPossible) {
                    counter=sd.getId();
                    //plan shipping day in
                    request.setShippingDay(sd);

                    for (Item i : request.getItemsKeySet()) {
                        int delta = -1 * request.getAmountOfItem(i);
                        p.updateStockLevels(sd, i, delta);
                    }
                    return true;
                }

            }
        }
        return false;
    }
}
