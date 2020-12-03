package localsearch.other;

import localsearch.LocalSearchStep;
import model.Planning;

/**
 * Will convert next planning p1 to p2
 * p1: 1    1   1   1   1   s12     2   2   2   s23     s31      1   1   1
 * p2: 1    1   1   1   1   s12     2   2   2   s31     IDLE     1   1   1
 */
public class JoinNeighbouringSetups extends LocalSearchStep {


    public JoinNeighbouringSetups(int maxTries) {
        super(maxTries);
    }

    @Override
    public boolean execute(Planning p) {

        return false;
    }
}
