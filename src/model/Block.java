package model;
import java.util.ArrayList;
import java.util.List;

public class Block {
	private int id;
	private List<Machine> machines;

	public Block() {
		machines = new ArrayList<>();
	}
}
