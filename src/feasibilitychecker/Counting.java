package feasibilitychecker;

public class Counting {
    int isConsecutiveInNightShiftsPast = 0;
    int checkNighShiftBlocksConstraints = 0;
    int checkOvertimeConstraints = 0;
    int checkParallelConstraints = 0;
    int checkProductionConstraints = 0;
    int checkStockConstraints = 0;
    int checkSetupTypeConstraint = 0;
    int checkShippingDayConstraints = 0;
    int checkChangeOverAndMaintenanceBoundaryConstraints = 0;
    int checkMaintenanceConstraints = 0;

    public static int JoinSingleNeighbouringSetup =0;


    public Counting() {

    }

    public void increaseIsConsecutiveInNightShiftsPast() {
        this.isConsecutiveInNightShiftsPast++;
    }

    public void increaseCheckNighShiftBlocksConstraints() {
        this.checkNighShiftBlocksConstraints ++;
    }

    public void increaseCheckOvertimeConstraints() {
        this.checkOvertimeConstraints ++;
    }

    public void increaseCheckParallelConstraints() {
        this.checkParallelConstraints ++;
    }

    public void increaseCheckProductionConstraints() {
        this.checkProductionConstraints ++;
    }

    public void increaseCheckStockConstraints() {
        this.checkStockConstraints ++;
    }

    public void increaseCheckSetupTypeConstraint() {
        this.checkSetupTypeConstraint ++;
    }

    public void increaseCheckShippingDayConstraints() {
        this.checkShippingDayConstraints ++;
    }

    public void increaseCheckChangeOverAndMaintenanceBoundaryConstraints() {
        this.checkChangeOverAndMaintenanceBoundaryConstraints++;
    }

    public void increaseCheckMaintenanceConstraints() {
        this.checkMaintenanceConstraints ++;
    }


    @Override
    public String toString() {
        return "ErrorCounting{" +
                "isConsecutiveInNightShiftsPast=" + isConsecutiveInNightShiftsPast +
                ", checkNighShiftBlocksConstraints=" + checkNighShiftBlocksConstraints +
                ", checkOvertimeConstraints=" + checkOvertimeConstraints +
                ", checkParallelConstraints=" + checkParallelConstraints +
                ", checkProductionConstraints=" + checkProductionConstraints +
                ", checkStockConstraints=" + checkStockConstraints +
                ", checkSetupTypeConstraint=" + checkSetupTypeConstraint +
                ", checkShippingDayConstraints=" + checkShippingDayConstraints +
                ", checkChangeOverAndMaintenanceBoundaryConstraints=" + checkChangeOverAndMaintenanceBoundaryConstraints +
                ", checkMaintenanceConstraints=" + checkMaintenanceConstraints +
                '}';
    }
}
