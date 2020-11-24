package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public Set<Item> getItems() {
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
