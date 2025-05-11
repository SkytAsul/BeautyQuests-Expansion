package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.api.questers.Quester;

public class IllegalPointsBalanceException extends Exception {

	private static final long serialVersionUID = 8142562529319509619L;

	public IllegalPointsBalanceException(Quester quester, int illegalBalance) {
		super("Illegal quest points balance for " + quester.getDetailedName() + ": " + illegalBalance);
	}

}
