package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.api.questers.QuesterManager;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.questers.data.sql.SqlDataManager;
import net.kyori.adventure.key.Key;
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

public class QuestPointsLeaderboard {

	private static final LoggerExpanded LOGGER = LoggerExpanded.get("BeautyQuests-Expansion.QuestPointsLeaderboard");

	private static final long CACHE_TIME_TICKS = 40 * 20;
	private static final LeaderboardEntry LOADING_ENTRY = new LeaderboardEntry("loading...", 0);

	private final QuestPointsManager pointsManager;
	private final SqlDataManager dbManager;
	private final QuesterManager questerManager;

	private BukkitTask refreshTask;
	private Map<Integer, LeaderboardEntry> cachedEntries;

	private int maxRankFetched;

	public QuestPointsLeaderboard(QuestPointsManager pointsManager, SqlDataManager dbManager,
			QuesterManager questerManager) {
		this.pointsManager = pointsManager;
		this.dbManager = dbManager;
		this.questerManager = questerManager;
	}

	private String getFetchStatement(int amount, int offset) {
		String pointsColumn = pointsManager.pointsData.getColumnName();
		return """
				SELECT `provider`, `identifier`, `%s`
				FROM %s
				WHERE %s > 0
				ORDER BY `%s` DESC
				LIMIT %d OFFSET %d
				""".formatted(pointsColumn, dbManager.getSqlHandler().QUESTERS_TABLE, pointsColumn, pointsColumn, amount,
				offset);
	}

	private void launchRefreshTask() {
		refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(BeautyQuestsExpansion.getInstance(), () -> {
			Map<Integer, LeaderboardEntry> firstEntries = fetchFirst(maxRankFetched);
			if (firstEntries == null)
				return;
			cachedEntries = firstEntries;
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

	private LeaderboardEntry getEntryFromRow(ResultSet resultSet) throws SQLException {
		String provider = resultSet.getString("provider");
		String identifier = resultSet.getString("identifier");
		int points = resultSet.getInt(pointsManager.pointsData.getColumnName());

		String name = "unknown";
		try {
			name = questerManager.getQuesterProvider(Key.key(provider)).getQuesterName(identifier).orElse("unknown");
		} catch (IllegalArgumentException ex) {
			LOGGER.warning("Failed to fetch quester {0} provider {1}", ex, identifier, provider);
		}

		return new LeaderboardEntry(name, points);
	}

	@Nullable
	private Map<Integer, LeaderboardEntry> fetchFirst(int amount) {
		try (Connection connection = dbManager.getSqlHandler().getDatabase().getConnection();
				Statement statement = connection.createStatement()) {
			Map<Integer, LeaderboardEntry> entries = new HashMap<>();
			ResultSet resultSet = statement.executeQuery(getFetchStatement(amount, 0));
			int index = 1;
			while (resultSet.next()) {
				entries.put(index, getEntryFromRow(resultSet));
				index++;
			}

			for (; index <= amount; index++) {
				entries.put(index, null);
			}

			return entries;
		} catch (SQLException ex) {
			LOGGER.severe("An exception occurred while trying to fetch leaderboard", ex);
			return null;
		}
	}

	@Nullable
	private LeaderboardEntry fetchRank(int rank) {
		try (Connection connection = dbManager.getSqlHandler().getDatabase().getConnection();
				Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(getFetchStatement(1, rank - 1));
			if (resultSet.next())
				return getEntryFromRow(resultSet);
		} catch (SQLException ex) {
			LOGGER.severe("An exception occurred while trying to fetch points for rank " + rank, ex);
		}
		return null;
	}

	public static record LeaderboardEntry(@NotNull String name, int points) {
	}

}
