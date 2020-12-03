package model;

import model.machinestate.MachineState;
import model.machinestate.Maintenance;

import java.util.HashMap;
import java.util.Map;

public class Block {
    private final int id;
    private final Map<Machine, MachineState> machineState;

    public Block(int id) {
        this.id = id;
        machineState = new HashMap<>();
    }

    public Block(Block b) {
        this.id = b.id;
        this.machineState = new HashMap<>(b.machineState.size());
    }

    public void setMachineState(Machine m, MachineState ms) {
        machineState.put(m, ms);
    }

    public int getId() {
        return id;
    }

    public MachineState getMachineState(Machine m) {
        return machineState.get(m);
    }

    public boolean isInMaintenance() {
        for (MachineState ms : machineState.values()) {
            if (ms.getClass() == Maintenance.class) {
                return true;
            }
        }
        return false;
    }
}