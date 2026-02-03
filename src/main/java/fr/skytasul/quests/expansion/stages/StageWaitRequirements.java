package fr.skytasul.quests.expansion.stages;

import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.QuestsConfiguration;
import fr.skytasul.quests.api.objects.QuestObjectLocation;
import fr.skytasul.quests.api.players.PlayerQuester;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageController;
import fr.skytasul.quests.api.stages.StageDescriptionPlaceholdersContext;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.creation.StageCreationContext;
import fr.skytasul.quests.api.utils.progress.ProgressPlaceholders;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageWaitRequirements extends AbstractStage {

	private BukkitTask task;
	private List<Player> players;

	private Map<Player, List<String>> lastDescriptions = new HashMap<>();

	protected StageWaitRequirements(@NotNull StageController controller) {
		super(controller);
	}

	@Override
	public @NotNull String getDefaultDescription(@NotNull StageDescriptionPlaceholdersContext context) {
		if (!(context.getQuester() instanceof PlayerQuester playerQuester))
			throw new IllegalArgumentException("Only Player questers can do a stage requirement");

		if (!playerQuester.isActive())
			return "cannot get description for offline player";

		Player player = playerQuester.getPlayer().get();
		return ProgressPlaceholders.formatObjectList(context.getDescriptionSource(),
				QuestsConfiguration.getConfig().getStageDescriptionConfig(),
				getUnmetRequirementsDescriptions(player).toArray(String[]::new));
	}

	private List<@Nullable String> getUnmetRequirementsDescriptions(Player player) {
		return getValidationRequirements().stream()
				.filter(req -> req.isValid() && !req.test(player))
				.map(req -> req.getDescription(player))
				.toList();
	}

	protected void refresh() {
		for (Player player : players) {
			boolean requirementsMet = matchesRequirements(player);
			if (!requirementsMet) {
				var descriptions = getUnmetRequirementsDescriptions(player);
				if (descriptions.equals(lastDescriptions.get(player)))
					break;
				lastDescriptions.put(player, descriptions);
			}

			for (Quester quester : controller.getApplicableQuesters(player)) {
				if (requirementsMet) {
					controller.finishStage(quester);
				} else {
					controller.notifyQuesterUpdate(quester);
				}
			}
		}
	}

	@Override
	public void load() {
		super.load();
		players = new ArrayList<>();
		task = Bukkit.getScheduler().runTaskTimerAsynchronously(BeautyQuestsExpansion.getInstance(), this::refresh, 20, 20);
	}

	@Override
	public void unload() {
		super.unload();
		players = null;
		if (task != null)
			task.cancel();
	}

	@Override
	public void joined(@NotNull Player player, @NotNull Quester quester) {
		super.joined(player, quester);
		players.add(player);
	}

	@Override
	public void left(@NotNull Player player, @NotNull Quester quester) {
		super.left(player, quester);
		players.remove(player);
		lastDescriptions.remove(player);
	}

	@Override
	public void started(@NotNull Quester quester) {
		super.started(quester);
		players.addAll(quester.getOnlinePlayers());
	}

	@Override
	public void ended(@NotNull Quester quester) {
		super.ended(quester);
		players.removeAll(quester.getOnlinePlayers());
		quester.getOnlinePlayers().forEach(lastDescriptions::remove);
	}

	public static StageWaitRequirements deserialize(ConfigurationSection section, StageController controller) {
		return new StageWaitRequirements(controller);
	}

	public static class Creator extends StageCreation<StageWaitRequirements> {

		public Creator(@NotNull StageCreationContext<StageWaitRequirements> context) {
			super(context);
		}

		@Override
		public void start(@NotNull Player p) {
			super.start(p);
			QuestsAPI.getAPI().getRequirements().createGUI(QuestObjectLocation.STAGE, requirements -> {
				setRequirements(requirements);
				context.reopenGui();
			}, getRequirements()).open(p);
		}

		@Override
		protected @NotNull StageWaitRequirements finishStage(@NotNull StageController branch) {
			return new StageWaitRequirements(branch);
		}

	}

}
