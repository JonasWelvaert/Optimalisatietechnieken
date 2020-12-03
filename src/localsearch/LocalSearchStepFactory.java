package localsearch;

import localsearch.steps.*;

import java.util.HashMap;
import java.util.Map;

import static localsearch.EnumStep.*;

public class LocalSearchStepFactory {

    private Map<EnumStep, LocalSearchStep> localSearchSteps;
    private final int maxTries = 100;

    public LocalSearchStepFactory() {
        localSearchSteps = new HashMap<>();
        localSearchSteps.put(ADD_SINGLE_PRODUCTION, new AddSingleProduction(maxTries));
        localSearchSteps.put(MOVE_MAINTENANCE, new MoveMaintenance(maxTries));
        localSearchSteps.put(ADD_PRODUCTION_FOR_SHIPPING, new AddProductionForShipping(maxTries));
        localSearchSteps.put(CHANGE_PRODUCTION, new ChangeProduction(maxTries));
        localSearchSteps.put(REMOVE_PRODUCTION, new RemoveProduction(maxTries));
        localSearchSteps.put(MOVE_PRODUCTION, new MoveProduction(maxTries));
        localSearchSteps.put(MOVE_SHIPPING_DAY, new MoveShippingDay(maxTries));
        localSearchSteps.put(PLAN_SHIPPING_DAY, new PlanShippingDay(maxTries));
        localSearchSteps.put(JOIN_NEIGHBOURING_SETUPS, new JoinNeighbouringSetups(maxTries));

    }

    public LocalSearchStep getLocalSearchStep(EnumStep es) {
        return localSearchSteps.get(es);
    }

    public void addLocalSearchStep(EnumStep es, LocalSearchStep localSearchStep) {
        localSearchSteps.put(es, localSearchStep);
    }
}
