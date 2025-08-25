package fr.skytasul.quests.expansion.questers.server;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.questers.QuesterManager;
import fr.skytasul.quests.api.questers.QuesterProvider;
import fr.skytasul.quests.api.questers.data.QuesterData;
import fr.skytasul.quests.api.questers.data.QuesterDataManager.QuesterFetchRequest;
import fr.skytasul.quests.api.quests.Quest;
import fr.skytasul.quests.api.quests.events.QuestCreateEvent;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.options.OptionAutoQuest;
import fr.skytasul.quests.questers.AbstractQuesterImplementation;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import java.util.Collection;
import java.util.List;

public class ServerQuesterProvider implements QuesterProvider, Listener {

	private static final LoggerExpanded LOGGER = LoggerExpanded.get("BeautyQuests Expansion.ServerQuester");

	private static final Key KEY = Key.key("beautyquests-expansion", "server");
	private static final String IDENTIFIER = "server";

	private final BeautyQuestsExpansion plugin;

	private ServerQuester quester;
	private List<ServerQuester> questerList;

	public ServerQuesterProvider(@NotNull BeautyQuestsExpansion plugin) {
		this.plugin = plugin;
	}

	@Override
	public @NotNull Key key() {
		return KEY;
	}

	@Override
	public @NotNull @UnmodifiableView Collection<? extends Quester> getPlayerQuesters(@NotNull Player player) {
		return questerList;
	}

	@Override
	public @NotNull @UnmodifiableView Collection<? extends Quester> getLoadedQuesters() {
		return questerList;
	}

	@Override
	public void load(@NotNull QuesterManager questerManager) {
		questerManager.getDataManager().fetchQuester(new QuesterFetchRequest(KEY, IDENTIFIER, true, true))
				.whenComplete(LOGGER.logError(result -> {
					if (!result.type().isSuccess()) {
						LOGGER.severe("Failed to load data of server quester.");
						return;
					}
					quester = new ServerQuester(this, result.dataHandler());
					questerList = List.of(quester);

					LOGGER.debug("Loaded Server quester.");

					Bukkit.getScheduler().runTask(plugin, () -> {
						for (var quest : QuestsAPI.getAPI().getQuestsManager().getQuests())
							startAutomaticQuest(quest);
					});

					Bukkit.getPluginManager().registerEvents(this, plugin);
				}, "Failed to load data of server quester.", null));
	}

	private void startAutomaticQuest(@NotNull Quest quest) {
		if (quest.getOptionValueOrDef(OptionAutoQuest.class) && !quester.getDataHolder().hasQuestData(quest)
				&& quest.getQuesterStrategy().isQuesterApplicable(quester)) {
			LOGGER.debug(
					"Starting the quest {} for server quester since it is an automatic quest and no previous data has been found.",
					quest.getId());
			quest.start(quester, false);
		}
	}

	@EventHandler
	public void onQuestCreated(QuestCreateEvent event) {
		startAutomaticQuest(event.getQuest());
	}

	public @Nullable Quester getQuester() {
		return quester;
	}

	private class ServerQuester extends AbstractQuesterImplementation implements ForwardingAudience {

		private final Iterable<? extends Audience> playersAudience =
				List.of(BeautyQuests.getInstance().getAudiences().players());

		protected ServerQuester(@NotNull QuesterProvider provider, @NotNull QuesterData dataHolder) {
			super(provider, dataHolder);
		}

		@Override
		public @NotNull Iterable<? extends Audience> audiences() {
			return playersAudience;
		}

		@Override
		public boolean isActive() {
			return true;
		}

		@Override
		public @NotNull String getIdentifier() {
			return IDENTIFIER;
		}

		@Override
		public @NotNull String getFriendlyName() {
			return "Server";
		}

		@Override
		public @NotNull String getDetailedName() {
			return "Server";
		}

		@Override
		public @NotNull Collection<OfflinePlayer> getOfflinePlayers() {
			return List.of(Bukkit.getOfflinePlayers());
		}

		@Override
		public @NotNull Collection<Player> getOnlinePlayers() {
			return (@NotNull Collection<Player>) Bukkit.getOnlinePlayers();
		}

	}

}
