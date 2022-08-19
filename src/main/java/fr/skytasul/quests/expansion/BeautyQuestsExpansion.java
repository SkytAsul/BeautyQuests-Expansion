package fr.skytasul.quests.expansion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

import org.bukkit.plugin.java.JavaPlugin;

import fr.skytasul.quests.BeautyQuests;
import fr.skytasul.quests.BeautyQuests.LoadingException;
import fr.skytasul.quests.api.Locale;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.options.QuestOptionCreator;
import fr.skytasul.quests.api.stages.StageType;
import fr.skytasul.quests.expansion.api.tracking.TrackerRegistry;
import fr.skytasul.quests.expansion.options.TimeLimitOption;
import fr.skytasul.quests.expansion.points.QuestPointsManager;
import fr.skytasul.quests.expansion.stages.StageStatistic;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import fr.skytasul.quests.gui.ItemUtils;
import fr.skytasul.quests.utils.XMaterial;
import fr.skytasul.quests.utils.configupdater.ConfigUpdater;
import fr.skytasul.quests.utils.logger.LoggerExpanded;

public class BeautyQuestsExpansion extends JavaPlugin {
	
	public static LoggerExpanded logger;
	private static BeautyQuestsExpansion instance;
	
	public static BeautyQuestsExpansion getInstance() {
		return instance;
	}
	
	private Handler loggerHandler;
	private List<ExpansionFeature> features = new ArrayList<>();
	
	private TrackerRegistry trackersRegistry;
	private QuestPointsManager pointsManager;
	
	@Override
	public void onLoad() {
		instance = this;
		logger = new LoggerExpanded(getLogger());
		loggerHandler = BeautyQuests.getInstance().getLoggerHandler().getSubhandler("Expansion");
		if (loggerHandler != null) getLogger().addHandler(loggerHandler);
	}
	
	@Override
	public void onEnable() {
		try {
			logger.info("------- BeautyQuests Expansion -------");
			logger.info("Thank you for purchasing the expansion!");
			
			if (!BeautyQuests.getInstance().isEnabled())
				throw new LoadingException("BeautyQuests has not been properly loaded.");
			
			logMessage("Hooked expansion version " + getDescription().getVersion());
			
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
		BeautyQuests.getInstance().getCommand().registerCommandsClass(new ExpansionCommands());
	}
	
	@Override
	public void onDisable() {
		unloadFeatures();
	}
	
	private void loadConfig() {
		try {
			saveDefaultConfig();
			ConfigUpdater.update(this, "config.yml", new File(getDataFolder(), "config.yml"));
		}catch (IOException ex) {
			logger.severe("An exception occurred while loading config file.", ex);
		}
	}
	
	private void loadLang() throws LoadingException {
		try {
			Locale.loadLang(this, LangExpansion.values(), getConfig().getString("lang"));
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
				return LangExpansion.Tracking_Description.format(trackersRegistry == null ? "x" : trackersRegistry.getCreators().size());
			}
		});
		features.add(new ExpansionFeature(
				LangExpansion.TimeLimit_Name.toString(),
				LangExpansion.TimeLimit_Description.toString(),
				() -> QuestsAPI.registerQuestOption(new QuestOptionCreator<>("timeLimit", 40, TimeLimitOption.class, TimeLimitOption::new, 0)),
				null));
		features.add(new ExpansionFeature(
				LangExpansion.Stage_Statistic_Name.toString(),
				LangExpansion.Stage_Statistic_Description.toString(),
				() -> QuestsAPI.getStages().register(new StageType<>(
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
				() -> pointsManager = new QuestPointsManager(),
				null));
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
	
	public void logMessage(String message) {
		BeautyQuests.getInstance().getLoggerHandler().write(message, "Expansion");
	}
	
}
