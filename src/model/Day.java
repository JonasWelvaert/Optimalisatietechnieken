package model;

import model.machinestate.Idle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Day implements Iterable<Block> {
    private static int numberOfBlocksPerDay;
    public static int indexOfBlockE;
    public static int indexOfBlockL;
    public static int indexOfBlockS;
    public static int indexOfBlockO;
    private final int id;
    private final List<Block> blocks;
    private boolean hasNightShift;

    public Day(int id) {
        this.id = id;
        blocks = new ArrayList<>(numberOfBlocksPerDay);
        for (int i = 0; i < numberOfBlocksPerDay; i++) {
            blocks.add(new Block(i));
        }
        this.hasNightShift = false;
    }

    public Day(Day d) {
        this.id = d.id;
        this.hasNightShift = d.hasNightShift;
        this.blocks = new ArrayList<>(d.blocks.size());
        for (Block b : d.blocks) {
            this.blocks.add(new Block(b));
        }
    }

    public Block getBlock(int id) {
        return blocks.get(id);
    }

    public static void setNumberOfBlocksPerDay(int numberOfBlocksPerDay) {
        Day.numberOfBlocksPerDay = numberOfBlocksPerDay;
    }

    public List<Block> getBlocksBetweenInclusive(int t1, int t2) {
        List<Block> temp = new ArrayList<>();
        for (Block b : blocks) {
            if (t1 <= b.getId() && b.getId() <= t2) temp.add(b);
        }
        return temp;
    }

    public static int getNumberOfBlocksPerDay() {
        return numberOfBlocksPerDay;
    }

    public int getId() {
        return id;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public boolean hasNightShift() {
        return hasNightShift;
    }

    public void setNightShift(boolean hasNightShift) {
        this.hasNightShift = hasNightShift;
    }

    public static int getIndexOfBlockE() {
        return indexOfBlockE;
    }

    public static int getIndexOfBlockL() {
        return indexOfBlockL;
    }

    public static int getIndexOfBlockS() {
        return indexOfBlockS;
    }

    public static int getIndexOfBlockO() {
        return indexOfBlockO;
    }

    public int getNumberOfOvertimeBlock(List<Machine> machinesList) {
        int counter = 0;
        boolean working;
        if (!hasNightShift) {
            //TODO check for index 0 or 1 for block 1
            for (int i = indexOfBlockS + 1; i <= indexOfBlockO; i++) { //[bs+1;bo]
                working = false;
                Block b = blocks.get(i);
                for (Machine m : machinesList) {
                    if (!(b.getMachineState(m) instanceof Idle)) {
                        working = true;
                    }
                }
                if (working) {
                    counter++;
                }
            }
        }
        return counter;
    }

    public boolean getParallel(List<Machine> machinesList) {
        boolean bool;

        //TODO check for index 0 or 1 for block 1
        for (int i = 1; i <= indexOfBlockS; i++) { //[b1;bs]
            Block b = blocks.get(i);
            bool = false;
            for (Machine m : machinesList) {
                if (!(b.getMachineState(m) instanceof Idle)) {
                    if (bool) {
                        return true;
                    }
                    bool = true;
                }
            }
        }
        return false;
    }

    @Override
    public Iterator<Block> iterator() {
        return blocks.iterator();
    }

}
