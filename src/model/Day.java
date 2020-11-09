package model;
import java.util.ArrayList;
import java.util.List;

public class Day {
	public static int NUMBER_OF_BLOCKS_PER_DAY;
	public static int INDEX_OF_BLOCK_E;
	public static int INDEX_OF_BLOCK_L;
	public static int INDEX_OF_BLOCK_S;
	public static int INDEX_OF_BLOCK_O;
	private int id;
	private List<Block> timeslots;

	public Day() {
		id = 0;
		timeslots = new ArrayList<>();
	}
}
