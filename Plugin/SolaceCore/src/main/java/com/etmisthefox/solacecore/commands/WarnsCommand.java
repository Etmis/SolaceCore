package com.etmisthefox.solacecore.commands;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import com.etmisthefox.solacecore.utils.PaginationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WarnsCommand implements CommandExecutor {

    private static final int PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("mm:HH dd-MM-yyyy");

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
            return true;
        }

        // Filter only warnings
        List<Punishment> warnings = punishments.stream()
                .filter(p -> p.getPunishmentType() != null && p.getPunishmentType().equalsIgnoreCase("warn"))
                .collect(Collectors.toList());

        if (warnings.isEmpty()) {
            sender.sendMessage(Component.text("No warnings found for " + targetName + ".", NamedTextColor.GRAY));
            return true;
        }

        // Sort by start desc (nulls last), then id desc as tie-breaker
        warnings.sort(Comparator
                .comparing(Punishment::getStart, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Punishment::getId)
                .reversed());

        // Compute pagination via util
        PaginationUtil.PageInfo info = PaginationUtil.paginate(page, warnings.size(), PAGE_SIZE);
        List<Punishment> pageItems = PaginationUtil.pageItems(warnings, info);

        // Header using util
        Component header = PaginationUtil.buildHeader("Warnings for ", targetName, info);
        sender.sendMessage(header);

        // Lines
        for (Punishment p : pageItems) {
            String reason = p.getReason() != null ? p.getReason() : "-";
            String operator = p.getOperator() != null ? p.getOperator() : "-";
            LocalDateTime start = p.getStart();
            String startStr = start != null ? DATE_FMT.format(start) : "-";

            Component line = Component.text(reason, NamedTextColor.WHITE)
                    .append(Component.text(" | by ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(operator, NamedTextColor.YELLOW))
                    .append(Component.text(" | at ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(startStr, NamedTextColor.AQUA));

            sender.sendMessage(line);
        }

        // Footer navigation via util
        Component footer = PaginationUtil.buildFooter(info, p -> "/" + label + " " + targetName + " " + p);
        if (!footer.equals(Component.empty())) {
            sender.sendMessage(footer);
        }

        return true;
    }
}
