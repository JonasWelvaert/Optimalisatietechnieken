package solver;

import feasibilitychecker.FeasibiltyChecker;
import model.Planning;

import java.io.IOException;

public class SimulatedAnnealingSolver extends Solver {
    private final double temperature;
    private final double coolingFactor;


    public SimulatedAnnealingSolver(FeasibiltyChecker feasibiltyChecker, double temperature, double coolingFactor) {
        super(feasibiltyChecker);
        logger.info("Simulated Anealing solver made");
        this.temperature = temperature;
        this.coolingFactor = coolingFactor;
    }

    @Override
    public Planning optimize(Planning initialPlanning) throws IOException {
        Planning current = new Planning(initialPlanning);
        Planning best = initialPlanning;
        Planning neighbor;
        int stockRiseLevel = 0;

        for (double t = temperature; t > 1; t *= coolingFactor) {
            logger.info("\t \t \t \t \t \t Temperature = " + t);
            current.calculateAllCosts();
            current.logCostsToCSV(t);
            do {
                neighbor = new Planning(current);
                localSearch(neighbor);
//                Main.printOutputToFile("testing.txt",neighbor);

            } while (!feasibiltyChecker.checkFeasible(neighbor));

            double neighborCost = neighbor.getTotalCost();
            double currentCost = current.getTotalCost();

            double probability;
            if (neighborCost < currentCost) {
                probability = 1;
            } else {
                probability = Math.exp((currentCost - neighborCost) / t);
            }
            // ACCEPT SOMETIMES EVEN IF COST IS WORSE
            if (Math.random() < probability) {
                current = new Planning(neighbor);
            }
            // OVERWRITE BEST IF COST IS IMPROVED
            if (current.getTotalCost() < best.getTotalCost()) {
                best = new Planning(current);
                if (best.getTotalCost() == 0) {
                    return best;
                }
            } else if (current.getTotalCost() == best.getTotalCost()) {
                if (current.getStockAmount() > best.getStockAmount()) {
                    best = new Planning(current);
                }
            }
        }
        System.out.println(feasibiltyChecker.getEc().toString());
        return best;
    }
}
