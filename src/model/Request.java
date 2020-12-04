package model;

import java.util.*;

public class Request {

    private final int id;
    private Day shippingDay;
    private final List<Day> possibleShippingDays;
    private final Map<Item, Integer> amountOfItem;

    public Request(int id) {
        this.id = id;
        possibleShippingDays = new ArrayList<>();
        amountOfItem = new HashMap<>();
        shippingDay = null;
    }

    public boolean containsItem(Item item) {
        return amountOfItem.containsKey(item);
    }

    public void removeShippingDay() {
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

    public List<Day> getPossibleShippingDays() {
        return possibleShippingDays;
    }

    public Set<Item> getItemsKeySet() {
        return amountOfItem.keySet();
    }

    public int getAmountOfItem(Item i) {
        return amountOfItem.get(i);
    }

    public boolean hasShippingDay() {
        return shippingDay != null;
    }

    public Map<Item, Integer> getMap() {
        return amountOfItem;
    }

}
