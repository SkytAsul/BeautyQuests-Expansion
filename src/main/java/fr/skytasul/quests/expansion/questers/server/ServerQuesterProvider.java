package fr.skytasul.quests.expansion.questers.server;

import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.questers.QuesterManager;
import fr.skytasul.quests.api.questers.QuesterProvider;
import fr.skytasul.quests.api.questers.data.QuesterData;
import fr.skytasul.quests.api.questers.data.QuesterDataManager.QuesterFetchRequest;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.questers.AbstractQuesterImplementation;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;
import java.util.Collection;
import java.util.List;

public class ServerQuesterProvider implements QuesterProvider {

	private static final LoggerExpanded LOGGER = LoggerExpanded.get("BeautyQuests-Expansion.ServerQuester");

	private static final Key KEY = Key.key("beautyquests-expansion", "server");
	private static final String IDENTIFIER = "server";

	private ServerQuester quester;
	private List<ServerQuester> questerList;

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
				}, "Failed to load data of server quester.", null));
	}

	public @Nullable Quester getQuester() {
		return quester;
	}

	private class ServerQuester extends AbstractQuesterImplementation {

		protected ServerQuester(@NotNull QuesterProvider provider, @NotNull QuesterData dataHolder) {
			super(provider, dataHolder);
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
