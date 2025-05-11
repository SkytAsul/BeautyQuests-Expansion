package fr.skytasul.quests.expansion;

import com.cryptomorin.xseries.XMaterial;
import com.tchristofferson.configupdater.ConfigUpdater;
import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.QuestsPlugin;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.localization.Locale;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.options.QuestOptionCreator;
import fr.skytasul.quests.api.stages.StageType;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.expansion.api.tracking.TrackerRegistry;
import fr.skytasul.quests.expansion.options.TimeLimitOption;
import fr.skytasul.quests.expansion.points.QuestPointsManager;
import fr.skytasul.quests.expansion.stages.StageStatistic;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BeautyQuestsExpansion extends JavaPlugin {

	public static LoggerExpanded logger;
	private static BeautyQuestsExpansion instance;

	public static BeautyQuestsExpansion getInstance() {
		return instance;
	}

	private List<ExpansionFeature> features = new ArrayList<>();

	private ExpansionConfiguration config;
	private TrackerRegistry trackersRegistry;
	private QuestPointsManager pointsManager;

	@Override
	public void onLoad() {
		instance = this;
		try {
			logger = new LoggerExpanded(getLogger());

			if (BeautyQuests.getInstance().getLoggerHandler() != null)
				getLogger().addHandler(BeautyQuests.getInstance().getLoggerHandler());
		}catch (Throwable ex) {
			getLogger().severe("Failed to inject custom loggers. This may be due to BeautyQuests being outdated.");
		}
	}

	@Override
	public void onEnable() {
		try {
			logger.info("------- BeautyQuests Expansion -------");
			logger.info("Thank you for purchasing the expansion!");

			if (!QuestsPlugin.getPlugin().isEnabled())
				throw new LoadingException("BeautyQuests has not been properly loaded.");

			if (!isBQUpToDate())
				throw new LoadingException("This version of the expansion is not compatible with the version of BeautyQuests.");

			logger.info("Hooked expansion version " + getDescription().getVersion());

			loadConfig();
			loadLang();

			addDefaultFeatures();
			loadFeatures();

			registerCommands();
		}catch (LoadingException ex) {
			if (ex.getCause() != null) logger.severe("A fatal error occurred while loading plugin.", ex.getCause());
			logger.severe(ex.getLoggerMessage());
			logger.severe("This is a fatal error. Now disabling.");
			setEnabled(false);
		}
	}

	private void registerCommands() {
		QuestsPlugin.getPlugin().getCommand().registerCommands("", new ExpansionCommands());
	}

	@Override
	public void onDisable() {
		unloadFeatures();
	}

	@SuppressWarnings("unused") // because we don't always use major, minor and revision at the same time
	private boolean isBQUpToDate() {
		Pattern bqVersion = Pattern.compile("(\\d+)\\.(\\d+)(?>\\.(\\d+))?(?>\\+build\\.(.+))?");
		Matcher matcher = bqVersion.matcher(QuestsPlugin.getPlugin().getDescription().getVersion());
		if (matcher.find()) {
			int major = Integer.parseInt(matcher.group(1));
			int minor = Integer.parseInt(matcher.group(2));
			String revisionStr = matcher.group(3);
			int revision = revisionStr == null ? 0 : Integer.parseInt(revisionStr);
			String buildStr = matcher.group(4);
			boolean useBuildNumber = false;

			if (!useBuildNumber || buildStr == null) {
				// means it's a release: we must use the major/minor/revision numbers
				return major >= 2 && revision >= 0;
			} else {
				// we have build number: it's easier to just use it instead of the version numbers
				try {
					int build = Integer.parseInt(buildStr);
					return build >= 15;
				}catch (NumberFormatException ex) {
					logger.warning(
							"Cannot parse BeautyQuests version. This version of the expansion might not be compatible.");
				}
			}
		} else {
			// Probably using old BQ versioning scheme (not semver-compliant)
			logger.warning("Cannot parse BeautyQuests version. Are you using the latest one?");
		}
		return true;
	}

	private void loadConfig() {
		try {
			saveDefaultConfig();
			ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
			config = new ExpansionConfiguration(getConfig());
		}catch (IOException ex) {
			logger.severe("An exception occurred while loading config file.", ex);
		}
	}

	private void loadLang() throws LoadingException {
		try {
			Locale.loadLang(this, LangExpansion.values(), config.getLang());
		}catch (Exception ex) {
			throw new LoadingException("Couldn't load language file.", ex);
		}
	}

	private void addDefaultFeatures() {
		features.add(new ExpansionFeature(
				LangExpansion.Tracking_Name.toString(),
				LangExpansion.Tracking_Description.toString(),
				() -> trackersRegistry = new TrackerRegistry(),
				null) {
			@Override
			public String getDescription() {
				return LangExpansion.Tracking_Description.quickFormat("trackers_amount",
						trackersRegistry == null ? "x" : trackersRegistry.getCreators().size());
			}
		});
		features.add(new ExpansionFeature(
				LangExpansion.TimeLimit_Name.toString(),
				LangExpansion.TimeLimit_Description.toString(),
				() -> QuestsAPI.getAPI().registerQuestOption(
						new QuestOptionCreator<>("timeLimit", 42, TimeLimitOption.class, TimeLimitOption::new, 0)),
				null));
		features.add(new ExpansionFeature(
				LangExpansion.Stage_Statistic_Name.toString(),
				LangExpansion.Stage_Statistic_Description.toString(),
				() -> QuestsAPI.getAPI().getStages().register(new StageType<>(
						"statistic",
						StageStatistic.class,
						LangExpansion.Stage_Statistic_Name.toString(),
						StageStatistic::deserialize,
						ItemUtils.item(XMaterial.FEATHER, "§a" + LangExpansion.Stage_Statistic_Name.toString(), QuestOption.formatDescription(LangExpansion.Stage_Statistic_Description.toString()), "", LangExpansion.Expansion_Label.toString()),
						StageStatistic.Creator::new)),
				null));
		features.add(new ExpansionFeature(
				LangExpansion.Points_Name.toString(),
				LangExpansion.Points_Description.toString(),
				() -> pointsManager = new QuestPointsManager(config.getPointsConfig()),
                () -> pointsManager.unload())); // cannot use pointsManager::unload here as the field is not yet initialized
	}

	private void loadFeatures() {
		int loaded = 0;

		for (ExpansionFeature feature : features) {
			if (feature.load()) loaded++;
		}

		logger.info(loaded + " expanded features have been loaded.");
	}

	private void unloadFeatures() {
		features.forEach(ExpansionFeature::unload);
	}

	public List<ExpansionFeature> getFeatures() {
		return features;
	}

	public TrackerRegistry getTrackersRegistry() {
		return trackersRegistry;
	}

	public QuestPointsManager getPointsManager() {
		return pointsManager;
	}

	public static class LoadingException extends Exception {
		private static final long serialVersionUID = -2811265488885752109L;

		private String loggerMessage;

		public LoadingException(String loggerMessage) {
			this.loggerMessage = loggerMessage;
		}

		public LoadingException(String loggerMessage, Throwable cause) {
			super(cause);
			this.loggerMessage = loggerMessage;
		}

		public String getLoggerMessage() {
			return loggerMessage;
		}

	}

}
