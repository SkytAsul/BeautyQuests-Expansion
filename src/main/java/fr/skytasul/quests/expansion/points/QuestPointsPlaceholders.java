package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.api.players.PlayerManager;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.points.QuestPointsLeaderboard.LeaderboardEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuestPointsPlaceholders extends PlaceholderExpansion {

	private static final Pattern TOP_PATTERN = Pattern.compile("^top_(\\d+)_(name|points)$");

	private final QuestPointsManager points;
	private final List<String> placeholders;

	public QuestPointsPlaceholders(QuestPointsManager points) {
		this.points = points;
		this.placeholders = new ArrayList<>(3);
		placeholders.add("player");
		if (points.getLeaderboard() != null) {
			placeholders.add("top_X_name");
			placeholders.add("top_X_points");
		}
	}

	@Override
	public @NotNull String getAuthor() {
		return "SkytAsul";
	}

	@Override
	public @NotNull String getIdentifier() {
		return "beautyquests-expansion-points";
	}

	@Override
	public @NotNull String getVersion() {
		return BeautyQuestsExpansion.getInstance().getDescription().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public @NotNull List<String> getPlaceholders() {
		return placeholders;
	}

	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		if (params.equals("player")) {
			if (player == null || !player.isOnline()) return "§cerror: offline";
			Quester quester = PlayerManager.getPlayerAccount(player.getPlayer());
			if (quester == null)
				return "§cerror: cannot find account of " + player.getPlayer().getName();
			return Integer.toString(points.getPoints(quester));
		}

		Matcher matcher = TOP_PATTERN.matcher(params);
		if (matcher.matches()) {
			if (points.getLeaderboard() == null) {
				return "§cerror: need database";
			} else {
				int rank = Integer.parseInt(matcher.group(1));
				LeaderboardEntry entry = points.getLeaderboard().getPlayer(rank);
				if (entry == null)
					return "";

				switch (matcher.group(2)) {
					case "name":
						return entry.getName();
					case "points":
						return Integer.toString(entry.getPoints());
					default:
						throw new IllegalArgumentException("Impossible");
				}
			}
		}
		return null;
	}

}
