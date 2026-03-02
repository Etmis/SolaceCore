package com.etmisthefox.solacecore.utils;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputUtil {

    private static final String CANCEL_KEYWORD = "cancel"; // lze případně lokalizovat

    private static final Map<UUID, PendingInput> PENDING = new ConcurrentHashMap<>();

    public static CompletableFuture<String> requestInput(Player player, String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PendingInput old = PENDING.put(player.getUniqueId(), new PendingInput(future));
        if (old != null) {
            // Předchozí nedokončený input zrušíme
            old.future.complete(null);
        }
        // Volající musí dodat celý prompt (včetně případného textu o cancel)
        player.sendMessage(prompt);
        return future;
    }

    public static boolean handleChatInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = PENDING.get(player.getUniqueId());
        if (pending == null) return false;

        // Převeď zprávu z Adventure Component na obyčejný text a ořízni whitespace
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        event.setCancelled(true); // Nechceme broadcastovat do veřejného chatu
        if (msg.equalsIgnoreCase(CANCEL_KEYWORD)) {
            // Zrušení bez posílání zprávy – to řeší volající podle vlastní lokalizace
            pending.future.complete(null);
        } else {
            pending.future.complete(msg);
        }
        PENDING.remove(player.getUniqueId());
        return true;
    }

    public static void cancelAll() {
        for (Map.Entry<UUID, PendingInput> e : PENDING.entrySet()) {
            e.getValue().future.complete(null);
        }
        PENDING.clear();
    }

    private record PendingInput(CompletableFuture<String> future) { }
}
