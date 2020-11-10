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
	private boolean hasNightShift;

	public Day(int id) {
		this.id = id;
		blocks = new ArrayList<>(numberOfBlocksPerDay);
		for (int i = 0; i < numberOfBlocksPerDay; i++) {
			blocks.add(new Block(i));
		}
		this.hasNightShift = false;
	}

	public Day(Day d) {
		this.id = d.id;
		this.hasNightShift = d.hasNightShift;
		this.blocks = new ArrayList<>(d.blocks.size());
		for (Block b : d.blocks) {
			this.blocks.add(new Block(b));
		}
	}

	public Block getBlock(int id) {
		return blocks.get(id);
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
		return hasNightShift;
	}

	public void setNightShift(boolean hasNightShift) {
		this.hasNightShift = hasNightShift;
	}

	@Override
	public Iterator<Block> iterator() {
		return blocks.iterator();
	}
}
