package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public final class WarnsCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Database database;
    private final LanguageManager lang;

    public WarnsCommand(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(lang.getMessage("usage.warns"));
            return true;
        }

        String targetName = args[0];
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        if (page < 1) page = 1;

        List<Punishment> punishments;
        try {
            punishments = database.getPunishmentsByName(targetName);
        } catch (SQLException e) {
            e.printStackTrace();
            sender.sendMessage("Database error while fetching punishments.");
            return true;
        }

        if (punishments.isEmpty()) {
            sender.sendMessage(Component.text("No punishments found for " + targetName + ".", NamedTextColor.GRAY));
            return true;
        }

        // Sort by start desc (nulls last), then id desc as tie-breaker
        punishments.sort(Comparator
                .comparing(Punishment::getStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Punishment::getId)
                .reversed());

        int total = punishments.size();
        int totalPages = (int) Math.ceil(total / (double) PAGE_SIZE);
        if (page > totalPages) page = totalPages;

        int fromIndex = (page - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, total);
        List<Punishment> pageItems = punishments.subList(fromIndex, toIndex);

        // Header
        Component header = Component.text("Punishments for ", NamedTextColor.GOLD)
                .append(Component.text(targetName, NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" (", NamedTextColor.GOLD))
                .append(Component.text("Page " + page + "/" + totalPages, NamedTextColor.AQUA))
                .append(Component.text(")", NamedTextColor.GOLD));
        sender.sendMessage(header);

        // Lines
        for (Punishment p : pageItems) {
            String type = p.getPunishmentType() != null ? p.getPunishmentType().toUpperCase() : "UNKNOWN";
            String reason = p.getReason() != null ? p.getReason() : "-";
            String operator = p.getOperator() != null ? p.getOperator() : "-";
            LocalDateTime start = p.getStart();
            String startStr = start != null ? DATE_FMT.format(start) : "-";
            boolean active = p.getIsActive();

            Component line = Component.text("#" + p.getId() + " ", NamedTextColor.DARK_GRAY)
                    .append(Component.text("[" + type + "] ", NamedTextColor.GOLD))
                    .append(Component.text(reason, NamedTextColor.WHITE))
                    .append(Component.text(" | by ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(operator, NamedTextColor.YELLOW))
                    .append(Component.text(" | at ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(startStr, NamedTextColor.AQUA))
                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(active ? "ACTIVE" : "INACTIVE", active ? NamedTextColor.GREEN : NamedTextColor.GRAY));

            sender.sendMessage(line);
        }

        // Footer navigation with clickable buttons
        Component footer = Component.empty();
        if (page > 1) {
            Component prev = Component.text("<<", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Previous page", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/" + label + " " + targetName + " " + (page - 1)));
            footer = footer.append(prev);
        }
        if (page > 1 && page < totalPages) {
            footer = footer.append(Component.text(" ", NamedTextColor.DARK_GRAY));
        }
        if (page < totalPages) {
            Component next = Component.text(">>", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .hoverEvent(HoverEvent.showText(Component.text("Next page", NamedTextColor.GRAY)))
                    .clickEvent(ClickEvent.runCommand("/" + label + " " + targetName + " " + (page + 1)));
            footer = footer.append(next);
        }

        if (!footer.equals(Component.empty())) {
            sender.sendMessage(footer);
        }

        return true;
    }
}
