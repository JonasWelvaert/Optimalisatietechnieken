package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Stock implements Iterable<Item> {
	private final List<Item> items;
	private static int nrOfDifferentItems;

	public Stock(int nrOfDifferentItems) {
		Stock.nrOfDifferentItems = nrOfDifferentItems;
		items = new ArrayList<>(nrOfDifferentItems);
		for (int i = 0; i < nrOfDifferentItems; i++) {
			items.add(new Item(i));
		}
	}

	protected Stock(Stock s, List<Day> days) {
		this.items = new ArrayList<>(nrOfDifferentItems);
		for (int i = 0; i < nrOfDifferentItems; i++) {
			Item item = new Item(s.getItem(i).getId());
			item.setCostPerItem(s.getItem(i).getCostPerItem());
			item.setInitialQuantityInStock(s.getItem(i).getInitialQuantityInStock());
			item.setMaxAllowedInStock(s.getItem(i).getMaxAllowedInStock());
			item.setMinAllowedInStock(s.getItem(i).getMinAllowedInStock());
			items.add(item);
		}
		for (int i = 0; i < nrOfDifferentItems; i++) {
			Item otherItem = s.getItem(i);
			Item item = this.getItem(i);
			for (int j = 0; j < nrOfDifferentItems; j++) {
				if (i != j) {
					item.setLargeSetup(this.getItem(j), otherItem.isLargeSetup(s.getItem(j)));
					item.setSetupTime(this.getItem(j), otherItem.getSetupTimeTo(s.getItem(j)));
				}
			}
			for (Day d : days) {
				item.setStockAmount(d, otherItem.getStockAmount(otherItem.findDayWithId(d.getId())));
			}
		}
	}

	public Item getItem(int id) {
		for (Item i : items) {
			if (i.getId() == id) {
				return i;
			}
		}
		throw new RuntimeException("Item id not found.");
	}

	public static int getNrOfDifferentItems() {
		return nrOfDifferentItems;
	}
	
	public List<Item> getPossibleItemsForMachine(Machine m){
		List<Item> ret = new ArrayList<>();
		for (Item i: items) {
			if(m.getEfficiency(i)>0) {
				ret.add(i);
			}
		}
		return ret;
	}

	public List<Item> getItems() {
		return items;
	}

	public Item findItemWithId(int id) {
		for (Item i : items) {
			if (i.getId() == id)
				return i;
		}
		return null;
	}

	@Override
	public Iterator<Item> iterator() {
		return items.iterator();
	}
}
