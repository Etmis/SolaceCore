package com.etmisthefox.solacecore.listeners;

import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import com.etmisthefox.solacecore.utils.DisconnectScreenUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.sql.SQLException;
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
            List<Punishment> punishments = database.getActivePunishmentsByName(event.getName());
            List<Punishment> ipPunishments = database.getActivePunishmentsByIp(event.getAddress().getHostAddress());
            for (Punishment punishment : punishments) {
                PunishmentType type = PunishmentType.valueOf(punishment.getPunishmentType().toUpperCase());
                switch (type) {
                    case PunishmentType.BAN -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.ban"),
                                    punishment.getReason(),
                                    punishment.getOperator(),
                                    null)
                    );
                    case PunishmentType.TEMPBAN -> event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.tempban"),
                                    punishment.getReason(),
                                    punishment.getOperator(),
                                    formatDuration(punishment.getDuration()))
                    );
                }
            }
            for (Punishment ipPunishment : ipPunishments) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        DisconnectScreenUtil.formatDisconnectScreen(lang.getMessage("player_messages.ipban"),
                                ipPunishment.getReason(),
                                ipPunishment.getOperator(),
                                null));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
