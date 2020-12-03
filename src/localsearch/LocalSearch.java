package localsearch;

import model.Planning;

public interface LocalSearch {

    boolean execute(Planning p); //TODO optioneel: return boolean (dan kan je al checken voor feasiblity in execute)

}
