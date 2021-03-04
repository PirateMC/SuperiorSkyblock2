package com.bgsoftware.superiorskyblock.commands.raiding;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.commands.ISuperiorCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        return 2;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        String inviteeName = args[1];
        Player invitee = Bukkit.getPlayer(inviteeName);

        if (invitee == null) {
            sender.sendMessage("Couldn't find player with name " + inviteeName + ".");
            return;
        }

        RaidInvitation invitation = new RaidInvitation(((Player) sender).getUniqueId(), invitee.getUniqueId());
        RaidInvitationHandler.addInvitation(invitation);
        sender.sendMessage("Invitation sent.");

        TextComponent[] messageComponents = createInvitationMessageComponents(sender.getName(), inviteeName);
        invitee.spigot().sendMessage(messageComponents);
    }

    private TextComponent[] createInvitationMessageComponents(String senderName, String inviteeName) {
        TextComponent mainComponent = new TextComponent(senderName + " has invited you to a raid.");
        mainComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("This text is shown on hover!").create()));

        TextComponent optionAccept = new TextComponent("Accept");
        optionAccept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "island startraid " + senderName + " " + inviteeName));
        optionAccept.setColor(ChatColor.GREEN);

        TextComponent optionDecline = new TextComponent("Decline");
        optionDecline.setColor(ChatColor.RED);

        return new TextComponent[]{mainComponent, optionAccept, optionDecline};
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

class RaidInvitation {
    private UUID senderUuid;
    private UUID inviteeUuid;
    private long timeLeft = 60;

    public RaidInvitation(UUID senderUuid, UUID inviteeUuid) {
        this.senderUuid = senderUuid;
        this.inviteeUuid = inviteeUuid;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public UUID getInviteeUuid() {
        return inviteeUuid;
    }

    public long getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(long timeLeft) {
        this.timeLeft = timeLeft;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RaidInvitation)) return false;
        return ((RaidInvitation) obj).senderUuid.equals(senderUuid)
                && ((RaidInvitation) obj).inviteeUuid.equals(inviteeUuid);
    }
}

class RaidInvitationHandler {
    private static Set<RaidInvitation> raidInvitations = new HashSet<>();

    public static boolean addInvitation(RaidInvitation invitation) {
        return raidInvitations.add(invitation);
    }
}