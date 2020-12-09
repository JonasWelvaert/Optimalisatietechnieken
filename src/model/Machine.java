package model;

import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Maintenance;
import model.machinestate.Production;
import model.machinestate.setup.Setup;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Machine {
    private final int id;
    private final Item initialSetup;
    private final int initialDaysPastWithoutMaintenance;
    private final int maxDaysWithoutMaintenance;
    private final int maintenanceDurationInBlocks;

    private final Map<Item, Integer> efficiency;
    private static final Logger logger = Logger.getLogger(Machine.class.getName());

    public Machine(int id, Item item, int daysPastWithoutMaintenance, int maxDaysWithoutMaintenance, int maintenanceDurationInBlocks) {
        logger.setLevel(Level.OFF);
        this.id = id;
        this.initialSetup = item;
        this.initialDaysPastWithoutMaintenance = daysPastWithoutMaintenance;
        this.maxDaysWithoutMaintenance = maxDaysWithoutMaintenance;
        this.maintenanceDurationInBlocks = maintenanceDurationInBlocks;
        efficiency = new HashMap<>();
    }

    public void addEfficiency(Item item, int efficiency) {
        this.efficiency.put(item, efficiency);
    }

    public int getId() {
        return id;
    }

    public Item getInitialSetup() {
        return initialSetup;
    }

    public int getMaintenanceDurationInBlocks() {
        return maintenanceDurationInBlocks;
    }

    public int getInitialDaysPastWithoutMaintenance() {
        return initialDaysPastWithoutMaintenance;
    }

    public int getMaxDaysWithoutMaintenance() {
        return maxDaysWithoutMaintenance;
    }

    public int getEfficiency(Item i) {
        return efficiency.get(i);
    }

    // voor een vorig item te vinden tov een bepaalde block
    public Item getPreviousItem(Planning p, Day day, Block block) {

        int currentDay = day.getId();
        int currentBlock = block.getId();

        if (currentBlock == 0) {
            if (currentDay == 0) {
                return initialSetup;
            } else {
                currentDay--;
                currentBlock = Day.getNumberOfBlocksPerDay() - 1;
            }
        } else {
            currentBlock--;
        }
        for (int d = currentDay; d >= 0; d--) {
            for (int b = currentBlock; b >= 0; b--) {
                MachineState ms = p.getDay(d).getBlock(b).getMachineState(this);
                if (ms instanceof Production) {
                    Production prod = (Production) p.getDay(d).getBlock(b).getMachineState(this);
                    return prod.getItem();
                } else if (ms instanceof Setup) {
                    Setup setup = (Setup) p.getDay(d).getBlock(b).getMachineState(this);
                    return setup.getTo();
                }
            }
            currentBlock = Day.getNumberOfBlocksPerDay() - 1;
        }
        return initialSetup;
    }

    public Item getNextNotIdle(Planning p, int randomDay, int randomBlock) {
        boolean foundNextItem = false;
        int currentDay = randomDay;
        int currentBlock = randomBlock;

        int aantalBlocks = p.getDay(0).getBlocks().size();
        int aantalDagen = p.getDays().size();

        while (!foundNextItem) { // afgaan tot als we block met production vinden
            if (currentBlock == aantalBlocks - 1 && currentDay == aantalDagen - 1) { // als we helemaal op het einde zitten
                foundNextItem = true;
            } else {
                boolean instanceOfIdle = p.getDay(currentDay).getBlock(currentBlock).getMachineState(this) instanceof Idle;
                if (!instanceOfIdle) {
                    Production prod = (Production) p.getDay(currentDay).getBlock(currentBlock).getMachineState(this);
                    return prod.getItem();
                } else {
                    if (currentBlock == aantalBlocks - 1) {
                        currentDay++;
                        currentBlock = 0;
                    } else {
                        currentBlock++;
                    }
                }
            }
        }
        return null;
    }

}
