package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.utils.PlayerNameFetcher;
import fr.skytasul.quests.questers.data.sql.SqlDataManager;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class QuestPointsLeaderboard {

	private static final long CACHE_TIME_TICKS = 40 * 20;
	private static final LeaderboardEntry LOADING_ENTRY = new LeaderboardEntry(null, 0) {
		@Override
		public String getName() {
			return "§cloading...";
		}
	};

	private QuestPointsManager pointsManager;
	private SqlDataManager dbManager;

	private BukkitTask refreshTask;
	private Map<Integer, LeaderboardEntry> cachedEntries;

	private int maxRankFetched;

	private final String fetchFirstStatement;
	private final String fetchRankStatement;

	public QuestPointsLeaderboard(QuestPointsManager pointsManager, SqlDataManager dbManager) {
		this.pointsManager = pointsManager;
		this.dbManager = dbManager;

		fetchFirstStatement = "SELECT `player_uuid`, `" + pointsManager.pointsData.getColumnName() + "`"
				+ " FROM " + dbManager.getSqlHandler().ACCOUNTS_TABLE
				+ " WHERE " + pointsManager.pointsData.getColumnName() + " > 0"
				+ " ORDER BY `" + pointsManager.pointsData.getColumnName() + "` DESC"
				+ " LIMIT %d";
		fetchRankStatement = "SELECT `player_uuid`, `" + pointsManager.pointsData.getColumnName() + "`"
				+ " FROM " + dbManager.getSqlHandler().ACCOUNTS_TABLE
				+ " WHERE " + pointsManager.pointsData.getColumnName() + " > 0"
				+ " ORDER BY `" + pointsManager.pointsData.getColumnName() + "` DESC"
				+ " LIMIT 1 OFFSET %d";
	}

	private void launchRefreshTask() {
		refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BeautyQuestsExpansion.getInstance(), () -> {
			Map<Integer, LeaderboardEntry> firstEntries = fetchFirst(maxRankFetched);
			if (firstEntries == null)
				return;
			cachedEntries = firstEntries;
			cachedEntries.values().stream().filter(Objects::nonNull).forEach(LeaderboardEntry::fetchName);
		}, 20L, CACHE_TIME_TICKS);

		// we have a 20 ticks delay to wait for the "maxRankFetched" field
		// to have a reasonable value
		// because getPlayer(rank) will be called with rank 1 then 2 then...
		// so it's better to wait a bit in order to get the last rank fetched.
	}

	public void unload() {
		if (refreshTask != null) {
			refreshTask.cancel();
			refreshTask = null;
		}
	}

	@Nullable
	public LeaderboardEntry getPlayer(int rank) {
		if (maxRankFetched < rank)
			maxRankFetched = rank;

		if (refreshTask == null)
			launchRefreshTask();

		if (cachedEntries == null)
			return LOADING_ENTRY;

		// cannot use Map#computeIfAbsent as it won't work with null values...
		if (!cachedEntries.containsKey(rank))
			cachedEntries.put(rank, fetchRank(rank));
		return cachedEntries.get(rank);
	}

	@Nullable
	private Map<Integer, LeaderboardEntry> fetchFirst(int amount) {
		try (Connection connection = dbManager.getSqlHandler().getDatabase().getConnection();
				Statement statement = connection.createStatement()) {
			Map<Integer, LeaderboardEntry> entries = new HashMap<>();
			ResultSet resultSet = statement.executeQuery(String.format(fetchFirstStatement, amount));
			int index = 1;
			while (resultSet.next()) {
				UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
				int points = resultSet.getInt(pointsManager.pointsData.getColumnName());
				entries.put(index, new LeaderboardEntry(uuid, points));
				index++;
			}

			for (; index <= amount; index++) {
				entries.put(index, null);
			}

			return entries;
		} catch (SQLException ex) {
			BeautyQuestsExpansion.logger.severe("An exception occurred while trying to fetch points leaderboard", ex);
			return null;
		}
	}

	@Nullable
	private LeaderboardEntry fetchRank(int rank) {
		try (Connection connection = dbManager.getSqlHandler().getDatabase().getConnection();
				Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(String.format(fetchRankStatement, rank - 1));
			if (resultSet.next()) {
				UUID uuid = UUID.fromString(resultSet.getString("player_uuid"));
				int points = resultSet.getInt(pointsManager.pointsData.getColumnName());
				return new LeaderboardEntry(uuid, points);
			}
		} catch (SQLException ex) {
			BeautyQuestsExpansion.logger.severe("An exception occurred while trying to fetch points for rank " + rank, ex);
		}
		return null;
	}

	public static class LeaderboardEntry {

		private final UUID uuid;
		private final int points;

		private String name;

		public LeaderboardEntry(UUID uuid, int points) {
			this.uuid = uuid;
			this.points = points;
		}

		private void fetchName() {
			name = "loading...";
			name = PlayerNameFetcher.getPlayerName(uuid);
		}

		@NotNull
		public String getName() {
			return name == null ? "unknown" : name;
		}

		public int getPoints() {
			return points;
		}

	}

}
