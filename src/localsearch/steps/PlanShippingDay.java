package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class PlanShippingDay extends LocalSearchStep {
    public PlanShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
