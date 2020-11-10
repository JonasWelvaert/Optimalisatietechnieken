package model.machinestate;

import model.Item;

public abstract class Setup implements MachineState {
	private Item from;
	private Item to;

	public Setup(Item from, Item to) {
		this.from = from;
		this.to = to;
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

}
