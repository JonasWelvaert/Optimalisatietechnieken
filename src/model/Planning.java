package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.istack.internal.Nullable;
import main.Main;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.LargeSetup;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.Main.*;

public class Planning {
    private static final Logger logger = Logger.getLogger(Planning.class.getName());

    private static int numberOfDays;
    private static int minConsecutiveDaysWithNightShift;
    private int pastConsecutiveDaysWithNightShift;
    private String instanceName;
    private List<Machine> machines;
    private List<Day> days;
    private List<Item> items;
    private Requests requests;
    private Stock stock;
    private double costNS;    //NS = night shift
    private double costOT; //OT = Over time
    private double costUR;    //UR = unscheduled request
    private double costSL;    //SL = Stock Level
    private double costDP;    //DP = Days Parallel


    public Planning(String instanceName, int nrOfMachines) {
        this.instanceName = instanceName;
        machines = new ArrayList<>(nrOfMachines);
        days = new ArrayList<>(numberOfDays);
        for (int i = 0; i < numberOfDays; i++) {
            days.add(new Day(i));
        }
        //TODO romeo set all max
        //cost = Integer.MAX_VALUE;
    }

    /**
     * Diepe copy
     *
     * @param p
     */
    public Planning(Planning p) {
        this.instanceName = p.instanceName;
        this.days = new ArrayList<>(p.days.size());
        for (Day d : p.days) {
            this.days.add(new Day(d));
        }
        this.stock = new Stock(p.getStock(), this.days);
        this.machines = new ArrayList<>(p.getMachines().size());
        for (Machine m : p.getMachines()) {
            this.machines.add(new Machine(m.getId(), this.stock.findItemWithId(m.getInitialSetup().getId()),
                    m.getInitialDaysPastWithoutMaintenance(), m.getMaxDaysWithoutMaintenance(),
                    m.getMaintenanceDurationInBlocks()));
        }
        for (Day d : days) {
            for (Block b : d) {
                for (Machine m : machines) {
                    MachineState ms = p.getDay(d.getId()).getBlock(b.getId())
                            .getMachineState(p.machines.get(m.getId()));
                    if (ms.getClass() == Production.class) {
                        b.setMachineState(m, new Production(stock.getItem(((Production) ms).getItem().getId())));
                    } else if (ms.getClass() == Maintenance.class) {
                        b.setMachineState(m, new Maintenance());
                    } else if (ms.getClass() == Idle.class) {
                        b.setMachineState(m, new Idle());
                    } else if (ms.getClass() == SmallSetup.class) {
                        b.setMachineState(m, new SmallSetup(stock.getItem(((Setup) ms).getFrom().getId()),
                                stock.getItem(((Setup) ms).getTo().getId())));
                    } else if (ms.getClass() == LargeSetup.class) {
                        b.setMachineState(m, new LargeSetup(stock.getItem(((Setup) ms).getFrom().getId()),
                                stock.getItem(((Setup) ms).getTo().getId())));
                    }
                }
            }
        }
        this.requests = new Requests();
        for (Request r : p.requests) {
            Request request = new Request(r.getId());
            if (r.getShippingDay() == null) {
                request.setShippingDay(null);
            } else {
                request.setShippingDay(days.get(r.getShippingDay().getId()));
            }
            for (Day d : r.getPossibleShippingDays()) {
                request.addPossibleShippingDay(days.get(d.getId()));
            }
            for (Item i : r.getItems()) {
                request.addItem(stock.getItem(i.getId()), r.getAmountOfItem(i));
            }
            this.requests.add(request);
        }
        this.costUR = p.costUR;
        this.costSL = p.costSL;
        this.costOT = p.costOT;
        this.costDP = p.costDP;
        this.costNS = p.costNS;

    }

