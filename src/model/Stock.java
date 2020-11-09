package model;

import java.util.ArrayList;
import java.util.List;

public class Stock {
	private List<Item> items;
	private int nrOfDifferentItems;

	public Stock(int nrOfDifferentItems) {
		this.nrOfDifferentItems = nrOfDifferentItems;
		items = new ArrayList<>(nrOfDifferentItems);
		for (int i = 0; i < nrOfDifferentItems; i++) {
			items.add(new Item(i));
		}
	}

	public Item getItem(int id) {
		return items.get(id);
	}

	public int getNrOfDifferentItems() {
		return nrOfDifferentItems;
	}

	public List<Item> getItems() {
		return items;
	}
	

}
