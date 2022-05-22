package fr.skytasul.quests.expansion.utils;

import fr.skytasul.quests.api.Locale;

public enum LangExpansion implements Locale {
	
	Tracking_Trackers("tracking.trackers"), // 0: tracker amount
	Tracking_Gui_Name("tracking.gui.name"),
	Tracking_Particles_Name("tracking.particles.name"),
	Tracking_Particles_Description("tracking.particles.description"),
	
	;
	
	private final String path;
	
	private String value = "§cnot loaded";
	
	private LangExpansion(String path) {
		this.path = path;
	}
	
	@Override
	public String getPath() {
		return path;
	}
	
	@Override
	public String getValue() {
		return value;
	}
	
	@Override
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString() {
		return getValue();
	}

}
