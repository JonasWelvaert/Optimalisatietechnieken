package localsearch.add;

import localsearch.LocalSearchStep;
import model.Day;
import model.Item;
import model.Planning;
import model.Request;

public class PlanShippingDay extends LocalSearchStep {
    public PlanShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        for (Request request : p.getRequests().getRequests()) {
            if (request.getShippingDay() == null) {
                boolean containsAllItems = true;

                // CHECK FOR ALL POSSIBLE SHIPPING DAYS
                for (Day sd : request.getPossibleShippingDays()) {
                    for (Item i : request.getItems()) {
                        if (i.getStockAmount(sd) - request.getAmountOfItem(i) < 0) {
                            containsAllItems = false;
                        }
                    }
                    if (containsAllItems) {
                        //plan shipping day in
                        request.setShippingDay(sd);
                        for (Item i : request.getItems()) {

                            for (int d = sd.getId(); d < Planning.getNumberOfDays(); d++) {
                                Day day = p.getDay(d);
                                int newStockAmount = i.getStockAmount(day) - request.getAmountOfItem(i);
                                i.setStockAmount(day, newStockAmount);
                            }
                        }
                        return;
                        //break; //TODO also possible
                    }
                }
            }
        }
        return false;
    }
}
