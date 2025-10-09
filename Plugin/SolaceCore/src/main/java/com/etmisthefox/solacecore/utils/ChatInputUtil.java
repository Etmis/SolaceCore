package com.etmisthefox.solacecore.utils;

import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility pro zachycení jednorázového textového vstupu hráče přes chat.
 * Použití:
 * ChatInputUtil.requestInput(player, "Zadej důvod nebo napiš cancel:").thenAccept(result -> { ... });
 * Pokud hráč napíše "cancel" (case-insensitive), vrací se null.
 */
public final class ChatInputUtil {

    private static final String CANCEL_KEYWORD = "cancel"; // lze případně lokalizovat

    private static final Map<UUID, PendingInput> PENDING = new ConcurrentHashMap<>();

    private ChatInputUtil() {}

    public static boolean isWaiting(UUID uuid) {
        return PENDING.containsKey(uuid);
    }

    /**
     * Vyžádá si vstup. Vrácený future se kompletuje:
     * - text co hráč napsal (string)
     * - null pokud hráč zadal cancel nebo byl odhlášen / zrušeno
     */
    public static CompletableFuture<String> requestInput(Player player, String prompt) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PendingInput old = PENDING.put(player.getUniqueId(), new PendingInput(future));
        if (old != null) {
            // Předchozí nedokončený input zrušíme
            old.future.complete(null);
        }
        player.sendMessage(prompt + " (napiš '" + CANCEL_KEYWORD + "' pro zrušení)");
        return future;
    }

    /**
     * Zpracuje chat event pokud hráč čeká na vstup. True = spotřebováno (zrušit broadcast).
     */
    public static boolean handleChatInput(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = PENDING.get(player.getUniqueId());
        if (pending == null) return false;

        String msg = event.getMessage();
        event.setCancelled(true); // Nechceme broadcastovat do veřejného chatu
        if (msg.equalsIgnoreCase(CANCEL_KEYWORD)) {
            player.sendMessage("Vstup zrušen.");
            pending.future.complete(null);
        } else {
            pending.future.complete(msg);
        }
        PENDING.remove(player.getUniqueId());
        return true;
    }

    /**
     * Zruší čekání (např. při odhlášení hráče / timeoutu) – vrací true pokud něco bylo zrušeno.
     */
    public static boolean cancel(UUID uuid) {
        PendingInput pending = PENDING.remove(uuid);
        if (pending != null) {
            pending.future.complete(null);
            return true;
        }
        return false;
    }

    /**
     * Lze volat z main threadu pro force cancel všech (např. onDisable).
     */
    public static void cancelAll() {
        for (Map.Entry<UUID, PendingInput> e : PENDING.entrySet()) {
            e.getValue().future.complete(null);
        }
        PENDING.clear();
    }

    private record PendingInput(CompletableFuture<String> future) { }
}
