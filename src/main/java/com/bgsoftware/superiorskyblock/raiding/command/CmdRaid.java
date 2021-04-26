package com.bgsoftware.superiorskyblock.raiding.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import com.bgsoftware.superiorskyblock.raiding.RaidCooldownHandler;
import com.bgsoftware.superiorskyblock.raiding.invites.RaidInvitation;
import com.bgsoftware.superiorskyblock.raiding.invites.RaidInvitationHandler;
import com.bgsoftware.superiorskyblock.raiding.queue.RaidQueue;
import com.bgsoftware.superiorskyblock.raiding.queue.RaidQueueAddResult;
import com.bgsoftware.superiorskyblock.raiding.queue.RaidQueueEntry;
import com.bgsoftware.superiorskyblock.utils.LocaleUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Arrays;
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

        if (!sender.isOp() && !SuperiorSkyblockPlugin.isRaidingEnabled()) {
            sender.sendMessage(ChatColor.RED + "This feature is currently disabled.");
            return;
        }

        //TODO Move toggle option to its own class
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
                //TODO Send more specific message
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

            // Check if sender or invitee islands have a cooldown
            if (RaidCooldownHandler.isCoolingDown(senderIsland)) {
                commandSender.sendMessage(Locale.RAID_COOLDOWN_ACTIVE.getMessage(
                        LocaleUtils.getLocale(commandSender),
                        RaidCooldownHandler.getCooldownOf(senderIsland)
                ));
                return;
            }
            if (RaidCooldownHandler.isCoolingDown(inviteeIsland)) {
                commandSender.sendMessage(Locale.RAID_COOLDOWN_ACTIVE_OTHER.getMessage(LocaleUtils.getLocale(commandSender)));
                return;
            }

            RaidInvitation invitation = new RaidInvitation(commandSender.getUniqueId(), invitee.getUniqueId());
            if (RaidInvitationHandler.addInvitation(invitation))
                sender.sendMessage(Locale.RAID_INVITATION_SENT.getMessage(LocaleUtils.getLocale(sender)));
            else {
                sender.sendMessage(Locale.PLAYER_ALREADY_INVITED_TO_RAID.getMessage(LocaleUtils.getLocale(commandSender)));
                return;
            }

            TextComponent[] messageComponents = createInvitationMessageComponents(sender.getName(), senderIsland);
            invitee.spigot().sendMessage(messageComponents);
        }
    }

    private void acceptInvitation(Player invitationSender, Player commandSender, SuperiorSkyblockPlugin plugin) {
        RaidInvitation invitation = getAndRemoveInvitationOf(invitationSender.getUniqueId(), commandSender.getUniqueId());

        if (invitation == null) {
            commandSender.sendMessage(Locale.NO_PENDING_RAID_INVITATIONS.getMessage(LocaleUtils.getLocale(commandSender)));
            return;
        }

        RaidQueue raidQueue = SuperiorSkyblockPlugin.getPlugin().getRaidQueue();
        RaidQueueEntry raidQueueEntry = new RaidQueueEntry(commandSender.getUniqueId(), invitationSender.getUniqueId());
        if (raidQueue.contains(raidQueueEntry)) {
            sendMessageToMultiple(ChatColor.YELLOW + "You are already in the queue.", commandSender, invitationSender);
            return;
        }
        RaidQueueAddResult result = raidQueue.add(raidQueueEntry);
        if (!result.wasSuccessful()) {
            sendMessageToMultiple(ChatColor.RED + "Unable to add you to the queue at this time.", invitationSender, commandSender);
            return;
        }

        sendMessageToMultiple(ChatColor.GREEN + "You are in position " + result.getSize() + " of the queue.", invitationSender, commandSender);
    }

    private void declineInvitation(Player invitationSender, Player commandSender) {
        if (RaidInvitationHandler.removeInvitation(invitationSender.getUniqueId(), commandSender.getUniqueId())) {
            commandSender.sendMessage(Locale.RAID_INVITATION_DECLINED.getMessage(LocaleUtils.getLocale(commandSender)));
            invitationSender.sendMessage(Locale.RAID_INVITATION_DECLINED_OTHER.getMessage(LocaleUtils.getLocale(invitationSender), commandSender.getName()));
        } else {
            commandSender.sendMessage(Locale.NO_PENDING_RAID_INVITATIONS.getMessage(LocaleUtils.getLocale(commandSender)));
        }
    }

    private RaidInvitation getAndRemoveInvitationOf(UUID sender, UUID invitee) {
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

    private void sendMessageToMultiple(String message, CommandSender commandSender, CommandSender... senders) {
        commandSender.sendMessage(message);
        Arrays.stream(senders).forEach(sender -> sender.sendMessage(message));
    }
}
