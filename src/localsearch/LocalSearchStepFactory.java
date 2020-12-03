package localsearch;

import localsearch.add.AddProductionForShipping;
import localsearch.add.AddShippingDay;
import localsearch.add.AddSingleProduction;
import localsearch.change.ChangeProduction;
import localsearch.move.MoveMaintenance;
import localsearch.move.MoveProduction;
import localsearch.move.MoveShippingDay;
import localsearch.other.JoinNeighbouringSetups;
import localsearch.remove.RemoveProduction;

import java.util.HashMap;
import java.util.Map;

import static localsearch.EnumLocalSearchStep.*;

public class LocalSearchStepFactory {

    private final Map<EnumLocalSearchStep, LocalSearchStep> localSearchSteps;

    public LocalSearchStepFactory() {
        localSearchSteps = new HashMap<>();
        int maxTries = 100;

        //ADD
        localSearchSteps.put(ADD_SINGLE_PRODUCTION, new AddSingleProduction(maxTries));
        localSearchSteps.put(ADD_PRODUCTION_FOR_SHIPPING, new AddProductionForShipping(maxTries));
        localSearchSteps.put(ADD_SHIPPING_DAY, new AddShippingDay(maxTries));

        //CHANGE
        localSearchSteps.put(CHANGE_PRODUCTION, new ChangeProduction(maxTries));

        //MOVE
        localSearchSteps.put(MOVE_MAINTENANCE, new MoveMaintenance(maxTries));
        localSearchSteps.put(MOVE_PRODUCTION, new MoveProduction(maxTries));
        localSearchSteps.put(MOVE_SHIPPING_DAY, new MoveShippingDay(maxTries));

        //OTHER
        localSearchSteps.put(JOIN_NEIGHBOURING_SETUPS, new JoinNeighbouringSetups(maxTries));

        //REMOVE
        localSearchSteps.put(REMOVE_PRODUCTION, new RemoveProduction(maxTries));

    }

    public LocalSearchStep getLocalSearchStep(EnumLocalSearchStep es) {
        return localSearchSteps.get(es);
    }

    public void addLocalSearchStep(EnumLocalSearchStep es, LocalSearchStep localSearchStep) {
        localSearchSteps.put(es, localSearchStep);
    }
}
