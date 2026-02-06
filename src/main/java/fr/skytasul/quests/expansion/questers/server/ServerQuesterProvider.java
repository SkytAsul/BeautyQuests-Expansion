package fr.skytasul.quests.expansion.questers.server;

import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.questers.QuesterManager;
import fr.skytasul.quests.api.questers.QuesterProvider;
import fr.skytasul.quests.api.questers.data.QuesterData;
import fr.skytasul.quests.api.questers.data.QuesterDataManager.QuesterFetchRequest;
import fr.skytasul.quests.api.questers.events.QuesterJoinEvent;
import fr.skytasul.quests.api.questers.events.QuesterLeaveEvent;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.questers.AbstractQuesterImplementation;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ServerQuesterProvider implements QuesterProvider, Listener {

	private static final LoggerExpanded LOGGER = LoggerExpanded.get("BeautyQuests Expansion.ServerQuester");

	private static final Key KEY = Key.key("beautyquests-expansion", "server");
	private static final String IDENTIFIER = "server";

	private final BeautyQuestsExpansion plugin;

	private ServerQuester quester;
	private List<ServerQuester> questerList = Collections.emptyList();

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

					for (Player player : Bukkit.getOnlinePlayers()) {
						Bukkit.getPluginManager().callEvent(new QuesterJoinEvent(quester, player, false));
					}

					Bukkit.getPluginManager().registerEvents(this, plugin);
				}, "Failed to load data of server quester.", null));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Bukkit.getPluginManager().callEvent(new QuesterJoinEvent(quester, event.getPlayer(), false));
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Bukkit.getPluginManager().callEvent(new QuesterLeaveEvent(quester, event.getPlayer()));
	}

	public @Nullable Quester getQuester() {
		return quester;
	}

	private class ServerQuester extends AbstractQuesterImplementation implements ForwardingAudience {

		private final Iterable<? extends Audience> playersAudience = List.of(plugin.getAudiences().players());

		protected ServerQuester(@NotNull QuesterProvider provider, @NotNull QuesterData dataHolder) {
			super(provider, dataHolder);
		}

		@Override
		public @NotNull Iterable<? extends Audience> audiences() {
			return playersAudience;
		}

		@Override
		public boolean isActive() {
			return !Bukkit.getOnlinePlayers().isEmpty();
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
