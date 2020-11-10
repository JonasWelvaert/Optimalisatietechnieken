package model;

import java.util.HashMap;
import java.util.Map;

public class Item {
	private int id;
	private double costPerItem;
	private int initialQuantityInStock;
	private int minAllowedInStock;
	private int maxAllowedInStock;
	private Map<Day, Integer> stockAmount;
	private Map<Item, Boolean> largeSetup;
	private Map<Item, Integer> setupTime;

	public Item(int id) {
		this.id = id;
		largeSetup = new HashMap<>();
		setupTime = new HashMap<>();
		stockAmount = new HashMap<>();
	}

	public void setCostPerItem(double costPerItem) {
		this.costPerItem = costPerItem;
	}

	public void setInitialQuantityInStock(int initialQuantityInStock) {
		this.initialQuantityInStock = initialQuantityInStock;
	}

	public void setMaxAllowedInStock(int maxAllowedInStock) {
		this.maxAllowedInStock = maxAllowedInStock;
	}

	public void setMinAllowedInStock(int minAllowedInStock) {
		this.minAllowedInStock = minAllowedInStock;
	}

	public void setLargeSetup(Item i, boolean b) {
		largeSetup.put(i, b);
	}

	public void setSetupTime(Item i, int time) {
		setupTime.put(i, time);
	}

	public void setStockAmount(Day d, int amount) {
		stockAmount.put(d, amount);
	}

	public int getInitialQuantityInStock() {
		return initialQuantityInStock;
	}

	public int getId() {
		return id;
	}

	public double getCostPerItem() {
		return costPerItem;
	}
	
	public int getMaxAllowedInStock() {
		return maxAllowedInStock;
	}
	
	public int getMinAllowedInStock() {
		return minAllowedInStock;
	}
	
	public boolean isLargeSetup(Item to) {
		return largeSetup.get(to);
	}
	
	public int getLengthSetup(Item to) {
		return setupTime.get(to);
	}
	
	public int  getStockAmount(Day d) {
		return stockAmount.get(d);
	}
	
	public Day findDayWithId(int id) {
		for(Day d:stockAmount.keySet()) {
			if(d.getId() == id) return d;
		}
		return null;
	}
}
