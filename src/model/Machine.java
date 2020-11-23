package model;

import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Production;

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

	// voor een vorig item te vinden tov een bepaalde block
	public Item getPreviousItem(Planning p, int randomDay, int randomBlock) {
		boolean foundPreviousItem = false;
		int currentDay = randomDay;
		int currentBlock = randomBlock;
		while (!foundPreviousItem) { // afgaan tot als we block met production vinden
			if (currentBlock == 0 && currentDay == 0) { // als we helemaal in het begin zitten
				foundPreviousItem = true;
			} else {
				MachineState ms = p.getDay(currentDay).getBlock(currentBlock).getMachineState(this);
				String msString = ms.toString();
				if (msString.startsWith("I_")) {
					Production prod = (Production) p.getDay(currentDay).getBlock(currentBlock).getMachineState(this);
					return prod.getItem();
				} else {
					if (currentBlock == 0) {
						currentDay--;
						currentBlock = Day.getNumberOfBlocksPerDay()-1;
					} else {
						currentBlock--;
					}
				}
			}
		}
		return initialSetup;
	}

	public Item getNextNotIdle(Planning p, int randomDay, int randomBlock) {
		boolean foundNextItem = false;
		int currentDay = randomDay;
		int currentBlock = randomBlock;

		int aantalBlocks = p.getDay(0).getBlocks().size();
		int aantalDagen = p.getDays().size();

		while (!foundNextItem) { // afgaan tot als we block met production vinden
			if (currentBlock == aantalBlocks-1 && currentDay == aantalDagen-1) { // als we helemaal op het einde zitten
				foundNextItem = true;
			} else {
				boolean instanceOfIdle = p.getDay(currentDay).getBlock(currentBlock).getMachineState(this) instanceof Idle;
				if (!instanceOfIdle) {
					Production prod = (Production) p.getDay(currentDay).getBlock(currentBlock).getMachineState(this);
					return prod.getItem();
				} else {
					if (currentBlock == aantalBlocks-1) {
						currentDay++;
						currentBlock = 0;
					} else {
						currentBlock++;
					}
				}
			}
		}
		return null;
	}


}
