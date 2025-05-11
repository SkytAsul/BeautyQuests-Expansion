package fr.skytasul.quests.expansion.api.tracking;

import com.cryptomorin.xseries.XMaterial;
import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.QuestsPlugin;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.objects.QuestObjectsRegistry;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageType;
import fr.skytasul.quests.api.stages.options.StageOptionAutoRegister;
import fr.skytasul.quests.api.stages.options.StageOptionCreator;
import fr.skytasul.quests.api.stages.types.Locatable;
import fr.skytasul.quests.api.stages.types.Locatable.LocatedType;
import fr.skytasul.quests.api.utils.IntegrationManager.BQDependency;
import fr.skytasul.quests.api.utils.MinecraftVersion;
import fr.skytasul.quests.expansion.options.TrackingOption;
import fr.skytasul.quests.expansion.tracking.*;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import fr.skytasul.quests.integrations.worldguard.StageArea;
import org.jetbrains.annotations.NotNull;

public class TrackerRegistry extends QuestObjectsRegistry<Tracker, TrackerCreator> {

	public TrackerRegistry() {
		super("trackers", "Location trackers");

		registerTrackers();
		registerOption();
	}

	private void registerTrackers() {
		if (MinecraftVersion.MAJOR > 8)
			register(new TrackerCreator("particles", ParticleTracker.class, ItemUtils.item(XMaterial.SPLASH_POTION, LangExpansion.Tracking_Particles_Name.toString(), QuestOption.formatDescription(LangExpansion.Tracking_Particles_Description.toString())), ParticleTracker::new));
		if (MinecraftVersion.MAJOR >= 17)
			register(new TrackerCreator("block-outline", BlockOutlineTracker.class, ItemUtils.item(XMaterial.ACACIA_STAIRS, LangExpansion.Tracking_Outline_Block_Name.toString(), QuestOption.formatDescription(LangExpansion.Tracking_Outline_Block_Description.toString())), BlockOutlineTracker::new, type -> Locatable.hasLocatedTypes(type.getStageClass(), LocatedType.BLOCK)));
		if (MinecraftVersion.MAJOR >= 17)
			register(new TrackerCreator("glowing", GlowingTracker.class,
					ItemUtils.item(XMaterial.SPECTRAL_ARROW, LangExpansion.Tracking_Glowing_Name.toString(),
							QuestOption.formatDescription(LangExpansion.Tracking_Glowing_Description.toString())),
					GlowingTracker::new, GlowingTracker::isStageEnabled));
		if (MinecraftVersion.MAJOR >= 13)
			register(new TrackerCreator("beacon-beam", BeaconTracker.class,
					ItemUtils.item(XMaterial.BEACON, LangExpansion.Tracking_Beacon_Name.toString(),
							QuestOption.formatDescription(LangExpansion.Tracking_Beacon_Description.toString())),
					BeaconTracker::new, this::isPreciseLocatable));

		register(new TrackerCreator("region-outline", RegionOutlineTracker.class, ItemUtils.item(XMaterial.IRON_AXE, LangExpansion.Tracking_Outline_Region_Name.toString(), QuestOption.formatDescription(LangExpansion.Tracking_Outline_Region_Description.toString())), RegionOutlineTracker::new, type -> type.getStageClass() == StageArea.class));

		QuestsPlugin.getPlugin().getIntegrationManager().addDependency(new BQDependency("GPS", () -> {
			register(new TrackerCreator("gps", GpsTracker.class,
					ItemUtils.item(XMaterial.COMPASS, LangExpansion.Tracking_Gps_Name.toString(),
							QuestOption.formatDescription(LangExpansion.Tracking_Gps_Description.toString())),
					GpsTracker::new, this::isPreciseLocatable));
		}));
	}

	private void registerOption() {
		QuestsAPI.getAPI().getStages().autoRegisterOption(new StageOptionAutoRegister() {

			@SuppressWarnings("rawtypes")
			@Override
			public <T extends AbstractStage> StageOptionCreator<T> createOptionCreator(@NotNull StageType<T> type) {
				return createOptionCreatorInternal((StageType) type); // NOSONAR magic
			}

			private <T extends AbstractStage & Locatable> StageOptionCreator<T> createOptionCreatorInternal(
					@NotNull StageType<T> type) {
				return StageOptionCreator.create("expansion-tracking", TrackingOption.class,
						() -> new TrackingOption<>(type.getStageClass()));
			}

			@Override
			public boolean appliesTo(@NotNull StageType<?> type) {
				return Locatable.class.isAssignableFrom(type.getStageClass());
			}
		});
	}

	private boolean isPreciseLocatable(StageType<?> stageType) {
		return Locatable.PreciseLocatable.class.isAssignableFrom(stageType.getStageClass());
	}

}
