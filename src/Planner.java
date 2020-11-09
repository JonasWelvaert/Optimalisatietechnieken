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
//		System.out.println("\t Romeo Permentier");
        System.out.println("\tNick Braeckman");
        System.out.println("\tElke Govaert");


        // 1. Initiele oplossing
        days = optimize();

        // 2.
        boolean feasible = checkFeasible(days);




        ReadFileIn();


        if (feasible) {
            evaluate(days);
        }
        // metaheuristik();

        // output();
    }

    private static List<Day> optimize() {

        return new ArrayList<Day>();
    }

    private static boolean checkFeasible(List<Day> days) {

        return true;
    }

    private static void evaluate(List<Day> solution) {


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
        return 1;/*dns × pn + to × po +
		X
		r∈V
		X
		i∈I
		(q
		i
		r × ci) + X
		d∈D
		(ud × ps) + dp × pp*/
    }

}
