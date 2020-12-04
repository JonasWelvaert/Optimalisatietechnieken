package solver;

import feasibilitychecker.FeasibiltyChecker;
import localsearch.LocalSearchStepFactory;
import model.Day;
import model.Planning;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static localsearch.EnumLocalSearchStep.*;


public abstract class Solver {
    protected static final Logger logger = Logger.getLogger(Solver.class.getName());
    protected final FeasibiltyChecker feasibiltyChecker;
    private static final Random random = new Random();
    private static int localSearchUpperBound = 100;

    public Solver(FeasibiltyChecker feasibiltyChecker) {
//        logger.setLevel(Level.OFF);
        this.feasibiltyChecker = feasibiltyChecker;

    }

    public abstract Planning optimize(Planning initialPlanning) throws IOException;


    public static Planning localSearch(Planning p) throws IOException {
        LocalSearchStepFactory lssf = new LocalSearchStepFactory();


        int randomInt = random.nextInt(localSearchUpperBound);
        int switcher = 0;

        if (randomInt < (switcher += 50)) {
            lssf.getLocalSearchStep(ADD_SINGLE_PRODUCTION).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(MOVE_MAINTENANCE).execute(p);
        } else if (randomInt < (switcher += 10)) {
            System.out.println();
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(ADD_PRODUCTION_FOR_SHIPPING).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(CHANGE_PRODUCTION).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(REMOVE_PRODUCTION).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(MOVE_PRODUCTION).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(MOVE_SHIPPING_DAY).execute(p);
        } else if (randomInt < (switcher += 0)) {
            lssf.getLocalSearchStep(ADD_SHIPPING_DAY).execute(p);
        } else {
            localSearchUpperBound = switcher;
        }

        p.calculateAllCosts();
        return p;
    }


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
                        int amountOfNightShifts = p.getMinConsecutiveDaysWithNightShift();
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
                        int amountOfNightShifts = p.getMinConsecutiveDaysWithNightShift();
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
