package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class RemoveProduction extends LocalSearchStep {
    public RemoveProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
