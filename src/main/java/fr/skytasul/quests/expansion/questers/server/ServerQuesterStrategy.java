package fr.skytasul.quests.expansion.questers.server;

import fr.skytasul.quests.api.QuestsAPI;
import fr.skytasul.quests.api.options.description.QuestDescriptionContext;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.quests.quester.QuestQuesterStrategy;
import fr.skytasul.quests.api.serializable.SerializableObject;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class ServerQuesterStrategy extends QuestQuesterStrategy {

	private final @NotNull ServerQuesterProvider questerProvider;

	private Optional<? extends Quester> questerOpt;

	public ServerQuesterStrategy(@NotNull ServerQuesterProvider questerProvider) {
		super(QuestsAPI.getAPI().getQuestQuesterStrategyRegistry());
		this.questerProvider = questerProvider;
	}

	@Override
	public @NotNull Optional<? extends Quester> getPlayerQuester(@NotNull Player player) {
		if (questerOpt == null)
			questerOpt = Optional.of(questerProvider.getQuester());
		return questerOpt;
	}

	@Override
	public boolean isQuesterApplicable(@NotNull Quester quester) {
		return questerProvider.getQuester().equals(quester);
	}

	@Override
	public boolean shouldAllPlayersMatchRequirements() {
		return false;
	}

	@Override
	public @Nullable String getTooltip(@NotNull QuestDescriptionContext context) {
		return LangExpansion.Quester_Server_Tooltip.toString();
	}

	@Override
	public @NotNull SerializableObject clone() {
		return this;
	}

	@Override
	public void save(@NotNull ConfigurationSection section) {}

	@Override
	public void load(@NotNull ConfigurationSection section) {}

}
