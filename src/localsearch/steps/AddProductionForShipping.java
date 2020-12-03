package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class AddProductionForShipping extends LocalSearchStep {

    public AddProductionForShipping(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
