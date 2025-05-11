package fr.skytasul.quests.expansion.points;

import fr.skytasul.quests.api.editors.TextEditor;
import fr.skytasul.quests.api.editors.parsers.NumberParser;
import fr.skytasul.quests.api.objects.QuestObjectClickEvent;
import fr.skytasul.quests.api.rewards.AbstractReward;
import fr.skytasul.quests.api.rewards.RewardGiveContext;
import fr.skytasul.quests.api.utils.messaging.PlaceholderRegistry;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ThreadLocalRandom;

public class QuestPointsReward extends AbstractReward {

	private int min;
	private int max;

	public QuestPointsReward() {}

	public QuestPointsReward(String customDescription, int min, int max) {
		super(customDescription);
		this.min = min;
		this.max = max;
	}

	@Override
	public void give(@NotNull RewardGiveContext context) {
		try {
			int points = min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
			BeautyQuestsExpansion.getInstance().getPointsManager().addPoints(context.getQuester(), points);
			context.addEarning(LangExpansion.Points_Amount.quickFormat("quest_points_amount", points));
		} catch (IllegalPointsBalanceException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	protected void createdPlaceholdersRegistry(@NotNull PlaceholderRegistry placeholders) {
		super.createdPlaceholdersRegistry(placeholders);
		placeholders.register("quest_points_min", () -> Integer.toString(min));
		placeholders.register("quest_points_max", () -> Integer.toString(max));
		placeholders.registerIndexed("quest_points_range", () -> min == max ? Integer.toString(min) : (min + " - " + max));
	}

	@Override
	public String getDefaultDescription(Player p) {
		return LangExpansion.Points_Reward_Tooltip.toString();
	}

	@Override
	public AbstractReward clone() {
		return new QuestPointsReward(getCustomDescription(), min, max);
	}

	@Override
	public void itemClick(QuestObjectClickEvent event) {
		LangExpansion.Points_Reward_Editor_Min.send(event.getPlayer());
		new TextEditor<>(event.getPlayer(), event::cancel, newMin -> {
			LangExpansion.Points_Reward_Editor_Max.send(event.getPlayer());
			new TextEditor<>(event.getPlayer(), event::cancel, newMax -> {
				min = newMin;
				max = newMax;

				event.reopenGUI();
			}, NumberParser.INTEGER_PARSER_POSITIVE).start();
		}, NumberParser.INTEGER_PARSER_POSITIVE).start();
	}

	@Override
	public void save(ConfigurationSection section) {
		section.set("min", min);
		section.set("max", max);
	}

	@Override
	public void load(ConfigurationSection section) {
		min = section.getInt("min");
		max = section.getInt("max");
	}

}
