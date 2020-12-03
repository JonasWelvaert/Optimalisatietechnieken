package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class MoveShippingDay extends LocalSearchStep {
    public MoveShippingDay(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
