package model;

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
    private int minConsecutiveDaysWithNightShift;
    private int pastConsecutiveDaysWithNightShift;
    private final String instanceName;
    private final List<Machine> machines;
    private final List<Day> days;
    private Requests requests;
    private Stock stock;
    private double costNightShift;
    private double costOverTime;
    private double costUnscheduledRequests;
    private double costStockLevel;
    private double costParallelDays;
    private final List<String> graphingOutput = new ArrayList<>();

    public Planning(String instanceName, int nrOfMachines) {
        logger.setLevel(Level.OFF);
        this.instanceName = instanceName;
        machines = new ArrayList<>(nrOfMachines);
        days = new ArrayList<>(numberOfDays);
        for (int i = 0; i < numberOfDays; i++) {
            days.add(new Day(i));
        }
    }

    /**
     * Constructor which makes a deep copy of a planning p
     *
     * @param p planning to copy
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
        this.costUnscheduledRequests = p.costUnscheduledRequests;
        this.costStockLevel = p.costStockLevel;
        this.costOverTime = p.costOverTime;
        this.costParallelDays = p.costParallelDays;
        this.costNightShift = p.costNightShift;
        this.pastConsecutiveDaysWithNightShift = p.pastConsecutiveDaysWithNightShift;
        this.minConsecutiveDaysWithNightShift = p.minConsecutiveDaysWithNightShift;

        for (Machine m : this.getMachines()) {
            Machine otherMachine = p.getMachines().get(m.getId());
            for (Item item : this.stock) {
                m.addEfficiency(item, otherMachine.getEfficiency(p.getStock().getItem(item.getId())));
            }
        }
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

    public int getMinConsecutiveDaysWithNightShift() {
        return minConsecutiveDaysWithNightShift;
    }

    public void setMinConsecutiveDaysWithNightShift(int minConsecutiveDaysWithNightShift) {
        this.minConsecutiveDaysWithNightShift = minConsecutiveDaysWithNightShift;
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

    public List<String> getGraphingOutput() {
        return graphingOutput;
    }

    public long getStockAmount() {
        List<Item> items = stock.getItems();

        long counter = 0;
        for (Item i : items) {
            for (Day d : days) {
                counter += i.getStockAmount(d);
            }
        }
        return counter;
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

    /**
     * @param efficiency can be negative
     */
    public void updateStockLevels(Day day, Item nItem, int efficiency) {
        for (int i = day.getId(); i < numberOfDays; i++) {
            Day dayTemp = days.get(i);
            int newAmount = nItem.getStockAmount(dayTemp) + efficiency;
            nItem.setStockAmount(dayTemp, newAmount);

        }
    }


    /* ------------------------- COSTS ------------------------- */
    public double getCostNightShift() {
        return costNightShift;
    }

    public void setCostNightShift(double costNightShift) {
        this.costNightShift = costNightShift;
    }

    //dns × pn (=COST_OF_NIGHTSHIFT)
    public void calculateNS() {
        int dns = 0;
        for (Day d : days) {
            if (d.hasNightShift()) {
                dns++;
            }
        }
        setCostNightShift(dns * COST_OF_NIGHT_SHIFT);
    }

    public double getCostOverTime() {
        return costOverTime;
    }

    public void setCostOverTime(double costOverTime) {
        this.costOverTime = costOverTime;
    }

    // to × po (=COST_OF_NIGHTSHIFT)
    public void calculateOT() {
        int to = 0;
        for (Day d : days) {
            to += d.getNumberOfOvertimeBlock(machines);
        }

        setCostOverTime(to * COST_OF_NIGHT_SHIFT);
    }

    public double getCostUnscheduledRequests() {
        return costUnscheduledRequests;
    }

    public void setCostUnscheduledRequests(double costUnscheduledRequests) {
        this.costUnscheduledRequests = costUnscheduledRequests;
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
        setCostUnscheduledRequests(total);
    }

    public double getCostStockLevel() {
        return costStockLevel;
    }

    public void setCostStockLevel(double costStockLevel) {
        this.costStockLevel = costStockLevel;
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
        setCostStockLevel(totalUD * COST_PER_ITEM_UNDER_MINIMUM_LEVEL);
    }

    public double getCostParallelDays() {
        return costParallelDays;
    }

    public void setCostParallelDays(double costParallelDays) {
        this.costParallelDays = costParallelDays;
    }

    //dp × pp (=COST_OF_PARALLEL_TASK)
    public void calculateDP() {
        int dp = 0;
        for (Day d : days) {
            if (d.getParallel(machines)) dp++;
        }
        setCostParallelDays(dp * COST_OF_PARALLEL_TASK);

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
        return costParallelDays + costNightShift + costOverTime + costStockLevel + costUnscheduledRequests;
    }

    public void logAllCosts() {
        String msg = "(NS: " + costNightShift + "\t | OT: " + costOverTime + "\t | UR: " + costUnscheduledRequests + "\t | SL: " + costStockLevel + "\t | DP: " + costParallelDays + ")" + "\t [TOTAL: " + getTotalCost() + "]";
        logger.log(Level.INFO, msg);

        String line = getTotalCost() + "," + costNightShift + "," + costOverTime + "," + costUnscheduledRequests + "," + costStockLevel + "," + costParallelDays;
        graphingOutput.add(line);
    }
}
