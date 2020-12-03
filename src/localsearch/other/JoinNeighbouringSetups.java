package localsearch.other;

import localsearch.LocalSearchStep;
import model.Planning;


public class JoinNeighbouringSetups extends LocalSearchStep {

    //WILL JOIN SETUPS LIKE THIS :     1 1 1 1 S12 2 2 2 (S23 S31) 1 1 1  --> 1 1 1 1 S12 2 2 2 (Idle S21) 1 1 1

    public JoinNeighbouringSetups(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {

        return false;
    }
}
