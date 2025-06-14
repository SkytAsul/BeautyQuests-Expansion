package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.api.players.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import sh.okx.rankup.RankupPlugin;
import sh.okx.rankup.events.RankupRegisterEvent;
import sh.okx.rankup.requirements.DeductibleRequirement;
import sh.okx.rankup.requirements.ProgressiveRequirement;
import sh.okx.rankup.requirements.Requirement;

public class QuestPointsRankup implements Listener {

	private final @NotNull QuestPointsManager manager;
	private final @NotNull PlayerManager playerManager;

	public QuestPointsRankup(QuestPointsManager manager, @NotNull PlayerManager playerManager) {
		this.manager = manager;
		this.playerManager = playerManager;
	}

	@EventHandler
	public void onRankupRegister(RankupRegisterEvent event) {
		event.addRequirement(new PointsRequirement(event.getPlugin(), "beautyquests-pointsh"),
				new PointsDeductibleRequirement(event.getPlugin(), "beautyquests-points"));
	}

	private class PointsRequirement extends ProgressiveRequirement {

		public PointsRequirement(RankupPlugin plugin, String name) {
			super(plugin, name);
		}

		public PointsRequirement(Requirement clone) {
			super(clone);
		}

		@Override
		public double getProgress(Player player) {
			return manager.getPoints(PlayerManager.getPlayerAccount(player));
		}

		@Override
		public Requirement clone() {
			return new PointsRequirement(this);
		}

	}

	private class PointsDeductibleRequirement extends PointsRequirement
			implements DeductibleRequirement {

		public PointsDeductibleRequirement(RankupPlugin plugin, String name) {
			super(plugin, name);
		}

		public PointsDeductibleRequirement(Requirement clone) {
			super(clone);
		}

		@Override
		public void apply(Player player, double multiplier) {
			try {
				manager.addPoints(playerManager.getQuester(player), -getValueInt() * (int) multiplier);
			} catch (IllegalPointsBalanceException ex) {
				throw new IllegalArgumentException(ex);
			}
		}

		@Override
		public Requirement clone() {
			return new PointsDeductibleRequirement(this);
		}

	}

}
