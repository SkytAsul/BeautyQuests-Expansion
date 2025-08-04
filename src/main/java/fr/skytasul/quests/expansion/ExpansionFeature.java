package fr.skytasul.quests.expansion;

import fr.skytasul.quests.expansion.utils.LangExpansion;

public class ExpansionFeature {

	private final String name;
	private final String description;

	private boolean loaded;

	public ExpansionFeature(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public boolean canLoad() {
		return true;
	}

	public void onLoad() {}

	public void onUnload() {}

	public boolean isLoaded() {
		return loaded;
	}

	public final boolean load() {
		if (!canLoad())
			return false;
		try {
			onLoad();
			loaded = true;
		}catch (Throwable ex) {
			BeautyQuestsExpansion.logger.severe("An exception occurred while loading feature " + name, ex);
			loaded = false;
		}
		return loaded;
	}

	public void unload() {
		if (loaded)
			onUnload();
	}

	@Override
	public String toString() {
		String string = (loaded ? "§a" : "§c") + getName() + ":§f " + getDescription();
		if (!loaded) string += " §c(" + LangExpansion.Features_Unloaded.toString() + ")";
		return string;
	}

}
