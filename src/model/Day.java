package model;
import java.util.ArrayList;
import java.util.List;

public class Day {
	private int id;
	private List<Block> timeslots;

	public Day() {
		id = 0;
		timeslots = new ArrayList<>();
	}
}
