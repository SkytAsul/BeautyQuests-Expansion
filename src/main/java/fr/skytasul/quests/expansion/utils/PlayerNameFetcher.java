package fr.skytasul.quests.expansion.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.skytasul.quests.api.QuestsPlugin;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerNameFetcher {

	private static Map<UUID, String> cachedPlayerNames = new HashMap<>();
	private static Gson gson = new Gson();
	private static long lastOnlineFailure = 0;

	private PlayerNameFetcher() {}

	public static synchronized @Nullable String getPlayerName(@NotNull UUID uuid) {
		if (cachedPlayerNames.containsKey(uuid))
			return cachedPlayerNames.get(uuid);

		String name = Bukkit.getOfflinePlayer(uuid).getName();
		if (name == null && Bukkit.getOnlineMode()) {
			try {
				if (System.currentTimeMillis() - lastOnlineFailure < 30_000) {
					QuestsPlugin.getPlugin().getLoggerExpanded()
							.debug("Trying to fetch a name from an UUID but it failed within 30 seconds.");
					return null;
				}

				HttpURLConnection connection = (HttpURLConnection) new URL(
						"https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();
				connection.setReadTimeout(5000);

				JsonObject profile = gson.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream())),
						JsonObject.class);
				JsonElement nameElement = profile.get("name");
				if (nameElement == null) {
					name = null;
					QuestsPlugin.getPlugin().getLoggerExpanded().debug("Cannot find name for UUID " + uuid.toString());
				} else {
					name = nameElement.getAsString();
				}
			} catch (Exception e) {
				QuestsPlugin.getPlugin().getLoggerExpanded()
						.warning("Cannot connect to the mojang servers. UUIDs cannot be parsed.");
				lastOnlineFailure = System.currentTimeMillis();
				return null;
			}
		}

		cachedPlayerNames.put(uuid, name);
		return name;
	}

}
