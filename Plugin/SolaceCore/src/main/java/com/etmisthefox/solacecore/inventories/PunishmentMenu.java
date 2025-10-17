package com.etmisthefox.solacecore.inventories;

import com.etmisthefox.inv.ClickableItem;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.inv.SmartInventory;
import com.etmisthefox.inv.content.InventoryContents;
import com.etmisthefox.inv.content.InventoryProvider;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.utils.ChatInputUtil;
import com.etmisthefox.solacecore.utils.PunishmentUtil;
import com.etmisthefox.solacecore.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record PunishmentMenu(Database database, LanguageManager lang, InventoryManager inventoryManager, Player target, PunishmentType punishmentType) implements InventoryProvider {

    // Mapa pro uložení důvodu per hráč, který zadává trest (operátor), nikoli per cíl.
    private static final Map<UUID, String> REASONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> DURATIONS = new ConcurrentHashMap<>(); // v sekundách

    private String getReasonOrDefault(Player player) {
        String r = REASONS.get(player.getUniqueId());
        if (r == null || r.isBlank()) {
            return lang.getMessage("punishment.no_reason");
        }
        return r;
    }

    private Long getDuration(Player player) {
        return DURATIONS.get(player.getUniqueId());
    }

    public static SmartInventory getInventory(Database database, LanguageManager lang, InventoryManager inventoryManager, Player target, PunishmentType punishmentType) {
        return SmartInventory.builder()
                .id("punishmentMenu")
                .provider(new PunishmentMenu(database, lang, inventoryManager, target, punishmentType))
                .size(6, 9)
                .title("Punishment Menu")
                .manager(inventoryManager)
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        // Fill Border -> Black Stained Glass Pane
        ItemStack blackStainedGlassPane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta blackStainedGlassPaneMeta = blackStainedGlassPane.getItemMeta();
        blackStainedGlassPaneMeta.displayName(Component.text(" "));
        blackStainedGlassPane.setItemMeta(blackStainedGlassPaneMeta);
        contents.fillBorders(ClickableItem.empty(blackStainedGlassPane));

        // Back -> Arrow
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta arrowMeta = arrow.getItemMeta();
        arrowMeta.displayName(Component.text("Back"));
        arrow.setItemMeta(arrowMeta);
        contents.set(5, 0, ClickableItem.of(arrow, e -> MainMenu.getInventory(database, lang, inventoryManager, target).open(player)));

        // Player's Head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.displayName(Component.text(target.getName()));
        playerHeadMeta.setOwningPlayer(target);
        playerHead.setItemMeta(playerHeadMeta);
        contents.set(1, 4, ClickableItem.empty(playerHead));

        // Time -> Clock (only for TEMP actions)
        if (punishmentType == PunishmentType.TEMPBAN || punishmentType == PunishmentType.TEMPMUTE) {
            ItemStack clock = new ItemStack(Material.CLOCK);
            ItemMeta clockMeta = clock.getItemMeta();
            clockMeta.displayName(Component.text("Punishment Time"));
            Long currentDuration = getDuration(player);
            String formatted = currentDuration != null ? TimeUtil.formatDuration(currentDuration) : "(nenastaveno)";
            List<Component> clockLore = new ArrayList<>();
            clockLore.add(Component.text("Click to set punishment duration"));
            clockLore.add(Component.text("Aktuální: " + formatted));
            clockMeta.lore(clockLore);
            clock.setItemMeta(clockMeta);
            contents.set(2, 6, ClickableItem.of(clock, e -> {
                player.closeInventory();
                ChatInputUtil.requestInput(player, "Zadej délku trestu (např. 10m, 1h, 30s) nebo 'cancel'").thenAccept(input -> {
                    Plugin plugin = Bukkit.getPluginManager().getPlugin("SolaceCore");
                    if (plugin == null) return; // bezpečnost
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (input == null) {
                            player.sendMessage("Operace zrušena.");
                        } else {
                            long seconds = TimeUtil.parseDuration(input);
                            if (seconds <= 0) {
                                player.sendMessage(lang.getMessage("errors.invalid_time"));
                            } else {
                                DURATIONS.put(player.getUniqueId(), seconds);
                                player.sendMessage("Nastavená délka: " + TimeUtil.formatDuration(seconds));
                            }
                        }
                        getInventory(database, lang, inventoryManager, target, punishmentType).open(player);
                    });
                });
            }));
        }

        // Book & Quill -> Reason
        ItemStack bookAndQuill = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta bookAndQuillMeta = bookAndQuill.getItemMeta();
        bookAndQuillMeta.displayName(Component.text("Reason"));
        String currentReason = getReasonOrDefault(player);
        List<Component> reasonLore = new ArrayList<>();
        reasonLore.add(Component.text("Click to set punishment reason"));
        reasonLore.add(Component.text("Aktuální: " + currentReason));
        bookAndQuillMeta.lore(reasonLore);
        bookAndQuill.setItemMeta(bookAndQuillMeta);
        contents.set(2, 2, ClickableItem.of(bookAndQuill, e -> {
            player.closeInventory();
            ChatInputUtil.requestInput(player, "Zadej důvod trestu nebo 'cancel'").thenAccept(input -> {
                Plugin plugin = Bukkit.getPluginManager().getPlugin("SolaceCore");
                if (plugin == null) return;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (input == null) {
                        // cancel -> vyčistíme uložený důvod, následně se použije výchozí no_reason
                        REASONS.remove(player.getUniqueId());
                        player.sendMessage("Operace zrušena. Důvod ponechán: " + lang.getMessage("punishment.no_reason"));
                    } else {
                        String r = input.trim();
                        if (r.isEmpty()) {
                            r = lang.getMessage("punishment.no_reason");
                        }
                        REASONS.put(player.getUniqueId(), r);
                        player.sendMessage("Nastavený důvod: " + r);
                    }
                    getInventory(database, lang, inventoryManager, target, punishmentType).open(player);
                });
            });
        }));

        // Accept -> Green Wool
        ItemStack greenWool = new ItemStack(Material.GREEN_WOOL);
        ItemMeta greenWoolMeta = greenWool.getItemMeta();
        greenWoolMeta.displayName(Component.text("Accept"));
        greenWool.setItemMeta(greenWoolMeta);
        contents.set(5, 8, ClickableItem.of(greenWool, e -> {
            String reason = getReasonOrDefault(player);
            Long duration = getDuration(player);
            switch (punishmentType) {
                case KICK -> {
                    player.closeInventory();
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.KICK, player, target, reason, null);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case BAN -> {
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.BAN, player, target, reason, null);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case TEMPBAN -> {
                    if (duration == null) {
                        player.sendMessage(lang.getMessage("errors.invalid_time"));
                        return;
                    }
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPBAN, player, target, reason, duration);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case IPBAN -> {
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.IPBAN, player, target, reason, null);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case MUTE -> {
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.MUTE, player, target, reason, null);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case TEMPMUTE -> {
                    if (duration == null) {
                        player.sendMessage(lang.getMessage("errors.invalid_time"));
                        return;
                    }
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.TEMPMUTE, player, target, reason, duration);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
                case WARN -> {
                    PunishmentUtil.executePunishment(database, lang, PunishmentType.WARN, player, target, reason, null);
                    REASONS.remove(player.getUniqueId());
                    DURATIONS.remove(player.getUniqueId());
                }
            }
        }));
    }
}
