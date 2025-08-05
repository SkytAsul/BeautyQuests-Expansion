package fr.skytasul.quests.expansion.options;

import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageType;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.options.StageOption;
import fr.skytasul.quests.api.stages.options.StageOptionAutoRegister;
import fr.skytasul.quests.api.stages.options.StageOptionCreator;
import fr.skytasul.quests.api.stages.options.StageQuesterStrategy;
import fr.skytasul.quests.api.utils.logger.LoggerExpanded;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import me.pikamug.unite.api.objects.PartyProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PartyProgressStageOption<T extends AbstractStage> extends StageOption<T> implements StageQuesterStrategy {

	private static final LoggerExpanded LOGGER = LoggerExpanded.get("BeautyQuests Expansion.PartyProgressStageOption");

	private boolean enabled;

	public PartyProgressStageOption(@NotNull Class<? extends T> stageClass, boolean enabled) {
		super(stageClass);
		this.enabled = enabled;
	}

	@Override
	public @NotNull StageOption<T> clone() {
		return new PartyProgressStageOption<>(getStageClass(), enabled);
	}

	@Override
	public void startEdition(@NotNull StageCreation<T> creation) {
		creation.getLine().setItem(13, ItemUtils.itemSwitch(LangExpansion.Party_Progress_Name.toString(), enabled,
				QuestOption.formatDescription(LangExpansion.Party_Progress_Description.toString())), event -> {
			enabled = ItemUtils.toggleSwitch(event.getClicked());
		});
	}

	@Override
	public boolean shouldSave() {
		return enabled;
	}

	@Override
	public void save(@NotNull ConfigurationSection section) {
		section.set("enabled", enabled);
	}

	@Override
	public void load(@NotNull ConfigurationSection section) {
		enabled = section.getBoolean("enabled", false);
	}

	@Override
	public Collection<? extends Quester> getAdditionalQuesters(@NotNull Player player) {
		if (!enabled)
			return List.of();

		var partyProviderRegistration = Bukkit.getServicesManager().getRegistration(PartyProvider.class);
		if (partyProviderRegistration == null) {
			LOGGER.namedWarning("Tried to fetch the members of a party but no compatible party plugin is installed.",
					"no-provider", 60);
			return List.of();
		}

		var partyProvider = partyProviderRegistration.getProvider();
		if (!partyProvider.isPlayerInParty(player))
			return List.of();

		String partyId = partyProvider.getPartyId(player.getUniqueId());
		if (partyId == null)
			return List.of();

		var quest = getStageController().orElseThrow().getBranch().getQuest();
		var questQuesterStrategy = quest.getQuesterStrategy();
		return partyProvider.getOnlineMembers(partyId).stream()
				.map(uuid -> questQuesterStrategy.getPlayerQuester(Bukkit.getPlayer(uuid)))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.toList();
	}

	public static class AutoRegister implements StageOptionAutoRegister {

		@Override
		public boolean appliesTo(@NotNull StageType<?> type) {
			return true;
		}

		@Override
		public <T extends AbstractStage> StageOptionCreator<T> createOptionCreator(@NotNull StageType<T> type) {
			return StageOptionCreator.create("partyProgress", PartyProgressStageOption.class,
					() -> new PartyProgressStageOption<>(type.getStageClass(), false));
		}

	}

}
