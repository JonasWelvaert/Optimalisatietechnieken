package solver;

import java.io.IOException;

import feasibilitychecker.FeasibiltyChecker;
import model.Planning;

public class SteepestDescentSolver extends Solver {
	private int iterations;

	public SteepestDescentSolver(int iterations, FeasibiltyChecker feasibiltyChecker) {
		super(feasibiltyChecker);
		this.iterations = iterations;
	}

	@Override
	public Planning optimize(Planning initialPlanning) throws IOException {
		Planning best = new Planning(initialPlanning);
		double bestCost = best.getTotalCost();

		Planning current = initialPlanning;

		for (int i = iterations; i > 0; i--) {
			if(Thread.currentThread().isInterrupted()) {
				return null;
			}
			logger.info("\t i = " + i + "\tCost: " + best.getTotalCost());
			Planning neighbor;
			current.logCostsToCSV(i);
			do {
				neighbor = new Planning(current);
				localSearch(neighbor);
			} while (!feasibiltyChecker.checkFeasible(neighbor));

			double neighborCost = neighbor.getTotalCost();

			if (neighborCost > bestCost) {
				best = new Planning(neighbor);
				bestCost = neighborCost;
				if (best.getTotalCost() == 0) {
					return best;
				}
			}
		}
		logger.info(feasibiltyChecker.getEc().toString());
		return best;
	}
}
