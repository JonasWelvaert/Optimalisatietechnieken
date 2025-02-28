package model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Requests implements Iterable<Request> {
    private final List<Request> requests;

    public Requests() {
        requests = new ArrayList<>();
    }

    public void add(Request r) {
        requests.add(r);
    }

    public boolean isEmpty() {
        if (requests == null)
            return true;
        return requests.size() == 0;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public Request get(int id) {
		for (Request r: requests) {
			if (r.getId() == id) {
				return r;
			}
		}
		throw new RuntimeException("Request id not found.");
    }
    
    public int amountOfItemNeeded(Item item) {
    	int ret = 0;
    	for(Request r: requests) {
    		if(!r.hasShippingDay() && r.containsItem(item)) {
    			ret += r.getAmountOfItem(item);
    		}
    	}
    	return ret;
    }

    @Override
    public Iterator<Request> iterator() {
        return requests.iterator();
    }

}
