package model;

import java.util.HashMap;
import java.util.Map;

import model.machinestate.MachineState;

public class Block {
	private int id;
	private Map<Machine, MachineState> machineState;

	public Block(int id) {
		this.id = id;
		machineState = new HashMap<>();
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
}