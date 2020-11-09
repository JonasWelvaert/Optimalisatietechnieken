package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

	private int id;
	private Day shippingDay;
	private List<Day> possibleShippingDays;
	private Map<Item, Integer> amountOfItem;

	public Request(int id) {
		this.id = id;
		possibleShippingDays = new ArrayList<>();
		amountOfItem = new HashMap<>();
		shippingDay = null;
	}

	public void addPossibleShippingDay(Day day) {
		possibleShippingDays.add(day);
	}

	public void addItem(Item i, int amountOfItem) {
		this.amountOfItem.put(i, amountOfItem);
	}

	public void setShippingDay(Day shippingDay) {
		this.shippingDay = shippingDay;
	}

	public Day getShippingDay() {
		return shippingDay;
	}
	
	public int getId() {
		return id;
	}

}
