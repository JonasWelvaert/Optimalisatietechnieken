package model.machinestate;

import model.Item;

public class Production implements MachineState {
	private Item item;

	@Override
	public String toString() {
		return "I_" + item.getId();
	}
}
