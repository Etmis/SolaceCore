package com.etmisthefox.solacecore.listeners;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import com.etmisthefox.solacecore.utils.ChatInputUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.etmisthefox.solacecore.utils.TimeUtil.formatDuration;

public final class ChatListener implements Listener {

    private final Database database;
    private final LanguageManager lang;

    public ChatListener(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        // Pokud hráč právě poskytuje vstup přes ChatInputUtil, zpracujeme a ukončíme.
        if (ChatInputUtil.handleChatInput(event)) {
            return;
        }
        try {
            List<Punishment> punishments = database.getActivePunishmentsByName(event.getPlayer().getName());
            for (Punishment punishment : punishments) {
                String type = punishment.getPunishmentType();
                if ("mute".equals(type)) {
                    event.getPlayer().sendMessage(lang.getMessage("chat.blocked_permanent_mute"));
                    event.setCancelled(true);
                } else if ("tempmute".equals(type)) {
                    if (punishment.getStart().plusSeconds(punishment.getDuration()).isBefore(LocalDateTime.now())) {
                        database.unpunishPlayer(event.getPlayer().getName(), "tempmute");
                    } else {
                        long remainingSeconds = Duration.between(LocalDateTime.now(), punishment.getStart().plusSeconds(punishment.getDuration())).getSeconds();
                        String remaining = formatDuration(remainingSeconds);
                        event.getPlayer().sendMessage(
                                lang.getMessage(
                                        "chat.blocked_temp_mute",
                                        "reason", punishment.getReason(),
                                        "operator", punishment.getOperator(),
                                        "remaining", remaining
                                )
                        );
                        event.setCancelled(true);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
