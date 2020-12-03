package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Stock implements Iterable<Item> {
    private List<Item> items;
    private int nrOfDifferentItems;

    public Stock(int nrOfDifferentItems) {
        this.nrOfDifferentItems = nrOfDifferentItems;
        items = new ArrayList<>(nrOfDifferentItems);
        for (int i = 0; i < nrOfDifferentItems; i++) {
            items.add(new Item(i));
        }
    }

    protected Stock(Stock s, List<Day> days) {
        this.nrOfDifferentItems = s.nrOfDifferentItems;
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
            Item otherItem = s.items.get(i);
            Item item = items.get(i);
            for (int j = 0; j < nrOfDifferentItems; j++) {
                if (i != j) {
                    item.setLargeSetup(items.get(j), otherItem.isLargeSetup(s.items.get(j)));
                    item.setSetupTime(items.get(j), otherItem.getSetupTimeTo(s.items.get(j)));
                }
            }
            for (Day d : days) {
                item.setStockAmount(d, otherItem.getStockAmount(otherItem.findDayWithId(d.getId())));
            }
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
