package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitation;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitationHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CmdRaid implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("raid");
    }

    @Override
    public String getPermission() {
        return "superior.island.raid";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "raid <" + Locale.COMMAND_ARGUMENT_ISLAND_NAME.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_RAID.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        String arg = args[1];
        Player commandSender = (Player) sender;
        Player invitee = Bukkit.getPlayer(arg);
        if (invitee == null) {
            if (args.length == 3) {
                Player invitationSender = Bukkit.getPlayer(args[2]);
                if (arg.equalsIgnoreCase("accept")) acceptInvitation(invitationSender, commandSender, plugin);
                else if (arg.equalsIgnoreCase("deny")) declineInvitation(invitationSender, commandSender);
                else sender.sendMessage("Couldn't find player with name " + arg + ".");
            } else commandSender.sendMessage("Invalid number of arguments.");
        } else {
            RaidInvitation invitation = new RaidInvitation(commandSender.getUniqueId(), invitee.getUniqueId());
            if (RaidInvitationHandler.addInvitation(invitation)) sender.sendMessage("Invitation sent.");
            else sender.sendMessage("You've already sent an invitation to this player.");

            TextComponent[] messageComponents = createInvitationMessageComponents(sender.getName());
            invitee.spigot().sendMessage(messageComponents);
        }
    }

    private void acceptInvitation(Player invitationSender, Player commandSender, SuperiorSkyblockPlugin plugin) {
        RaidInvitation invitation = handleInvitation(invitationSender.getUniqueId(), commandSender.getUniqueId());

        if (invitation == null) {
            commandSender.sendMessage("You have no pending invitations from this player.");
            return;
        }

        Island teamOneIsland = plugin.getGrid().getIsland(invitation.getSenderUuid());
        Island teamTwoIsland = plugin.getGrid().getIsland(invitation.getInviteeUuid());

        plugin.getRaidsHandler().startRaid(teamOneIsland.getIslandMembers(true), teamTwoIsland.getIslandMembers(true));
    }

    private void declineInvitation(Player invitationSender, Player commandSender) {
        if (RaidInvitationHandler.removeInvitation(invitationSender.getUniqueId(), commandSender.getUniqueId())) {
            commandSender.sendMessage("Invitation declined.");
            invitationSender.sendMessage(commandSender.getName() + " has declined your invitation.");
        } else {
            commandSender.sendMessage("You have no pending invitations from this player.");
        }
    }

    private RaidInvitation handleInvitation(UUID sender, UUID invitee) {
        RaidInvitation invitation = RaidInvitationHandler.getInvitation(sender, invitee);
        if (invitation == null) return null;
        RaidInvitationHandler.removeInvitationsOfSender(sender);
        RaidInvitationHandler.removeInvitationsOfInvitee(invitee);
        return invitation;
    }

    private TextComponent[] createInvitationMessageComponents(String senderName) {
        TextComponent mainComponent = new TextComponent(senderName + " has invited you to a raid. ");
        mainComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("[Insert island level and members online here]").create()));

        TextComponent optionAccept = new TextComponent("Accept");
        optionAccept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island raid accept " + senderName));
        optionAccept.setColor(ChatColor.GREEN);

        TextComponent orText = new TextComponent(" or ");
        orText.setColor(ChatColor.WHITE);

        TextComponent optionDecline = new TextComponent("Decline");
        optionDecline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island raid deny " + senderName));
        optionDecline.setColor(ChatColor.RED);

        TextComponent period = new TextComponent(".");
        period.setColor(ChatColor.WHITE);

        return new TextComponent[]{mainComponent, optionAccept, orText, optionDecline};
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

