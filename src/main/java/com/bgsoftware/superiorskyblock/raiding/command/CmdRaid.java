package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitation;
import com.bgsoftware.superiorskyblock.raiding.RaidInvitationHandler;
import com.bgsoftware.superiorskyblock.raiding.SuperiorRaid;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.*;

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

        if (!sender.isOp() && !SuperiorSkyblockPlugin.isRaidingEnabled()) {
            sender.sendMessage(ChatColor.RED + "This feature is currently disabled.");
            return;
        }

        if (sender.isOp() && args[1].equalsIgnoreCase("toggle")) {
            SuperiorSkyblockPlugin.setRaidingEnabled(!SuperiorSkyblockPlugin.isRaidingEnabled());
            sender.sendMessage(SuperiorSkyblockPlugin.isRaidingEnabled() ? ChatColor.GREEN + "Enabled island raiding." : ChatColor.RED + "Disabled island raiding.");
            return;
        }

        String arg = args[1];
        Player commandSender = (Player) sender;
        Player invitee = Bukkit.getPlayer(arg);
        if (invitee == null) {
            if (args.length == 3) {
                Player invitationSender = Bukkit.getPlayer(args[2]);
                if (arg.equalsIgnoreCase("accept")) {
                    commandSender.sendMessage(ChatColor.GREEN + "Invitation accepted.");
                    invitationSender.sendMessage(ChatColor.GREEN + commandSender.getName() + " has accepted your invitation.");
                    acceptInvitation(invitationSender, commandSender, plugin);
                } else if (arg.equalsIgnoreCase("deny")) declineInvitation(invitationSender, commandSender);
                else sender.sendMessage(Locale.INVALID_PLAYER.getMessage(LocaleUtils.getLocale(sender), arg));
            } else {
                commandSender.sendMessage(Locale.INVALID_ARGUMENT_COUNT.getMessage(LocaleUtils.getLocale(commandSender)));
            }
        } else {
            if (commandSender.getUniqueId().equals(invitee.getUniqueId())) {
                commandSender.sendMessage(Locale.INVALID_INVITATION_TARGET.getMessage(LocaleUtils.getLocale(commandSender)));
                return;
            }
            // Ensure both players have an island
            Island senderIsland = plugin.getGrid().getIsland(commandSender.getUniqueId());
            Island inviteeIsland = plugin.getGrid().getIsland(invitee.getUniqueId());
            if (senderIsland == null) {
                commandSender.sendMessage(Locale.PLAYER_NOT_ISLAND_OWNER.getMessage(LocaleUtils.getLocale(commandSender), commandSender.getName()));
                return;
            }
            if (inviteeIsland == null) {
                commandSender.sendMessage(Locale.PLAYER_NOT_ISLAND_OWNER.getMessage(LocaleUtils.getLocale(invitee), invitee.getName()));
                return;
            }
            RaidInvitation invitation = new RaidInvitation(commandSender.getUniqueId(), invitee.getUniqueId());
            if (RaidInvitationHandler.addInvitation(invitation))
                sender.sendMessage(Locale.RAID_INVITATION_SENT.getMessage(LocaleUtils.getLocale(sender)));
            else
                sender.sendMessage(Locale.PLAYER_ALREADY_INVITED_TO_RAID.getMessage(LocaleUtils.getLocale(commandSender)));

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

        Map<SuperiorPlayer, ItemStack[]> teamOneMembers = new HashMap<>(), teamTwoMembers = new HashMap<>();

        for (SuperiorPlayer superiorPlayer : teamOneIsland.getIslandMembers(true)){
            if (!superiorPlayer.isOnline()) continue;

            teamOneMembers.put(superiorPlayer, superiorPlayer.asPlayer().getInventory().getContents());
        }

        for (SuperiorPlayer superiorPlayer : teamTwoIsland.getIslandMembers(true)) {
            if (!superiorPlayer.isOnline()) continue;

            teamTwoMembers.put(superiorPlayer, superiorPlayer.asPlayer().getInventory().getContents());
        }

        Pair<Location, Location> raidIslandLocations = plugin.getRaidIslandManager().setupIslands(teamOneIsland, teamTwoIsland);
        final World raidWorld = raidIslandLocations.getKey().getWorld();
        raidIslandLocations.getValue().setYaw(raidIslandLocations.getValue().getYaw());

        Location teleportLocation = teamOneIsland.getTeleportLocation(World.Environment.NORMAL);
        Location islandCenter = teamOneIsland.getCenter(World.Environment.NORMAL);
        double xDiff = teleportLocation.getX() - islandCenter.getX();
        double yDiff = teleportLocation.getY() - islandCenter.getY();
        double zDiff = teleportLocation.getZ() - islandCenter.getZ();
        double[] diffs1 = new double[]{xDiff, yDiff, zDiff};
        teamOneMembers.keySet().forEach(member -> {
            Location adjustedTeleportLocation = raidIslandLocations.getKey().add(diffs1[0], diffs1[1], diffs1[2]);
            double y = raidWorld.getHighestBlockYAt(adjustedTeleportLocation.getBlockX(), adjustedTeleportLocation.getBlockZ());
            member.teleport(new Location(raidWorld, adjustedTeleportLocation.getX(), y, adjustedTeleportLocation.getZ()).add(0.5, 0, 0.5));
        });

        teleportLocation = teamTwoIsland.getTeleportLocation(World.Environment.NORMAL);
        islandCenter = teamTwoIsland.getCenter(World.Environment.NORMAL);
        xDiff = teleportLocation.getX() - islandCenter.getX();
        yDiff = teleportLocation.getY() - islandCenter.getY();
        zDiff = teleportLocation.getZ() - islandCenter.getZ();
        double[] diffs2 = new double[]{xDiff, yDiff, zDiff};
        teamTwoMembers.keySet().forEach(member -> {
            Location adjustedTeleportLocation = raidIslandLocations.getValue().subtract(diffs2[0], diffs2[1], diffs2[2]);
            double y = raidWorld.getHighestBlockYAt(adjustedTeleportLocation.getBlockX(), adjustedTeleportLocation.getBlockZ());
            member.teleport(new Location(raidWorld, adjustedTeleportLocation.getX(), y, adjustedTeleportLocation.getZ()).add(0.5, 0, 0.5));
        });

        SuperiorRaid superiorRaid = new SuperiorRaid();
        superiorRaid.setTeamOnePlayers(teamOneMembers);
        superiorRaid.setTeamTwoPlayers(teamTwoMembers);
        superiorRaid.setTeamOneLocation(raidIslandLocations.getKey().clone());
        superiorRaid.setTeamTwoLocation(raidIslandLocations.getValue().clone());
        superiorRaid.setTeamOneMinLocation(raidIslandLocations.getKey().clone().add(-teamOneIsland.getIslandSize(), -teamOneIsland.getIslandSize(), -teamOneIsland.getIslandSize()));
        superiorRaid.setTeamOneMaxLocation(raidIslandLocations.getKey().clone().add(teamOneIsland.getIslandSize(), teamOneIsland.getIslandSize(), teamOneIsland.getIslandSize()));
        superiorRaid.setTeamTwoMinLocation(raidIslandLocations.getValue().clone().add(-teamTwoIsland.getIslandSize(), -teamTwoIsland.getIslandSize(), -teamTwoIsland.getIslandSize()));
        superiorRaid.setTeamTwoMaxLocation(raidIslandLocations.getValue().clone().add(teamTwoIsland.getIslandSize(), teamTwoIsland.getIslandSize(), teamTwoIsland.getIslandSize()));

        plugin.getRaidsHandler().startRaid(superiorRaid);
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
        optionAccept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Accept raid invitation").create()));
        optionAccept.setColor(ChatColor.GREEN);

        TextComponent orText = new TextComponent(" or ");
        orText.setColor(ChatColor.WHITE);

        TextComponent optionDecline = new TextComponent("Decline");
        optionDecline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/island raid deny " + senderName));
        optionDecline.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Decline raid invitation").create()));
        optionDecline.setColor(ChatColor.RED);

        TextComponent period = new TextComponent(".");
        period.setColor(ChatColor.WHITE);

        return new TextComponent[]{senderNameComponent, mainComponent, optionAccept, orText, optionDecline, period};
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

