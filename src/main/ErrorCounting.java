package main;

public class ErrorCounting {
    int isConsecutiveInNightShiftsPast=0;
    int checkNighShiftBlocksConstraints=0;
    int checkOvertimeConstraints=0;
    int checkParallelConstraints=0;
    int checkProductionConstraints=0;
    int checkStockConstraints=0;
    int checkSetupTypeConstraint=0;
    int checkShippingDayConstraints=0;
    int checkChangeOverAndMaintenanceBoundaryConstraints=0;
    int checkMaintenanceConstraints=0;


    public ErrorCounting (){

    }

    public void setIsConsecutiveInNightShiftsPast(int isConsecutiveInNightShiftsPast) {
        this.isConsecutiveInNightShiftsPast = isConsecutiveInNightShiftsPast;
    }

    public void setCheckNighShiftBlocksConstraints(int checkNighShiftBlocksConstraints) {
        this.checkNighShiftBlocksConstraints = checkNighShiftBlocksConstraints;
    }

    public void setCheckOvertimeConstraints(int checkOvertimeConstraints) {
        this.checkOvertimeConstraints = checkOvertimeConstraints;
    }

    public void setCheckParallelConstraints(int checkParallelConstraints) {
        this.checkParallelConstraints = checkParallelConstraints;
    }

    public void setCheckProductionConstraints(int checkProductionConstraints) {
        this.checkProductionConstraints = checkProductionConstraints;
    }

    public void setCheckStockConstraints(int checkStockConstraints) {
        this.checkStockConstraints = checkStockConstraints;
    }

    public void setCheckSetupTypeConstraint(int checkSetupTypeConstraint) {
        this.checkSetupTypeConstraint = checkSetupTypeConstraint;
    }

    public void setCheckShippingDayConstraints(int checkShippingDayConstraints) {
        this.checkShippingDayConstraints = checkShippingDayConstraints;
    }

    public void setCheckChangeOverAndMaintenanceBoundaryConstraints(int checkChangeOverAndMaintenanceBoundaryConstraints) {
        this.checkChangeOverAndMaintenanceBoundaryConstraints = checkChangeOverAndMaintenanceBoundaryConstraints;
    }

    public void setCheckMaintenanceConstraints(int checkMaintenanceConstraints) {
        this.checkMaintenanceConstraints = checkMaintenanceConstraints;
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
