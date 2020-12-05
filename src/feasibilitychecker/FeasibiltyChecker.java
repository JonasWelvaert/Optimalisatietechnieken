package feasibilitychecker;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.LargeSetup;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FeasibiltyChecker {
    private static final Logger logger = Logger.getLogger(FeasibiltyChecker.class.getName());

    private Counting ec;

    public FeasibiltyChecker() {
        logger.setLevel(Level.OFF);
        ec = new Counting();
    }

    public boolean checkFeasible(Planning planning) {

        int teller1 = 0;

        if (!isConsecutiveInNightShiftsPast(planning)) {

            logger.warning("isConsecutiveInNightShiftsPast");
            ec.increaseIsConsecutiveInNightShiftsPast();
            return false;
        }

        for (int d = 0; d < Planning.getNumberOfDays(); d++) {

            List<String> setupTypes = new ArrayList<>();
            Map<String, Integer> setupMap = new HashMap<>();

            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {

                if (!checkNighShiftBlocksConstraints(b, d, planning)) {
                    ec.increaseCheckNighShiftBlocksConstraints();
                    logger.warning("checkNighShiftBlocksConstraints");
                    return false;
                }

                if (!checkOvertimeConstraints(teller1, b, d, planning)) {
                    logger.warning("checkOvertimeConstraints");
                    ec.increaseCheckOvertimeConstraints();
                    return false;
                }

                int parallelTeller = 0;

                for (Machine m : planning.getMachines()) {

                    MachineState state = planning.getDay(d).getBlock(b).getMachineState(m);

                    // adding setup types to a list -> duplicates are allowed!
                    // the list will be used for counting the occurrences of a setup type
                    if (state instanceof Setup) {
                        Item i1 = ((Setup) state).getFrom();
                        Item i2 = ((Setup) state).getTo();
                        setupTypes.add(String.valueOf(i1.getId()) + String.valueOf(i2.getId()));
                        setupMap.put(String.valueOf(i1.getId()) + String.valueOf(i2.getId()), i1.getSetupTimeTo(i2));
                    }

                    // check that only 1 maintenance/large setup is scheduled at any block of a day
                    // -> no parallel maintenance/large setup
                    if (state instanceof LargeSetup || state instanceof Maintenance) {
                        parallelTeller++;
                        if (parallelTeller > 1) {
                            logger.warning("Parallel planned !!!");
                            return false;
                        }
                    }

                    if (d < Planning.getNumberOfDays() && b < Day.getNumberOfBlocksPerDay() - 1) {
                        if (!checkProductionConstraints(m, b, d, planning)) {
                            ec.increaseCheckProductionConstraints();
                            logger.warning("checkProductionConstraints");
                            return false;
                        }
                    }
                }
            }
            if (!checkStockConstraints(planning.getDay(d), planning)) {
                ec.increaseCheckStockConstraints();
                logger.warning("checkStockConstraints \t\t");
                return false;
            }

            if (!checkSetupTypeConstraint(setupTypes, setupMap)) {
                logger.warning("checkSetupTypeConstraint");
                ec.increaseCheckSetupTypeConstraint();
                return false;
            }

/*if (!checkShippingDayConstraints(d, planning)) {
                logger.warning("checkShippingDayConstraints");
                ec.checkShippingDayConstraints++;
                return false;
            }
*/

        }

        for (Machine m : planning.getMachines()) {

            if (!checkSetupConstraint(m, planning)) {
                logger.warning("checkSetupConstraint");
                return false;
            }

            for (int d = 0; d < Planning.getNumberOfDays(); d++) {
                if (!checkChangeOverAndMaintenanceBoundaryConstraints(d, m, planning)) {
                    logger.warning("checkChangeOverAndMaintenanceBoundaryConstraints");
                    ec.increaseCheckChangeOverAndMaintenanceBoundaryConstraints();
                    return false;
                }
                if (!checkMaintenanceConstraints(d, m, planning)) {
                    logger.warning("checkMaintenanceConstraints");
                    ec.increaseCheckMaintenanceConstraints();
                    return false;
                }
            }
        }

        for (Item i : planning.getStock().getItems()) {
            i.checkStock();
        }
        return true;
    }

    // check if the minimum amount of consecutive night shifts is fulfilled when
    // their are past consecutive days
    private boolean isConsecutiveInNightShiftsPast(Planning p) {
        if (p.getPastConsecutiveDaysWithNightShift() > 0) {
            for (int i = 0; i < (p.getMinConsecutiveDaysWithNightShift()
                    - p.getPastConsecutiveDaysWithNightShift()); i++) {
                if (!p.getDay(i).hasNightShift()) {
                    return false;
                }
            }
        }
        return true;
    }

    // check that their is no maintenance/large setup between 0-b_e and b_l-b_n
    private boolean checkNighShiftBlocksConstraints(int b, int d, Planning planning) {
        if (b < Day.indexOfBlockE || b > Day.indexOfBlockL) {
            for (Machine m : planning.getMachines()) {
                if (planning.getDay(d).getBlock(b).getMachineState(m) instanceof LargeSetup
                        || planning.getDay(d).getBlock(b).getMachineState(m) instanceof Maintenance) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkOvertimeConstraints(int teller, int b, int d, Planning p) {

        if (p.getDay(d).hasNightShift()) {

            teller++; // TODO teller opgehoogd, maar niet meer gebruikt ?
            // check that the minimum amount of consecutive days with night shift is
            // fulfilled
            if (d > (p.getMinConsecutiveDaysWithNightShift()
                    - p.getPastConsecutiveDaysWithNightShift())) {
                // for (int l = d; l < (d + Planning.getMinConsecutiveDaysWithNightShift());
                // l++) {
                for (int l = d; l < Planning.getNumberOfDays(); l++) {
                    if (!p.getDay(l).hasNightShift()) {
                        return false;
                    }
                }
            }
        } else {

            // check that the night shift days are consecutive
            if (teller > 0 && teller < p.getMinConsecutiveDaysWithNightShift()) {
                return false;
            } else {
                teller = 0; // TODO teller op null gezet maar niet meer gebruikt ?
            }

            // check that the overtime blocks are consecutive
            if (b > Day.indexOfBlockS && b < Day.indexOfBlockO && !isConsecutiveInOvertime(b, d, p)) {
                return false;
            }

            // check that their is no overtime after block b_o for all machines
            if (b > Day.indexOfBlockO) {
                for (Machine m : p.getMachines())
                    if (!(p.getDay(d).getBlock(b).getMachineState(m) instanceof Idle)) {
                        return false;
                    }
            }
        }
        return true;
    }

    private boolean isConsecutiveInOvertime(int b, int d, Planning planning) {

        for (Machine m : planning.getMachines())
            if (planning.getDay(d).getBlock(b + 1).getMachineState(m) instanceof Production
                    || planning.getDay(d).getBlock(b + 1).getMachineState(m) instanceof SmallSetup) {
                if (!(planning.getDay(d).getBlock(b).getMachineState(m) instanceof Production
                        || planning.getDay(d).getBlock(b).getMachineState(m) instanceof SmallSetup)) {
                    return false;
                }
            }
        return true;
    }

    // check that for each day only 1 setup of a certain type is scheduled
    private boolean checkSetupTypeConstraint(List<String> setupTypes, Map<String, Integer> setupMap) {

        Map<String, Integer> hm = new HashMap<>();

        // setup type is the key -> only 1 value possible
        // key = setup type
        // value = number of occurrences of set up types
        for (String setUpTime : setupTypes) {
            Integer j = hm.get(setUpTime);
            hm.put(setUpTime, (j == null) ? 1 : j + 1);
        }

        // if number of set up types > set up time -> return false
        for (Map.Entry<String, Integer> setupType : hm.entrySet()) {
            int setupTime = setupMap.get(setupType.getKey());
            int setupOccurrences = setupType.getValue();
            if (setupOccurrences > setupTime) {
                return false;
            }
        }

        return true;
    }

    // check that the production of an item is only possible after the right
    // configuration
    // only possible if previous block is changeover to item or previous block is
    // production of same item
    private boolean checkProductionConstraints(Machine m, int b, int d, Planning planning) {

        MachineState state1 = planning.getDay(d).getBlock(b).getMachineState(m);
        MachineState state2 = planning.getDay(d).getBlock(b + 1).getMachineState(m);

        if (state1 instanceof Production && state2 instanceof Production) {
            if (((Production) state1).getItem().getId() != ((Production) state2).getItem().getId()) {
                return false;
            }
        }
        if (state1 instanceof Setup && state2 instanceof Production) {
            return ((Setup) state1).getTo().getId() == ((Production) state2).getItem().getId();
        }
        if (state2 instanceof Setup && state1 instanceof Production) {
            return ((Setup) state2).getFrom().getId() == ((Production) state1).getItem().getId();
        }
        return true;
    }

    private boolean checkSetupConstraint(Machine m, Planning p) {

        Item currentItem = m.getInitialSetup();
        for (int d = 0; d < Planning.getNumberOfDays(); d++) {
            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {
                if (p.getDay(d).getBlock(b).getMachineState(m) instanceof Production) {
                    Production production = (Production) p.getDay(d).getBlock(b).getMachineState(m);
                    if (production.getItem().getId() != currentItem.getId()) {
                        return false;
                    }
                } else if (p.getDay(d).getBlock(b).getMachineState(m) instanceof Setup) {
                    Setup s = (Setup) p.getDay(d).getBlock(b).getMachineState(m);
                    Item to = s.getTo();
                    Item from = s.getFrom();
                    int lengthsetup = from.getSetupTimeTo(to);
                    if (s.getFrom().getId() != currentItem.getId()) {
                        return false;
                    } else {
                        currentItem = s.getTo();
                    }
                    b = b + lengthsetup;
                }
            }
        }

        return true;
    }

    private boolean checkStockConstraints(Day day, Planning planning) {
        int stockAmount = 0, maxStockAmount = 0;
        for (Item i : planning.getStock().getItems()) {
            stockAmount = i.getStockAmount(day);
            maxStockAmount = i.getMaxAllowedInStock();
            // check that the stock level on day d is less than the max allowed of stock of
            // an item
            if (stockAmount > maxStockAmount) {
                return false;
            }

            // check that the stock level on day d is 0 when the stock level is below the
            // minimum amount
            /*
             * if (i.getStockAmount(day) < i.getMinAllowedInStock() && i.getStockAmount(day)
             * != 0) { return false; }
             */

            if (stockAmount < 0) {
                return false;
            }

        }

        return true;
    }

    // check that the minimum amount of maintenances are scheduled in the time
    // horizon
    private boolean checkMaintenanceConstraints(int d, Machine m, Planning planning) {

        int init = m.getInitialDaysPastWithoutMaintenance();
        int max = m.getMaxDaysWithoutMaintenance();
        int now = max - init;
        int maintenanceBlockTeller = 0;
        int maintenanceDayTeller = 0;
        int duration = 0;
        if ((d - now) == 0 || ((d - now) % (max + 1)) == 0) {
            for (int b = 0; b < Day.getNumberOfBlocksPerDay(); b++) {
                if (planning.getDay(d).getBlock(b).getMachineState(m) instanceof Maintenance) {
                    duration = m.getMaintenanceDurationInBlocks();
                    maintenanceBlockTeller++;
                    if (maintenanceBlockTeller == duration) {
                        return true;
                    }
                } else {
                    maintenanceBlockTeller = 0;
                }
            }
            return false;
        }

        return true;

    }

    // check that no production is done in between blocks of changeover and blocks
    // of maintenance
    public boolean checkChangeOverAndMaintenanceBoundaryConstraints(int d, Machine m, Planning planning) {

        int setupTeller = 0;
        int maintenanceTeller = 0;
        int setupTime = 0;
        int maintenanceTime = 0;
        Item i1 = null;
        Item i2 = null;
        for (int j = 0; j < Day.getNumberOfBlocksPerDay(); j++) {
            MachineState state = planning.getDay(d).getBlock(j).getMachineState(m);

            // maintenance/idle is allowed in between setup blocks and setup/idle is allowed
            // in between maintenance blocks
            if (state instanceof Setup || state instanceof Idle || state instanceof Maintenance) {

                // machine state is in setup configuration
                if (state instanceof Setup) {
                    Setup setup = (Setup) planning.getDay(d).getBlock(j).getMachineState(m);

                    // check if the setup is still the same (S1_2 -> S2_3)
                    if (i1 == null || !(i1.getId() == (setup.getFrom().getId()) || !(i2.getId() == setup.getFrom().getId()))) {

                        if (setupTeller > 0 && setupTeller <= setupTime) {
                            return false;
                        }

                        // get initial setup
                        i1 = setup.getFrom();
                        i2 = setup.getTo();
                        setupTime = i1.getSetupTimeTo(i2);
                        setupTeller++;

                    } else {
                        // setup is still the same -> teller increasing
                        setupTeller++;

                        // setup time is exceeded -> restart
                        if (setupTeller == setupTime) {
                            setupTeller = 0;
                            setupTime = 0;
                            i1 = null;
                            i2 = null;
                        }
                    }
                    // machine state is in maintenance configuration
                } else if (state instanceof Maintenance) {

                    Maintenance maintenance = (Maintenance) planning.getDay(d).getBlock(j).getMachineState(m);

                    // first occurrence of maintenance
                    if (maintenanceTeller == 0) {
                        maintenanceTime = m.getMaintenanceDurationInBlocks();
                    }

                    maintenanceTeller++;

                    // stop counting if maintenance time is exceeded
                    if (maintenanceTeller == maintenanceTime) {
                        maintenanceTeller = 0;
                        maintenanceTime = 0;
                    }
                }

                // block is in production -> check if the block is not between setup's or
                // maintenance's
            } else if ((setupTeller > 0 && setupTeller <= setupTime)
                    || (maintenanceTeller > 0 && maintenanceTeller <= maintenanceTime)) {
                return false;

                // restart
            } else {
                setupTeller = 0;
                setupTime = 0;
                maintenanceTeller = 0;
                maintenanceTime = 0;

            }
        }

        return true;

    }

    private boolean checkShippingDayConstraints(int d, Planning planning) {
        // check every request whether the amount produced/in-stock is sufficient
        for (Request request : planning.getRequests()) {

            // if day d is a shipping day
            if (request.getShippingDay() != null) {
                if (request.getShippingDay().getId() == planning.getDay(d).getId()) {

                    // if day d not in possible shipping days
                    if (!request.getPossibleShippingDays().contains(planning.getDay(d))) {
                        return false;
                    }

                    // check if the amount for each item in the request is fulfilled
                    for (Item i : request.getItemsKeySet()) {
                        int amount = 0;

                        // amount in the stock
                        amount = planning.getStock().getItem(i.getId()).getStockAmount(planning.getDay(d));

                        for (Machine m : planning.getMachines()) {
                            // check the amount produced on the last block in the day for every machine
                            int lastBlockOfProduction = 0;
                            for (int j = 0; j < Day.getNumberOfBlocksPerDay(); j++) {
                                if (planning.getDay(d).getBlock(j).getMachineState(m) instanceof Production) {
                                    lastBlockOfProduction = planning.getDay(d).getBlock(j).getId();
                                }
                            }
                            if (planning.getDay(d).getBlock(lastBlockOfProduction)
                                    .getMachineState(m) instanceof Production) {
                                Production production = (Production) planning.getDay(d).getBlock(lastBlockOfProduction)
                                        .getMachineState(m);
                                if (production.getItem().getId() == i.getId()) {
                                    amount += production.getItem().getStockAmount(planning.getDay(d));
                                }
                            }
                        }
                        if (amount < request.getAmountOfItem(i)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public Counting getEc() {
        return ec;
    }
}
