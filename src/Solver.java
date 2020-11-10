import model.Planning;

public class Solver {

	public static Planning localSearch(Planning optimizedPlanning) {
		// TODO: Elke
		// some localsearch thing.

		// TODO: hier al de cost wijzigen per wijziging

		return null;/*
					 * dns × pn + to × po + SOM r∈V SOM i∈I (q i r × ci) + SOM d∈D (ud ×
					 * ps) + dp × pp
					 */
	}

	public static boolean checkFeasible(Planning planning) {
		// TODO: Nick
		return true;
	}

	private static int evaluate(Planning planning) {
		// TODO: Romeo
		return 0;
	}

	public static Planning optimize(long seed, long timeLimit, Planning initialPlanning) {

		Planning optimizedPlanning = new Planning(initialPlanning);
		
		// Jonas: om errors te vermijden
		boolean copyDebug = true;
		if (copyDebug) {
			return optimizedPlanning; 
		}

		// int previousCost = initialPlanning.getCost();

		int cost;
		int solution = 0;
		int teller = 0;
		while (teller < 1000) {
			Planning newPlanning = localSearch(optimizedPlanning);
			if (optimizedPlanning.getCost() > newPlanning.getCost()) {
				optimizedPlanning = newPlanning;
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