    public void addMachine(Machine m) {
        machines.add(m);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public static int getNumberOfDays() {
        return numberOfDays;
    }

    public static void setNumberOfDays(int nrOfDays) {
        Planning.numberOfDays = nrOfDays;
    }

    public static int getMinConsecutiveDaysWithNightShift() {
        return minConsecutiveDaysWithNightShift;
    }

    public static void setMinConsecutiveDaysWithNightShift(int minConsecutiveDaysWithNightShift) {
        Planning.minConsecutiveDaysWithNightShift = minConsecutiveDaysWithNightShift;
    }

    public int getPastConsecutiveDaysWithNightShift() {
        return pastConsecutiveDaysWithNightShift;
    }

    public void setPastConsecutiveDaysWithNightShift(int pastConsecutiveDaysWithNightShift) {
        this.pastConsecutiveDaysWithNightShift = pastConsecutiveDaysWithNightShift;
    }

    public List<Machine> getMachines() {
        return machines;
    }

    public Day getDay(int id) {
        return days.get(id);
    }

    public List<Day> getDays() {
        return days;
    }

    public Requests getRequests() {
        return requests;
    }

    public void setRequests(Requests requests) {
        this.requests = requests;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }


    public int getAmountOfNightShiftsInNextPeriod() {
        int teller = 0;
        for (int d = numberOfDays - 1; d == 0; d++) {
            if (days.get(d).hasNightShift()) {
                teller++;
            }
        }
        //if(teller == 0) return 0; dit was overbodig

        return teller;
    }


    /* ------------------------- COSTS ------------------------- */
    public double getCostNS() {
        return costNS;
    }

    public void setCostNS(double costNS) {
        this.costNS = costNS;
//        logger.log(Level.INFO, "Cost of NS= " + String.valueOf(costNS));

    }

    //dns × pn (=COST_OF_NIGHTSHIFT)
    public void calculateNS() {
        int dns = 0;
        for (Day d : days) {
            if (d.hasNightShift()) {
                dns++;
            }
        }
        setCostNS(dns * COST_OF_NIGHT_SHIFT);
    }

    public double getCostOT() {
        return costOT;
    }

    public void setCostOT(double costOT) {
        this.costOT = costOT;
//        logger.log(Level.INFO, "Cost of OT= " + String.valueOf(costOT));
    }

    // to × po (=COST_OF_NIGHTSHIFT)
    public void calculateOT() {
        int to = 0;
        for (Day d : days) {
            to += d.getNumberOfOvertimeBlock(machines);
        }

        setCostOT(to * COST_OF_NIGHT_SHIFT);
    }

    public double getCostUR() {
        return costUR;
    }

    public void setCostUR(double costUR) {
        this.costUR = costUR;
//        logger.log(Level.INFO, "Cost of UR= " + String.valueOf(costUR));
    }

    //SOM r∈V SOM i∈I (q i r × ci)
    public void calculateUR() {
        double total = 0;
        double ci;
        int qir;
        for (Request req : requests) {
            if (!req.hasShippingDay()) {
                for (Map.Entry<Item, Integer> entry : req.getMap().entrySet()) {
                    ci = entry.getKey().getCostPerItem();
                    qir = entry.getValue();
                    total += (ci * qir);
                }
            }
        }
        setCostUR(total);
    }

    public double getCostSL() {
        return costSL;
    }

    public void setCostSL(double costSL) {
        this.costSL = costSL;
//        logger.log(Level.INFO, "Cost of SL= " + String.valueOf(costSL));
    }

    //SOM d∈D (ud × ps) ud= aantal items below stock level onder dag d
    public void calculateSL() {
        double totalUD = 0;
        int i1, i2;
        for (Item item : stock) {
            for (Day d : days) {
                i1 = item.getStockAmount(d);
                i2 = item.getMinAllowedInStock();
                int ud = i2 - i1;
                if (ud > 0) {
                    totalUD += ud;
                }
            }
        }
        setCostSL(totalUD * COST_PER_ITEM_UNDER_MINIMUM_LEVEL);
    }

    public double getCostDP() {
        return costDP;
    }

    public void setCostDP(double costDP) {
        this.costDP = costDP;
//        logger.log(Level.INFO, "Cost of DP= " + String.valueOf(costDP));
    }

    //dp × pp (=COST_OF_PARALLEL_TASK)
    public void calculateDP() {
        int dp = 0;
        for (Day d : days) {
            if (d.getParallel(machines)) dp++;
        }
        setCostDP(dp * COST_OF_PARALLEL_TASK);

    }

    public void calculateAllCosts() throws IOException {
        calculateDP();
        calculateNS();
        calculateOT();
        calculateSL();
        calculateUR();
        logAllCosts();
    }

    public double getTotalCost() {
        return costDP + costNS + costOT + costSL + costUR;
    }

    private void logAllCosts(@Nullable Object... params) throws IOException {
        String msg = "(NS: " + costNS + "| OT: " + costOT + "| UR: " + costUR + "| SL: " + costSL + "| DP: " + costDP + ")" + " [TOTAL: " + getTotalCost() + "]";
        logger.log(Level.INFO, msg, params);

        String line = getTotalCost() + "," + costNS + "," + costOT + "," + costUR + "," + costSL + "," + costDP;
        Main.output.add(line);
    }
}
