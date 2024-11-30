package fr.skytasul.quests.expansion.options;

import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.objects.QuestObjectLocation;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.players.PlayerAccount;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageController;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.options.StageOption;
import fr.skytasul.quests.api.stages.types.Locatable;
import fr.skytasul.quests.api.utils.XMaterial;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.api.tracking.Tracker;
import fr.skytasul.quests.expansion.api.tracking.TrackerCreator;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TrackingOption<T extends AbstractStage & Locatable> extends StageOption<T> {

	private @NotNull List<Tracker> trackers = Collections.emptyList();

	private int itemSlot;

	public TrackingOption(Class<T> stageClass) {
		super(stageClass);
	}

	private void setTrackers(List<Tracker> trackers) {
		this.trackers.forEach(Tracker::detach);

		this.trackers = Collections.unmodifiableList(trackers);

		// TODO attach the trackers
	}

	@Override
	public StageOption<T> clone() {
		TrackingOption<T> option = new TrackingOption<>(getStageClass());
		if (!trackers.isEmpty())
			option.trackers = trackers.stream().map(Tracker::clone).collect(Collectors.toList());
		return option;
	}

	@Override
	public boolean shouldSave() {
		return !trackers.isEmpty();
	}

	@Override
	public void save(ConfigurationSection section) {
		for (Tracker tracker : trackers) {
			tracker.save(section.createSection(tracker.getCreator().getID()));
		}
	}

	@Override
	public void load(ConfigurationSection section) {
		var trackers = new ArrayList<Tracker>();
		for (String key : section.getKeys(false)) {
			TrackerCreator creator = BeautyQuestsExpansion.getInstance().getTrackersRegistry().getByID(key);
			if (creator == null) {
				BeautyQuestsExpansion.getInstance().getLogger().warning("Cannot find tracker type " + key);
			} else {
				Tracker tracker = creator.newObject();
				tracker.load(section.getConfigurationSection(key));
				trackers.add(tracker);
			}
		}
		setTrackers(trackers);
	}

	@Override
	public void startEdition(StageCreation<T> creation) {
		itemSlot = creation.getLine().setItem(10,
				ItemUtils.item(XMaterial.COMPASS, LangExpansion.Tracking_Gui_Name.toString(), getLore()), event -> {
					BeautyQuestsExpansion.getInstance().getTrackersRegistry()
							.createGUI(QuestObjectLocation.OTHER, trackers -> {
								setTrackers(trackers);
								creation.getLine().refreshItemLore(itemSlot, getLore());
								event.reopen();
							}, trackers, creator -> creator.matches(creation.getCreationContext().getType()))
							.open(event.getPlayer());
				});
	}

	private String[] getLore() {
		return new String[] {QuestOption.formatDescription(
				LangExpansion.Tracking_Trackers.quickFormat("trackers_amount", trackers.size())), "",
				LangExpansion.Expansion_Label.toString()};
	}

	@Override
	public void stageLoad(StageController stage) {
		trackers.forEach(x -> x.start((T) stage.getStage()));
	}

	@Override
	public void stageUnload(StageController stage) {
		trackers.forEach(Tracker::stop);
	}

	@Override
	public void stageStart(PlayerAccount acc, StageController stage) {
		if (acc.isCurrent())
			showTrackers(acc.getPlayer(), (T) stage.getStage());
	}

	@Override
	public void stageJoin(Player p, StageController stage) {
		showTrackers(p, (T) stage.getStage());
	}

	@Override
	public void stageEnd(PlayerAccount acc, StageController stage) {
		if (acc.isCurrent()) hideTrackers(acc.getPlayer());
	}

	@Override
	public void stageLeave(Player p, StageController stage) {
		hideTrackers(p);
	}

	private void showTrackers(Player player, Locatable stage) {
		if (stage.isShown(player))
			trackers.forEach(x -> x.show(player));
	}

	private void hideTrackers(Player p) {
		trackers.forEach(x -> x.hide(p));
	}

}
