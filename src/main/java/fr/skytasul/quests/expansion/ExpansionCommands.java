package fr.skytasul.quests.expansion;

import fr.skytasul.quests.api.utils.messaging.MessageType.DefaultMessageType;
import fr.skytasul.quests.api.utils.messaging.MessageUtils;
import fr.skytasul.quests.expansion.utils.LangExpansion;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;
import java.util.StringJoiner;

public class ExpansionCommands implements OrphanCommand {

	@Subcommand ("expansion")
	@CommandPermission ("beautyquests.expansion.command.expansion")
	public void expansion(BukkitCommandActor actor) {
		StringJoiner joiner = new StringJoiner("\n");
		joiner.add(LangExpansion.Features_Header.quickFormat("features_amount",
				BeautyQuestsExpansion.getInstance().getFeatures().size()));
		for (ExpansionFeature feature : BeautyQuestsExpansion.getInstance().getFeatures()) {
			joiner.add("- " + feature.toString());
		}
		MessageUtils.sendMessage(actor.audience().get(), joiner.toString(), DefaultMessageType.PREFIXED);
	}

}
