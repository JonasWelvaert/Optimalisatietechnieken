import model.Planning;

public class Solver {


    public static int localSearch() {
        // some localsearch thing.
        return 1;/*
         * dns × pn + to × po + SOM r∈V SOM i∈I (q i r × ci) + SOM d∈D (ud × ps) + dp ×
         * pp
         */
    }

    public static boolean checkFeasible(Planning planning) {
        //TODO
        return true;
    }

    private static int evaluate(Planning planning) {
        return 0;
    }

    public static Planning optimize(long seed, long timeLimit, Planning initialPlanning) {

        Planning optimizedPlanning = null;
        int cost;
        int solution = 0;
        int teller = 0;
        while (teller < 1000) {
            int value = localSearch();
            if (solution < value) {
                solution = value;
                teller = 0;
            } else {
                teller++;
            }
        }
        boolean feasible = checkFeasible(optimizedPlanning);
        if (feasible) {
            cost = evaluate(optimizedPlanning);
        }

        return optimizedPlanning;
    }

}
