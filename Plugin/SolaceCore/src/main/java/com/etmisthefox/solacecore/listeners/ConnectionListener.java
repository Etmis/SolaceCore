package com.etmisthefox.solacecore.listeners;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static com.etmisthefox.solacecore.utils.TimeUtil.formatDuration;

public final class ConnectionListener implements Listener {

    private final Database database;
    private final LanguageManager lang;

    public ConnectionListener(Database database, LanguageManager lang) {
        this.database = database;
        this.lang = lang;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onConnect(AsyncPlayerPreLoginEvent event) {
        try {
            database.addPlayer(event.getName(), event.getUniqueId().toString(), event.getAddress().getHostAddress());
            List<Punishment> punishments = database.getPunishmentsByName(event.getName());
            for (Punishment punishment : punishments) {
                String type = punishment.getPunishmentType();
                if ("ban".equals(type)) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            lang.getMessage("login.banned_permanent",
                                    "reason", punishment.getReason(),
                                    "operator", punishment.getOperator())
                    );
                } else if ("tempban".equals(type)) {
                    if (punishment.getStart().plusSeconds(punishment.getDuration()).isBefore(LocalDateTime.now())) {
                        database.unpunishPlayer(event.getName(), "tempban");
                    } else {
                        long remainingSeconds = Duration.between(LocalDateTime.now(), punishment.getStart().plusSeconds(punishment.getDuration())).getSeconds();
                        String remaining = formatDuration(remainingSeconds);
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                                lang.getMessage("login.banned_temp",
                                        "reason", punishment.getReason(),
                                        "operator", punishment.getOperator(),
                                        "remaining", remaining)
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
