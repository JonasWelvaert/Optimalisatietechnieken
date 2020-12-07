package localsearch.add;

import localsearch.LocalSearchStep;
import model.*;
import model.machinestate.Idle;
import model.machinestate.MachineState;
import model.machinestate.Production;
import model.machinestate.setup.Setup;

import java.util.List;

public class AddSingleProduction extends LocalSearchStep {

	public AddSingleProduction(int maxTries) {
		super(maxTries);
	}

	@Override
	public boolean execute(Planning p) {
		int count = 0;
		int randomDay, randomBlock, randomMachine, randomItem;

		tries: while (count < maxTries) {
			count++;
			randomDay = random.nextInt(Planning.getNumberOfDays());
			Day day = p.getDay(randomDay);
			randomBlock = random.nextInt(Day.getNumberOfBlocksPerDay());
			Block block = day.getBlock(randomBlock);
			randomMachine = random.nextInt(p.getMachines().size());
			Machine machine = p.getMachines().get(randomMachine);
			List<Item> possibleRandomItems = p.getStock().getPossibleItemsForMachine(machine);
			// Geen random items meer
			randomItem = random.nextInt(possibleRandomItems.size());
			Item nItem = possibleRandomItems.get(randomItem);

			Day temp = p.getLastNOTPlannedShippingDayForItem(nItem);
			if (temp == null) {
				continue tries;
			} else if (temp.getId() < randomDay) {
				continue tries;
			}

			MachineState machineState = block.getMachineState(machine);
			if (machineState instanceof Idle) {
				Item pItem = machine.getPreviousItem(p, day, block);

				int machineEfficiency = machine.getEfficiency(nItem);
				if (machineEfficiency != 0) {
					boolean productionCanBePlanned;

					// STARTING FROM FEASIBLE STATE, SO IF NO BEFORE NEEDED, ALSO NO AFTER IS NEEDED
					if (!pItem.equals(nItem)) {
						Setup setupBefore = pItem.getSetupTo(nItem);
						Setup setupAfter = nItem.getSetupTo(pItem); // TODO niet altijd nodig !!!
						List<Block> beforeBlocks = getSetupBlockBeforeProduction(setupBefore, day, block, machine, p);
						List<Block> afterBlocks = getSetupBlocksAfterProduction(setupAfter, day, block, machine, p);

						// PLAN THE SETUPS IF NOT NULL
						if (beforeBlocks != null && afterBlocks != null) {
							for (Block b : beforeBlocks) {
								b.setMachineState(machine, setupBefore);
							}
							for (Block b : afterBlocks) {
								b.setMachineState(machine, setupAfter);
							}
							productionCanBePlanned = true;
						} else {
							continue tries;
						}
					}
					// ITEM ARE THE SAME SO NO SETUP NEEDED
					else {
						productionCanBePlanned = true;
					}
					// PLAN PRODUCTION
					if (productionCanBePlanned) {
						boolean isPossible = true;
						for (Day d : p.getSuccessorDaysInclusive(day)) {
							if (nItem.getStockAmount(d) + machine.getEfficiency(nItem) > nItem.getMaxAllowedInStock()) {
								continue tries;
							}
						}
						if (isPossible) {
							block.setMachineState(machine, new Production(nItem));
							p.updateStockLevels(day, nItem, machineEfficiency);

							return true;
						}
					}
				}
			}
		}
		return false;
	}
}
