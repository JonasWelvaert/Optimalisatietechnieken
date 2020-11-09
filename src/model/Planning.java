package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Planning {
	public static int NR_OF_DAYS;
	private String instanceName;
	private List<Machine> machines;
	private Map<Machine, List<Day>> days;

	public Planning(String instanceName, int nrOfMachines) {
		this.instanceName = instanceName;
		machines = new ArrayList<>(nrOfMachines);
		days = new HashMap<>(nrOfMachines);
	}
	
	public Map<Machine, List<Day>> getDays() {
		return days;
	}
}
