package model;

import model.machinestate.setup.LargeSetup;
import model.machinestate.setup.Setup;
import model.machinestate.setup.SmallSetup;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Item {
    private final int id;
    private double costPerItem;
    private int initialQuantityInStock;
    private int minAllowedInStock;
    private int maxAllowedInStock;
    private final Map<Day, Integer> stockAmount;
    private final Map<Item, Boolean> largeSetup;
    private final Map<Item, Integer> setupTime;

    public Item(int id) {
        this.id = id;
        largeSetup = new HashMap<>();
        setupTime = new HashMap<>();
        stockAmount = new HashMap<>();
    }

    public void updateItem(Day d, Machine m, int factor) {
        int amount = stockAmount.get(d);
        int efficiency = m.getEfficiency(this);
        //int efficiency = 123;
        int newAmount = amount + (factor * efficiency);
        stockAmount.replace(d, newAmount);
    }

    public void replace(Day d, int amount) {
        stockAmount.replace(d, amount);
    }

    public void setCostPerItem(double costPerItem) {
        this.costPerItem = costPerItem;
    }

    public void setInitialQuantityInStock(int initialQuantityInStock) {
        this.initialQuantityInStock = initialQuantityInStock;
    }

    public Setup getSetupTo(Item to) {
        Setup setup;
        if (largeSetup.get(to)) {
            setup = new LargeSetup(this, to);
        } else {
            setup = new SmallSetup(this, to);
        }
        return setup;
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

    public void removeStockAmount(Day d) {
        stockAmount.remove(d);
    }

    public void setStockAmount(Day d, int amount) {
        stockAmount.put(d, amount);

        if (stockAmount.get(d) < 0) {
            System.out.println("Fout");
        }
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

    public int getSetupTimeTo(Item to) {
        return setupTime.get(to);
    }

    public int getStockAmount(Day d) {
        return stockAmount.get(d);
    }

    public Day findDayWithId(int id) {
        for (Day d : stockAmount.keySet()) {
            if (d.getId() == id) return d;
        }
        return null;
    }

    public int getAantalItems() {
        return largeSetup.size() + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Day, Integer> entry : stockAmount.entrySet()) {
            sb.append("Day "+entry.getKey().getId() + "\t :\t" + entry.getValue() + "\n");
        }

        return sb.toString();
    }

    public int checkStock(){
        for (Map.Entry<Day, Integer> entry : stockAmount.entrySet()) {
           if( entry.getValue()<0){
               return entry.getKey().getId();
           }
        }
        return -1;
    }
}
