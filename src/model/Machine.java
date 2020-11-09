package model;

import java.util.HashMap;
import java.util.Map;

public class Machine {
	private int id;
	private Item initialSetup;
	private int initialDaysPastWithoutMaintenance;
	private int maxDaysWithoutMaintenance;
	private int maintenanceDurationInBlocks;
	private Map<Item, Integer> efficiency;

	public Machine(int id, Item item, int daysPastWithoutMaintenance, int maxDaysWithoutMaintenance,
			int maintenanceDurationInBlocks) {
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
}
