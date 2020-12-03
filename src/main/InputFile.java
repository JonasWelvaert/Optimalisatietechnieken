package main;

public enum InputFile {
    Toy("toy_inst.txt"),
    D10_R10_B30("A_10_10_30.txt"),
    D10_R10_B60("A_10_10_60.txt"),
    D10_R15_B30("A_10_15_30.txt"),
    D10_R15_B60("A_10_15_60.txt"),
    D20_R15_B30("A_20_15_30.txt"),
    D20_R15_B60("A_20_15_60.txt"),
    D20_R25_B30("A_20_25_30.txt"),
    D20_R25_B60("A_20_25_60.txt"),
    D40_R100_B30("A_40_100_30.txt"),
    D40_R100_B60("A_40_100_60.txt");

    private String string;

    InputFile(String string) {
        this.string = string;
    }

    public String toString() {
        return string;
    }
}
