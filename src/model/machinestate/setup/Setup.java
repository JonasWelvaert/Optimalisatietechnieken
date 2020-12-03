package model.machinestate.setup;

import model.Item;
import model.machinestate.MachineState;

public abstract class Setup implements MachineState {
    private final Item from;
    private final Item to;
    private int setupTime;

    public Setup(Item from, Item to) {
        this.from = from;
        this.to = to;
        this.setupTime = from.getSetupTimeTo(to);
    }

    @Override
    public String toString() {
        return "S_" + from.getId() + "_" + to.getId();
    }

    public Item getTo() {
        return to;
    }

    public Item getFrom() {
        return from;
    }

    public int getSetupTime() {
        return setupTime;
    }

    public void setSetupTime(int setupTime) {
        this.setupTime = setupTime;
    }
}
