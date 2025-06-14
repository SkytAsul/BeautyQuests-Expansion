package fr.skytasul.quests.expansion.points;

import com.cryptomorin.xseries.XMaterial;
import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.api.data.SavableData;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.requirements.RequirementCreator;
import fr.skytasul.quests.api.rewards.RewardCreator;
import fr.skytasul.quests.api.utils.IntegrationManager.BQDependency;
import fr.skytasul.quests.api.utils.messaging.PlaceholderRegistry;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.ExpansionConfiguration.QuestPointsConfiguration;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import fr.skytasul.quests.questers.data.sql.SqlDataManager;
import org.bukkit.Bukkit;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.exception.UnknownCommandException;
import revxrsal.commands.node.ExecutionContext;
import revxrsal.commands.orphan.OrphanCommand;

public class QuestPointsManager implements OrphanCommand {

	protected final SavableData<Integer> pointsData = new SavableData<>("points", Integer.class, 0);

	@Nullable
	private QuestPointsLeaderboard leaderboard;

	private final @NotNull QuestPointsConfiguration config;
	private final @NotNull BeautyQuests plugin;

	public QuestPointsManager(@NotNull QuestPointsConfiguration config, @NotNull BeautyQuests plugin) {
		this.config = config;
		this.plugin = plugin;

		plugin.getAPI().getQuesterManager().addSavableData(pointsData);

		if (plugin.getQuesterManager().getDataManager() instanceof SqlDataManager sqlDataManager) {
			leaderboard = new QuestPointsLeaderboard(this, sqlDataManager);
		} else {
			BeautyQuestsExpansion.logger.warning(
					"You are not using a database to save BeautyQuests datas. Quest points leaderboard is disabled.");
		}

		plugin.getAPI().getRewards().register(new RewardCreator(
				"points",
				QuestPointsReward.class,
				ItemUtils.item(XMaterial.EMERALD, "§a" + LangExpansion.Points_Name.toString(),
						QuestOption.formatDescription(LangExpansion.Points_Reward_Description.toString()),
						"",
						QuestOption.formatDescription(LangExpansion.Points_Description.toString()),
						"",
						LangExpansion.Expansion_Label.toString()),
				QuestPointsReward::new));
		plugin.getAPI().getRequirements().register(new RequirementCreator(
				"points",
				QuestPointsRequirement.class,
				ItemUtils.item(XMaterial.EMERALD, "§a" + LangExpansion.Points_Name.toString(),
						QuestOption.formatDescription(LangExpansion.Points_Requirement_Description.toString()),
						"",
						QuestOption.formatDescription(LangExpansion.Points_Description.toString()),
						"",
						LangExpansion.Expansion_Label.toString()),
				QuestPointsRequirement::new));

		plugin.getCommand().registerCommands("points", this);

		plugin.getIntegrationManager().addDependency(new BQDependency("Rankup", () -> {
			Bukkit.getPluginManager().registerEvents(new QuestPointsRankup(this, plugin.getPlayersManager()), plugin);
			BeautyQuestsExpansion.logger.info("Registered Rankup quest points requirements.");
		}));
		plugin.getIntegrationManager().addDependency(new BQDependency("PlaceholderAPI", () -> {
			new QuestPointsPlaceholders(this).register();
			BeautyQuestsExpansion.logger.info("Registered quest points placeholders.");
		}));
	}

	public int getPoints(Quester quester) {
		return quester.getDataHolder().getData(pointsData);
	}

	public void addPoints(Quester quester, int points) throws IllegalPointsBalanceException {
		int newBalance = getPoints(quester) + points;
		if (newBalance < 0) {
			switch (config.getNegativeBehavior()) {
				case ALLOW:
					// nothing to do here
					break;
				case FAIL:
					throw new IllegalPointsBalanceException(quester, newBalance);
				case FLOOR:
					newBalance = 0;
					break;
			}
		}

		quester.getDataHolder().setData(pointsData, newBalance);
	}

	public void unload() {
		if (leaderboard != null)
			leaderboard.unload();
	}

	@Nullable
	public QuestPointsLeaderboard getLeaderboard() {
		return leaderboard;
	}

	@CommandPlaceholder
	@CommandPermission (value = "beautyquests.expansion.command.points", defaultAccess = PermissionDefault.TRUE)
	public void pointsSelf(BukkitCommandActor actor, ExecutionContext<BukkitCommandActor> command,
			@Optional String subcommand) {
		if (subcommand != null)
			throw new UnknownCommandException(command.input().source());
		int points = getPoints(plugin.getPlayersManager().getQuester(actor.requirePlayer()));
		LangExpansion.Points_Command_Balance.quickSend(actor.sender(), "quest_points_balance", points);
	}

	@Subcommand ("get")
	@CommandPermission (value = "beautyquests.expansion.command.points.get", defaultAccess = PermissionDefault.OP)
	public void pointsGet(BukkitCommandActor actor, Quester quester) {
		int points = getPoints(quester);
		LangExpansion.Points_Command_Balance_Player.send(actor.sender(),
				PlaceholderRegistry.of("quest_points_balance", points, "target_name", quester.getFriendlyName()));
	}

	@Subcommand ("add")
	@CommandPermission (value = "beautyquests.expansion.command.points.add", defaultAccess = PermissionDefault.OP)
	public void pointsAdd(BukkitCommandActor actor, Quester quester, int points) throws IllegalPointsBalanceException {
		addPoints(quester, points);
		LangExpansion.Points_Command_Added.send(actor.sender(),
				PlaceholderRegistry.of("quest_points_balance", points, "target_name", quester.getFriendlyName()));
	}

}
