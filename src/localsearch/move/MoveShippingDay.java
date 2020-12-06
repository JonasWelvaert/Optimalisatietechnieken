package localsearch.move;

import localsearch.LocalSearchStep;
import model.Day;
import model.Planning;
import model.Request;

import java.util.ArrayList;
import java.util.List;

public class MoveShippingDay extends LocalSearchStep {

    public MoveShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
       /* // alle requesten met shipping day
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
            // als maar 1 shipping day niet wijzigen
            if (randomRequest.getPossibleShippingDays().size() >= 2) {
                // random day van alle mogelijke shippingdagen
                int randomPossibleShippingDay = random.nextInt(randomRequest.getPossibleShippingDays().size());
                Day newShippingDay = randomRequest.getPossibleShippingDays().get(randomPossibleShippingDay);
                // shipping day verwijderen
                randomRequest.removeShippingDay();
                // nieuwe shipping day toewijzen
                randomRequest.setShippingDay(newShippingDay);
            }
        } else {
            planShippingDay(p);
        }
        System.out.println("MoveShippingDay is in maintenance");
        return false;*/
    	return false;
    }
}
