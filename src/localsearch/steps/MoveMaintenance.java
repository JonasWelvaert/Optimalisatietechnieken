package localsearch.steps;

import localsearch.LocalSearchStep;
import model.Planning;

public class MoveMaintenance extends LocalSearchStep {
    public MoveMaintenance(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {
        return false;
    }
}
