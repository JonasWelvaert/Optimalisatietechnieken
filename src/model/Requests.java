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

    public Request get(int i) {
        return requests.get(i);
    }

    @Override
    public Iterator<Request> iterator() {
        return requests.iterator();
    }

}
