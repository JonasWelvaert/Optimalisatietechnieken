package main;

import feasibilitychecker.FeasibiltyChecker;
import localsearch.LocalSearchStepFactory;
import model.Day;
import model.Planning;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static localsearch.EnumStep.*;


public class Solver {
    private static final Logger logger = Logger.getLogger(Solver.class.getName());
    public static final int SIMULATED_ANEALING = 100;
    private double SATemperature = 1000;
    private double SACoolingFactor = 0.995;
    private int mode;
    private final FeasibiltyChecker feasibiltyChecker;
    private static final Random random = new Random();
    private static int localSearchUpperBound = 100;

    public Solver(int mode, FeasibiltyChecker feasibiltyChecker) {
        logger.setLevel(Level.OFF);
        this.feasibiltyChecker = feasibiltyChecker;

        if (mode == 100) {
            this.mode = mode;
        } else {
            logger.warning("Optimize mode " + this.mode + " not found");
            throw new RuntimeException("Optimize mode not found in main.Solver constructor");
        }
    }

    public void setSimulatedAnealingFactors(double SATemperature, double SACoolingFactor) {
        //TODO JONAS: moet in solverklasse staan
        this.SACoolingFactor = SACoolingFactor;
        this.SATemperature = SATemperature;
    }

    public Planning optimize(Planning initialPlanning) throws IOException {
        if (mode == SIMULATED_ANEALING) {
            return optimizeUsingSimulatedAnealing(initialPlanning, SATemperature, SACoolingFactor);
        }
        // hier kunnen andere optimalisaties toegevoegd worden als deze niet goed
        // blijkt.
        throw new RuntimeException("Optimize mode not found in main.Solver.optimize()");
    }

    private Planning optimizeUsingSimulatedAnealing(Planning initialPlanning, double temperature, double coolingFactor)
            throws IOException {
        Planning current = new Planning(initialPlanning);
        Planning best = initialPlanning;
        Planning neighbor;
        int stockRiseLevel = 0;

        for (double t = temperature; t > 1; t *= coolingFactor) {
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
                probability = Math.exp((currentCost - neighborCost) / t);
            }
            // ACCEPT SOMETIMES EVEN IF COST IS WORSE
            if (Math.random() < probability) {
                current = new Planning(neighbor);
            }
            // ALSO ACCEPT SOLUTION IF STOKE ROSE MORE THAN stockRiseLevel
            else if (neighbor.getStockAmount() - stockRiseLevel > current.getStockAmount()) {
                current = new Planning(neighbor);
            }
            // OVERWRITE BEST IF COST IS IMPROVED
            if (current.getTotalCost() < best.getTotalCost()) {
                best = new Planning(current);
            }

        }
        System.out.println(feasibiltyChecker.getEc().toString());
        return best;
    }

    public static Planning localSearch(Planning p) throws IOException {
        /* ------------------------ ENKEL GETALLEN AANPASSEN VOOR GEWICHTEN AAN TE PASSEN ------------------------ */
        LocalSearchStepFactory lssf = new LocalSearchStepFactory();

        int randomInt = random.nextInt(localSearchUpperBound);
        int switcher = 0;

        if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(ADD_SINGLE_PRODUCTION).execute(p);
        } else if (randomInt < (switcher += 50)) {
            lssf.getLocalSearchStep(MOVE_MAINTENANCE).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(ADD_PRODUCTION_FOR_SHIPPING).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(CHANGE_PRODUCTION).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(REMOVE_PRODUCTION).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(MOVE_PRODUCTION).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(MOVE_SHIPPING_DAY).execute(p);//TODO
        } else if (randomInt < (switcher += 1)) {
            lssf.getLocalSearchStep(PLAN_SHIPPING_DAY).execute(p);//TODO
        } else {
            localSearchUpperBound = switcher; //TODO ROMEO dees is nog fout peisk
        }

        p.calculateAllCosts();
        return p;
    }


    //TODO ROMEO
    private static int closestNightshift(Planning p, String when, int day) {
        if (when.equals("before")) {
            int lastNightShiftDay = -1;
            for (int i = 0; i < p.getDays().size(); i++) {
                if (p.getDay(i).hasNightShift() && i < day) { // controleren tot als laatste dag van nightshiftreeks
                    lastNightShiftDay = i;
                } else {
                    break;
                }
            }
            return lastNightShiftDay;
        } else if (when.equals("after")) {
            int firstNightShiftDay = -1;
            for (int i = day + 1; i < p.getDays().size(); i++) {
                if (p.getDay(i).hasNightShift()) { // controleren tot eerste dag van nieuwe nightshift reeks
                    firstNightShiftDay = i;
                    break;
                }
            }
            return firstNightShiftDay;
        }
        return -1;
    }

    private static void controlNewNightShift(Planning p, int randomDay, int randomBlock) {
        int maxAmountDaysBetweenExtendingNightshift = 3;
        Day d = p.getDay(randomDay);
        if (!d.hasNightShift()) { // als er al nightshift is valt er niks te controleren
            if (randomBlock > Day.getIndexOfBlockO()) { // niet overtime, wel nachtshift
                int nightshiftBefore = closestNightshift(p, "before", randomDay);
                int nightshiftAfter = closestNightshift(p, "after", randomDay);
                // als nightshift niet lang geleden => beter verlengen, dan nieuwe starten
                /*
                 * if (nightshiftAfter == -1 && nightshiftBefore == -1) { // nieuwe nightshift
                 * reeks int amountOfNightShifts =
                 * Planning.getMinConsecutiveDaysWithNightShift(); for (int i = randomDay; i <
                 * p.getDays().size(); i++) { if (amountOfNightShifts == 0) { break; } else {
                 * p.getDay(i).setNightShift(true); amountOfNightShifts--; } } } else
                 */
                if (nightshiftBefore < nightshiftAfter) { // als dichtste nightshift ervoor ligt
                    if (nightshiftBefore <= maxAmountDaysBetweenExtendingNightshift) {
                        // alle dagen ervoor ook nighshift maken
                        for (int i = randomDay; i > randomDay - nightshiftBefore; i--) {
                            p.getDay(i).setNightShift(true);
                        }
                    } else {
                        // nieuwe nightshift reeks
                        int amountOfNightShifts = Planning.getMinConsecutiveDaysWithNightShift();
                        for (int i = randomDay; i < p.getDays().size(); i++) {
                            if (amountOfNightShifts == 0) {
                                break;
                            } else {
                                p.getDay(i).setNightShift(true);
                                amountOfNightShifts--;
                            }
                        }
                    }
                } else { // als dichtste nighshift erna ligt of ze zijn gelijk
                    if (nightshiftAfter <= maxAmountDaysBetweenExtendingNightshift) {
                        // alle dagen erna ook nightshift maken
                        for (int i = randomDay; i < randomDay + nightshiftAfter; i++) {
                            p.getDay(i).setNightShift(true);
                        }
                    } else {
                        // nieuwe nightshift reeks
                        int amountOfNightShifts = Planning.getMinConsecutiveDaysWithNightShift();
                        for (int i = randomDay; i < p.getDays().size(); i++) {
                            if (amountOfNightShifts == 0) {
                                break;
                            } else {
                                p.getDay(i).setNightShift(true);
                                amountOfNightShifts--;
                            }
                        }
                    }
                }
            }
        }
    }

}
