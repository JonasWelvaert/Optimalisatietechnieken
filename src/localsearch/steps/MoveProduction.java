package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class MoveProduction extends LocalSearchStep {
    public MoveProduction(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
