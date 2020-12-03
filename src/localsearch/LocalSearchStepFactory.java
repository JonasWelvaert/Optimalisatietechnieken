package localsearch;

import java.util.HashMap;
import java.util.Map;

public class LocalSearchStepFactory {

    private Map<String, LocalSearchStep> localSearchSteps;

    public LocalSearchStepFactory() {
        localSearchSteps = new HashMap<>();
        localSearchSteps.put("AddSingleProduction", new AddSingleProduction(100));
    }

    public LocalSearchStep getLocalSearchStep(String lss) {
        return localSearchSteps.get(lss);
    }

    public void addLocalSearchStep(String lss, LocalSearchStep localSearchStep) {
        localSearchSteps.put(lss, localSearchStep);
    }
}
