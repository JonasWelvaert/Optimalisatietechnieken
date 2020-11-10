package model;

import java.util.ArrayList;
import java.util.List;

public class Planning {
	private static int numberOfDays;
	private static int minConsecutiveDaysWithNightShift;
	private static int pastConsecutiveDaysWithNightShift;
	private String instanceName;
	private List<Machine> machines;
	private List<Day> days;
	private Requests requests;
	private Stock stock;
	private int cost;

	public Planning(String instanceName, int nrOfMachines) {
		this.instanceName = instanceName;
		machines = new ArrayList<>(nrOfMachines);
		days = new ArrayList<>(numberOfDays);
		for (int i = 0; i < numberOfDays; i++) {
			days.add(new Day(i));
		}
	}



	public Planning(Planning p) {
		// TODO: deep copy
		this.instanceName = p.instanceName;
		this.machines = p.machines;
		this.days = p.days;
		this.requests = p.requests;
		this.stock = p.stock;
		this.cost = p.cost;
	}

	public void addMachine(Machine m) {
		machines.add(m);
	}

	public String getInstanceName() {
		return instanceName;
	}

	public static int getNumberOfDays() {
		return numberOfDays;
	}

	public static void setNumberOfDays(int nrOfDays) {
		Planning.numberOfDays = nrOfDays;
	}

	public static int getMinConsecutiveDaysWithNightShift() {
		return minConsecutiveDaysWithNightShift;
	}

	public static void setMinConsecutiveDaysWithNightShift(int minConsecutiveDaysWithNightShift) {
		Planning.minConsecutiveDaysWithNightShift = minConsecutiveDaysWithNightShift;
	}

	public static int getPastConsecutiveDaysWithNightShift() {
		return pastConsecutiveDaysWithNightShift;
	}

	public static void setPastConsecutiveDaysWithNightShift(int pastConsecutiveDaysWithNightShift) {
		Planning.pastConsecutiveDaysWithNightShift = pastConsecutiveDaysWithNightShift;
	}

	public List<Machine> getMachines() {
		return machines;
	}
	
	public Day getDay(int id) {
		return days.get(id);
	}
	
	public List<Day> getDays() {
		return days;
	}

	public Requests getRequests() {
		return requests;
	}

	public void setRequests(Requests requests) {
		this.requests = requests;
	}

	public Stock getStock() {
		return stock;
	}

	public void setStock(Stock stock) {
		this.stock = stock;
	}

	public int getCost() { return cost; }
}
