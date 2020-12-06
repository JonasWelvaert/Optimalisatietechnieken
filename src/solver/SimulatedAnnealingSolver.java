package solver;

import feasibilitychecker.FeasibiltyChecker;
import model.Planning;

import java.io.IOException;
import java.util.logging.Level;

public class SimulatedAnnealingSolver extends Solver {
    private final double temperature;

    private double coolingFactor;

    public SimulatedAnnealingSolver(FeasibiltyChecker feasibiltyChecker, double temperature, double coolingFactor) {
        super(feasibiltyChecker);
        //logger.setLevel(Level.OFF);
        logger.info("Simulated Anealing solver made");
        this.temperature = temperature;
        this.coolingFactor = coolingFactor;
    }

    @Override
    public Planning optimize(Planning initialPlanning) throws IOException {
        Planning current = new Planning(initialPlanning);
        Planning best = initialPlanning;
        Planning neighbor;

        for (double t = temperature; t > 1; t *= coolingFactor) {
            double delta = best.getTotalCost() - initialPlanning.getTotalCost();
            logger.info("\t Temperature = " + t + "\t Cooling = " + coolingFactor + "\tCost: " + best.getTotalCost() + "\t delta: " + delta);
            current.logCostsToCSV(t);
            do {
                neighbor = new Planning(current);
                localSearch(neighbor);

            } while (!feasibiltyChecker.checkFeasible(neighbor));

            double neighborCost = neighbor.getTotalCost();
            double currentCost = current.getTotalCost();

            double probability;
            if (neighborCost < currentCost) {
                probability = 1;
            } else {
            	double temp = t * exponentialRegulator;
                probability = Math.exp((currentCost - neighborCost) / temp);
             }
            // ACCEPT SOMETIMES EVEN IF COST IS WORSE
            if (Math.random() < probability) {
                current = new Planning(neighbor);
            }
            // OVERWRITE BEST IF COST IS IMPROVED
            if (current.getTotalCost() < best.getTotalCost()) {
                best = new Planning(current);
                if (tempReset) {
                    t = temperature / 2;                              //TODO RESTART IF BETTER SOLUTION FOUND
                }
                if (changeCooling) {
                    coolingFactor += 0.0000009;
                }
                if (best.getTotalCost() == 0) {
                    return best;
                }
            }/* else if (current.getTotalCost() == best.getTotalCost()) {
                if (current.getStockAmount() > best.getStockAmount()) {
                    best = new Planning(current);
                }
            }*/
        }
        System.out.println(feasibiltyChecker.getEc().toString());
        return best;
    }
}
