package fr.skytasul.quests.expansion.utils;

import fr.skytasul.quests.api.localization.Lang;
import fr.skytasul.quests.api.localization.Locale;
import fr.skytasul.quests.api.utils.messaging.MessageType;
import fr.skytasul.quests.api.utils.messaging.MessageType.DefaultMessageType;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("squid:S115")
public enum LangExpansion implements Locale {

	Expansion_Label("expansion.label"),

	Features_Header("features.header"), // 0: amount enabled
	Features_Unloaded("features.unloaded"),

	TimeLimit_Name("timelimit.name"),
	TimeLimit_Description("timelimit.description"),
	TimeLimit_Left("timelimit.left"), // 0: left duration
	TimeLimit_EDITOR("timelimit.editor", Lang.EditorPrefix),

	Stage_Statistic_Name("stage.statistic.name"),
	Stage_Statistic_Description("stage.statistic.description"),
	Stage_Statistic_Advancement("stage.statistic.advancement"), // 0: statistic, 1: player remaining
	Stage_Statistic_Item_Stat("stage.statistic.gui.stat"),
	Stage_Statistic_Item_Limit("stage.statistic.gui.limit"),
	Stage_Statistic_Item_Relative("stage.statistic.gui.relative"),
	Stage_Statistic_Item_Relative_Description("stage.statistic.gui.relativeDescription"),
	Stage_Statistic_StatList_Gui_Name("stage.statistic.gui.stats.name"),
	Stage_Statistic_StatList_Gui_Block("stage.statistic.gui.stats.block"),
	Stage_Statistic_StatList_Gui_Item("stage.statistic.gui.stats.item"),
	Stage_Statistic_StatList_Gui_Entity("stage.statistic.gui.stats.entity"),
	Stage_Statistic_EDITOR_LIMIT("stage.statistic.editor.limit", Lang.EditorPrefix),

	Stage_Requirements_Name("stage.requirements.name"),
	Stage_Requirements_Description("stage.requirements.description"),

	Tracking_Trackers("tracking.trackers"), // 0: tracker amount
	Tracking_Name("tracking.name"),
	Tracking_Description("tracking.description"), // 0: available trackers
	Tracking_Gui_Name("tracking.gui.name"),
	Tracking_Particles_Name("tracking.particles.name"),
	Tracking_Particles_Description("tracking.particles.description"),
	Tracking_Outline_Block_Name("tracking.outlineBlock.name"),
	Tracking_Outline_Block_Description("tracking.outlineBlock.description"),
	Tracking_Outline_Region_Name("tracking.outlineRegion.name"),
	Tracking_Outline_Region_Description("tracking.outlineRegion.description"),
	Tracking_Beacon_Name("tracking.beacon.name"),
	Tracking_Beacon_Description("tracking.beacon.description"),
	Tracking_Glowing_Name("tracking.glowing.name"),
	Tracking_Glowing_Description("tracking.glowing.description"),
	Tracking_Gps_Name("tracking.gps.name"),
	Tracking_Gps_Description("tracking.gps.description"),

	Points_Name("points.name"),
	Points_Description("points.description"),
	Points_Amount("points.amount"),
	Points_Reward_Description("points.reward.description"),
	Points_Reward_Tooltip("points.reward.tooltip"),
	Points_Reward_Editor_Min("points.reward.editor.min", Lang.EditorPrefix),
	Points_Reward_Editor_Max("points.reward.editor.max", Lang.EditorPrefix),
	Points_Requirement_Description("points.requirement.description"),
	Points_Requirement_Message("points.requirement.message", Lang.RequirementNotMetPrefix), // 0: points amount required
	Points_Requirement_Editor_Target("points.requirement.editor.target", Lang.EditorPrefix),
	Points_Requirement_Tooltip("points.requirement.tooltip"),
	Points_Command_Balance("points.command.balance"), // 0: points
	Points_Command_Balance_Player("points.command.balancePlayer"), // 0: points, 1: player
	Points_Command_Added("points.command.added", Lang.SuccessPrefix), // 0: points, 1: player

	Quester_Server_Name("quester.server.name"),
	Quester_Server_Description("quester.server.description"),
	Quester_Server_Tooltip("quester.server.tooltip"),

	Party_Progress_Name("party progress.name"),
	Party_Progress_Description("party progress.description"),

	;

	private final String path;

	private String value = "§cnot loaded";
	private Locale prefix;

	private LangExpansion(String path) {
		this(path, null);
	}

	private LangExpansion(String path, Locale prefix) {
		this.path = path;
		this.prefix = prefix;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String getValue() {
		return prefix == null ? value : (prefix.toString() + value);
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public @NotNull MessageType getType() {
		return DefaultMessageType.PREFIXED;
	}

	@Override
	public String toString() {
		return getValue();
	}

}
