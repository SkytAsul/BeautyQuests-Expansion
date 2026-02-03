package fr.skytasul.quests.expansion.stages;

import com.cryptomorin.xseries.XMaterial;
import fr.skytasul.quests.api.QuestsPlugin;
import fr.skytasul.quests.api.editors.TextEditor;
import fr.skytasul.quests.api.editors.parsers.NumberParser;
import fr.skytasul.quests.api.gui.ItemUtils;
import fr.skytasul.quests.api.gui.templates.StaticPagedGUI;
import fr.skytasul.quests.api.options.QuestOption;
import fr.skytasul.quests.api.players.PlayerQuester;
import fr.skytasul.quests.api.questers.Quester;
import fr.skytasul.quests.api.stages.AbstractStage;
import fr.skytasul.quests.api.stages.StageController;
import fr.skytasul.quests.api.stages.StageDescriptionPlaceholdersContext;
import fr.skytasul.quests.api.stages.creation.StageCreation;
import fr.skytasul.quests.api.stages.creation.StageCreationContext;
import fr.skytasul.quests.api.utils.ComparisonMethod;
import fr.skytasul.quests.api.utils.messaging.PlaceholderRegistry;
import fr.skytasul.quests.api.utils.progress.HasProgress;
import fr.skytasul.quests.api.utils.progress.ProgressPlaceholders;
import fr.skytasul.quests.expansion.BeautyQuestsExpansion;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.Statistic.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StageStatistic extends AbstractStage implements HasProgress {

	private final Statistic statistic;
	private final Material offsetMaterial;
	private final EntityType offsetEntity;

	private final int limit;
	private final ComparisonMethod comparison;
	private final boolean relative;

	private BukkitTask task;
	private List<Player> players;

	private Map<Quester, Integer> lastValues = new HashMap<>();

	public StageStatistic(StageController controller, Statistic statistic, int limit, ComparisonMethod comparison,
			boolean relative) {
		super(controller);

		this.statistic = statistic;
		this.offsetMaterial = null;
		this.offsetEntity = null;

		this.limit = limit;
		this.comparison = comparison;
		this.relative = relative;
	}

	public StageStatistic(StageController controller, Statistic statistic, Material offsetMaterial, int limit,
			ComparisonMethod comparison, boolean relative) {
		super(controller);

		this.statistic = statistic;
		this.offsetMaterial = offsetMaterial;
		this.offsetEntity = null;

		this.limit = limit;
		this.comparison = comparison;
		this.relative = relative;
	}

	public StageStatistic(StageController controller, Statistic statistic, EntityType offsetEntity, int limit,
			ComparisonMethod comparison, boolean relative) {
		super(controller);

		this.statistic = statistic;
		this.offsetMaterial = null;
		this.offsetEntity = offsetEntity;

		this.limit = limit;
		this.comparison = comparison;
		this.relative = relative;
	}

	@Override
	public @NotNull String getDefaultDescription(@NotNull StageDescriptionPlaceholdersContext context) {
		return LangExpansion.Stage_Statistic_Advancement.toString();
	}

	@Override
	protected void createdPlaceholdersRegistry(@NotNull PlaceholderRegistry placeholders) {
		super.createdPlaceholdersRegistry(placeholders);

		String offsetName = getOffsetName();
		placeholders.registerIndexed("statistic_type_name",
				statistic.name() + (offsetName == null ? "" : "(" + offsetName + ")"));
		placeholders.registerIndexedContextual("remaining_value", StageDescriptionPlaceholdersContext.class,
				context -> {
					if (context.getQuester() instanceof PlayerQuester quester && quester.isActive())
						return Integer.toString(limit - getPlayerTarget(quester.getPlayer().get(), quester));
					return "error: not a player";
				});
		placeholders.registerIndexed("statistic_name", statistic.name());
		placeholders.registerIndexed("type_name", offsetName);
		placeholders.register("target_value", limit);
		ProgressPlaceholders.registerProgress(placeholders, "statistic", this);
	}

	@Override
	public long getTotalAmount() {
		return limit;
	}

	@Override
	public long getRemainingAmount(@NotNull Quester quester) {
		if (!(quester instanceof PlayerQuester playerQuester))
			throw new IllegalArgumentException("Not a player");
		return limit - getPlayerTarget(playerQuester.getPlayer().get(), quester);
	}

	private String getOffsetName() {
		return offsetMaterial != null ? offsetMaterial.name() : (offsetEntity != null ? offsetEntity.name() : null);
	}

	private int getPlayerTarget(Player player, Quester quester) {
		int stat = getStatistic(player);

		if (relative) {
			Integer initial = getData(quester, "initial", Integer.class);
			if (initial != null) stat -= initial.intValue();
		}

		return stat;
	}

	private int getStatistic(Player player) {
		int stat;
		if (offsetMaterial != null) {
			stat = player.getStatistic(statistic, offsetMaterial);
		}else if (offsetEntity != null) {
			stat = player.getStatistic(statistic, offsetEntity);
		}else {
			stat = player.getStatistic(statistic);
		}
		return stat;
	}

	protected void refresh() {
		players.forEach(player -> {
			if (!matchesRequirements(player)) return;

			for (Quester quester : controller.getApplicableQuesters(player)) {
				int playerTarget = getPlayerTarget(player, quester);
				if (lastValues.getOrDefault(quester, Integer.MIN_VALUE) == playerTarget)
					continue;

				if (comparison.test(playerTarget - limit)) {
					lastValues.remove(quester);
					controller.finishStage(quester);
				} else {
					lastValues.put(quester, playerTarget);
					controller.notifyQuesterUpdate(quester);
				}
			}
		});
	}

	@Override
	public void load() {
		super.load();
		players = new ArrayList<>();
		task = Bukkit.getScheduler().runTaskTimerAsynchronously(BeautyQuestsExpansion.getInstance(), this::refresh, 20, 20);
	}

	@Override
	public void unload() {
		super.unload();
		players = null;
		if (task != null) task.cancel();
	}

	@Override
	public void initPlayerDatas(Quester quester, Map<String, Object> datas) {
		super.initPlayerDatas(quester, datas);
		if (relative) {
			int stat = 0;
			if (quester instanceof PlayerQuester playerQuester) {
				if (playerQuester.isActive()) {
					stat = getStatistic(playerQuester.getPlayer().get());
				} else {
					BeautyQuestsExpansion.logger.warning(
							"Trying to fetch initial statistic value for quester {0} that is offline (stage {1}).",
							quester.getDetailedName());
				}
			} else {
				BeautyQuestsExpansion.logger.warning(
						"Trying to fetch initial statistic value for quester {0} that is not an actual player (stage {1}).",
						quester.getDetailedName(), controller);
			}

			datas.put("initial", stat);
		}
	}

	@Override
	public void joined(@NotNull Player player, @NotNull Quester quester) {
		super.joined(player, quester);
		players.add(player);
	}

	@Override
	public void left(@NotNull Player player, @NotNull Quester quester) {
		super.left(player, quester);
		players.remove(player);
	}

	@Override
	public void started(@NotNull Quester quester) {
		super.started(quester);
		players.addAll(quester.getOnlinePlayers());
	}

	@Override
	public void ended(@NotNull Quester quester) {
		super.ended(quester);
		players.removeAll(quester.getOnlinePlayers());
	}

	@Override
	protected void serialize(ConfigurationSection section) {
		section.set("statistic", statistic.name());
		if (offsetMaterial != null) {
			section.set("material", offsetMaterial.name());
		}else if (offsetEntity != null) {
			section.set("entity", offsetEntity.name());
		}
		section.set("limit", limit);
		if (relative) section.set("relative", true);
		if (comparison != ComparisonMethod.GREATER_OR_EQUAL) section.set("comparison", comparison.name());
	}

	public static StageStatistic deserialize(ConfigurationSection section, StageController controller) {
		Statistic statistic = Statistic.valueOf(section.getString("statistic"));
		int limit = section.getInt("limit");
		boolean relative = section.getBoolean("relative", false);
		ComparisonMethod comparison = section.contains("comparison") ? ComparisonMethod.valueOf(section.getString("comparison")) : ComparisonMethod.GREATER_OR_EQUAL;

		if (section.contains("material")) {
			return new StageStatistic(controller, statistic, Material.valueOf(section.getString("material")), limit,
					comparison, relative);
		}else if (section.contains("entity")) {
			return new StageStatistic(controller, statistic, EntityType.valueOf(section.getString("entity")), limit,
					comparison, relative);
		}else {
			return new StageStatistic(controller, statistic, limit, comparison, relative);
		}
	}

	public static class Creator extends StageCreation<StageStatistic> {

		private static Map<Statistic, ItemStack> STATISTIC_ITEMS =
				Stream.of(Statistic.values()).collect(Collectors.toMap(stat -> stat, Creator::getStatisticItem));

		private static ItemStack getStatisticItem(Statistic object) {
			XMaterial material;
			String lore = null;
			switch (object.getType()) {
			case BLOCK:
				material = XMaterial.GRASS_BLOCK;
				lore = LangExpansion.Stage_Statistic_StatList_Gui_Block.toString();
				break;
			case ENTITY:
				material = XMaterial.BLAZE_SPAWN_EGG;
				lore = LangExpansion.Stage_Statistic_StatList_Gui_Entity.toString();
				break;
			case ITEM:
				material = XMaterial.STONE_HOE;
				lore = LangExpansion.Stage_Statistic_StatList_Gui_Item.toString();
				break;
			default:
				material = XMaterial.FEATHER;
				break;
			}
			return ItemUtils.item(material, "§e" + object.name(), QuestOption.formatDescription(lore));
		}

		private static final int SLOT_STAT = 5;
		private static final int SLOT_LIMIT = 6;
		private static final int SLOT_RELATIVE = 7;

		private Statistic statistic;
		private Material offsetMaterial;
		private EntityType offsetEntity;

		private int limit;
		private ComparisonMethod comparison = ComparisonMethod.GREATER_OR_EQUAL;
		private boolean relative = false;

		public Creator(@NotNull StageCreationContext<StageStatistic> context) {
			super(context);

			getLine().setItem(SLOT_STAT,
					ItemUtils.item(XMaterial.FEATHER, LangExpansion.Stage_Statistic_Item_Stat.toString()), event -> {
						openStatisticGUI(event.getPlayer(), event::reopen, false);
			});
			getLine().setItem(SLOT_LIMIT,
					ItemUtils.item(XMaterial.REDSTONE, LangExpansion.Stage_Statistic_Item_Limit.toString()), event -> {
						openLimitEditor(event.getPlayer(), event::reopen, event::reopen);
			});
			getLine().setItem(SLOT_RELATIVE,
					ItemUtils.itemSwitch(LangExpansion.Stage_Statistic_Item_Relative.toString(), relative,
							QuestOption
									.formatDescription(LangExpansion.Stage_Statistic_Item_Relative_Description.toString())),
					event -> {
						relative = ItemUtils.toggleSwitch(event.getClicked());
					});
		}

		public void setStatistic(Statistic statistic) {
			this.statistic = statistic;

			String name;
			if (offsetMaterial != null) {
				name = offsetMaterial.name();
			}else if (offsetEntity != null) {
				name = offsetEntity.name();
			}else {
				name = null;
			}
			getLine().refreshItemLoreOptionValue(SLOT_STAT, statistic.name() + (name == null ? "" : " (" + name + ")"));
		}

		public void setLimit(int limit) {
			this.limit = limit;
			getLine().refreshItemLoreOptionValue(SLOT_LIMIT, limit);
		}

		public void setRelative(boolean relative) {
			this.relative = relative;
			getLine().refreshItem(SLOT_RELATIVE, item -> ItemUtils.setSwitch(item, relative));
		}

		@Override
		public void start(Player p) {
			super.start(p);
			openStatisticGUI(p, context::removeAndReopenGui, true);
		}

		private void openStatisticGUI(Player p, Runnable cancel, boolean askLimit) {
			new StaticPagedGUI<>(LangExpansion.Stage_Statistic_StatList_Gui_Name.toString(), DyeColor.LIGHT_BLUE,
					STATISTIC_ITEMS, stat -> {
						if (stat == null) {
							cancel.run();
						} else {
							switch (stat.getType()) {
								case BLOCK:
								case ITEM:
									boolean isItem = stat.getType() == Type.ITEM;
									new TextEditor<>(p, cancel, offset -> {
										Runnable end = () -> {
											offsetMaterial = offset.parseMaterial();
											setStatistic(stat);
											context.reopenGui();
										};
										if (askLimit) {
											openLimitEditor(p, cancel, end);
										} else
											end.run();

									}, QuestsPlugin.getPlugin().getEditorManager().getFactory().getMaterialParser(isItem,
											!isItem))
													.start();
									break;
								case ENTITY:
									QuestsPlugin.getPlugin().getGuiManager().getFactory()
											.createEntityTypeSelection(offset -> {
												Runnable end = () -> {
													offsetEntity = offset;
													setStatistic(stat);
													context.reopenGui();
												};
												if (askLimit) {
													openLimitEditor(p, cancel, end);
												} else
													end.run();
											}, null).open(p);
									break;
								default:
									Runnable end = () -> {
										setStatistic(stat);
										context.reopenGui();
									};
									if (askLimit) {
										openLimitEditor(p, cancel, end);
									} else
										end.run();
									break;
							}
						}
					}).addSearchButton(Statistic::name, true).open(p);
		}

		private void openLimitEditor(Player p, Runnable cancel, Runnable end) {
			LangExpansion.Stage_Statistic_EDITOR_LIMIT.send(p);
			new TextEditor<>(p, cancel, newLimit -> {
				// add comparison editor

				setLimit(newLimit);
				end.run();
			}, NumberParser.INTEGER_PARSER_POSITIVE).start();
		}

		@Override
		public void edit(StageStatistic stage) {
			super.edit(stage);
			if (stage.offsetEntity != null) {
				this.offsetEntity = stage.offsetEntity;
			}else if (stage.offsetMaterial != null) {
				this.offsetMaterial = stage.offsetMaterial;
			}
			setStatistic(stage.statistic);
			setLimit(stage.limit);
			setRelative(stage.relative);
		}

		@Override
		protected StageStatistic finishStage(StageController controller) {
			if (offsetMaterial != null) {
				return new StageStatistic(controller, statistic, offsetMaterial, limit, comparison, relative);
			}else if (offsetEntity != null) {
				return new StageStatistic(controller, statistic, offsetEntity, limit, comparison, relative);
			}else {
				return new StageStatistic(controller, statistic, limit, comparison, relative);
			}
		}

	}

}
