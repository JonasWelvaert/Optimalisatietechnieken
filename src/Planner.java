import java.util.ArrayList;
import java.util.List;

import model.Day;
import model.Machine;

public class Planner {
	private static List<Machine> machines;
	private static List<Day> days;

	public static void main(String[] args) {
		System.out.println("Hello world!");
		System.out.println("#Verwijder jouw lijn als je kan bewerken op GIT.");
		System.out.println("\t Romeo Permentier");
		System.out.println("\tNick Braeckman");
		System.out.println("\tElke Govaert");

		ReadFileIn();

		// metaheuristik();

		// output();
	}

	private static void ReadFileIn() {

		machines = new ArrayList<>();
		machines.add(new Machine());
		machines.add(new Machine());
		// days.get(2).getblock(7).getMachineStatus(machines.get(2));
	}

	private static void metaHeuristic() {
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
	}
	private static int localSearch() {
		//some localsearch thing.
		return 1;//
	}

}
