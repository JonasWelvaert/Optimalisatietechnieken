package model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import machinetoestand.MachineToestand;

public class Block {
	private int id;
	private Map<Machine, MachineToestand> machineToestanden;

	public Block() {
		machineToestanden = new HashMap<>();
	}
}