package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitation;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitationHandler;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
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
        return "raid <" + Locale.COMMAND_ARGUMENT_PLAYER_NAME.getMessage(locale) + ">";
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
                else sender.sendMessage(Locale.INVALID_PLAYER.getMessage(LocaleUtils.getLocale(sender), arg));
            } else {
                commandSender.sendMessage(Locale.INVALID_ARGUMENT_COUNT.getMessage(LocaleUtils.getLocale(commandSender)));
            }
        } else {
            if (commandSender.getUniqueId().equals(invitee.getUniqueId())) {
                commandSender.sendMessage(Locale.INVALID_INVITATION_TARGET.getMessage(LocaleUtils.getLocale(commandSender)));
                return;
            }
            RaidInvitation invitation = new RaidInvitation(commandSender.getUniqueId(), invitee.getUniqueId());
            if (RaidInvitationHandler.addInvitation(invitation))
                sender.sendMessage(Locale.RAID_INVITATION_SENT.getMessage(LocaleUtils.getLocale(sender)));
            else
                sender.sendMessage(Locale.PLAYER_ALREADY_INVITED_TO_RAID.getMessage(LocaleUtils.getLocale(commandSender)));

            Island senderIsland = plugin.getGrid().getIsland(commandSender.getUniqueId());
            TextComponent[] messageComponents = createInvitationMessageComponents(sender.getName(), senderIsland);
            invitee.spigot().sendMessage(messageComponents);
        }
    }

    private void acceptInvitation(Player invitationSender, Player commandSender, SuperiorSkyblockPlugin plugin) {
        RaidInvitation invitation = handleInvitation(invitationSender.getUniqueId(), commandSender.getUniqueId());

        if (invitation == null) {
            commandSender.sendMessage(Locale.NO_PENDING_RAID_INVITATIONS.getMessage(LocaleUtils.getLocale(commandSender)));
            return;
        }

        Island teamOneIsland = plugin.getGrid().getIsland(invitation.getSenderUuid());
        Island teamTwoIsland = plugin.getGrid().getIsland(invitation.getInviteeUuid());

        List<SuperiorPlayer> teamOneMembers = teamOneIsland.getIslandMembers(true);
        List<SuperiorPlayer> teamTwoMembers = teamTwoIsland.getIslandMembers(true);

        Pair<Location, Location> raidIslandLocations = plugin.getRaidIslandManager().setupIslands(teamOneIsland, teamTwoIsland);
        teamOneMembers.forEach(member -> member.teleport(raidIslandLocations.getKey()));
        teamTwoMembers.forEach(member -> member.teleport(raidIslandLocations.getValue()));

        plugin.getRaidsHandler().startRaid(teamOneMembers, teamTwoMembers);
    }

    private void declineInvitation(Player invitationSender, Player commandSender) {
        if (RaidInvitationHandler.removeInvitation(invitationSender.getUniqueId(), commandSender.getUniqueId())) {
            commandSender.sendMessage(Locale.RAID_INVITATION_DECLINED.getMessage(LocaleUtils.getLocale(commandSender)));
            invitationSender.sendMessage(Locale.RAID_INVITATION_DECLINED_OTHER.getMessage(LocaleUtils.getLocale(invitationSender), commandSender));
        } else {
            commandSender.sendMessage(Locale.NO_PENDING_RAID_INVITATIONS.getMessage(LocaleUtils.getLocale(commandSender)));
        }
    }

    private RaidInvitation handleInvitation(UUID sender, UUID invitee) {
        RaidInvitation invitation = RaidInvitationHandler.getInvitation(sender, invitee);
        if (invitation == null) return null;
        RaidInvitationHandler.removeInvitationsOfSender(sender);
        RaidInvitationHandler.removeInvitationsOfInvitee(invitee);
        return invitation;
    }

    private TextComponent[] createInvitationMessageComponents(String senderName, Island senderIsland) {
        BigDecimal islandLevel = senderIsland.getIslandLevel();
        int islandSize = senderIsland.getIslandSize();

        TextComponent senderNameComponent = new TextComponent(senderName);
        senderNameComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Island level: " + islandLevel + "\nIsland size: " + islandSize).create()));
        senderNameComponent.setColor(ChatColor.YELLOW);

        TextComponent mainComponent = new TextComponent(" has invited you to a raid. ");

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

        return new TextComponent[]{senderNameComponent, mainComponent, optionAccept, orText, optionDecline};
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

