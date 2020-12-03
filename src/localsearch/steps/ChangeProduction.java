package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class ChangeProduction extends LocalSearchStep {
    public ChangeProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
