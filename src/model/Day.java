package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Day implements Iterable<Block> {
	private static int numberOfBlocksPerDay;
	public static int indexOfBlockE;
	public static int indexOfBlockL;
	public static int indexOfBlockS;
	public static int indexOfBlockO;
	private int id;
	private List<Block> blocks;

	public Day(int id) {
		this.id = id;
		blocks = new ArrayList<>(numberOfBlocksPerDay);
		for (int i = 0; i < numberOfBlocksPerDay; i++) {
			blocks.add(new Block(i));
		}
	}

	public static void setNumberOfBlocksPerDay(int numberOfBlocksPerDay) {
		Day.numberOfBlocksPerDay = numberOfBlocksPerDay;
	}

	public static int getNumberOfBlocksPerDay() {
		return numberOfBlocksPerDay;
	}

	public int getId() {
		return id;
	}

	public List<Block> getBlocks() {
		return blocks;
	}

	public boolean hasNightShift() {
		return false;// TODO
	}

	@Override
	public Iterator<Block> iterator() {
		return blocks.iterator();
	}
}
