package model.machinestate;

import model.Item;

public class Production implements MachineState {
	private Item item;

	public Production(Item item) {
		this.item = item;
	}

	@Override
	public String toString() {
		return "I_" + item.getId();
	}

	public Item getItem() {
		return item;
	}
}
