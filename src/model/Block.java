package model;
import java.util.HashMap;
import java.util.Map;

import machinestates.MachineState;

public class Block {
	private int id;
	private Map<Machine, MachineState> machineToestanden;

	public Block() {
		machineToestanden = new HashMap<>();
	}
}