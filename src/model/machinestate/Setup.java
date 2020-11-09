package model.machinestate;

import model.Item;

public abstract class Setup implements MachineState {
	private Item from;
	private Item to;

	@Override
	public String toString() {
		return "S_" + from.getId() + "_" + to.getId();
	}
}
