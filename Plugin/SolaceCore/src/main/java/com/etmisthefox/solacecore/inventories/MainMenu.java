package com.etmisthefox.solacecore.inventories;

import com.etmisthefox.inv.ClickableItem;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.inv.SmartInventory;
import com.etmisthefox.inv.content.InventoryContents;
import com.etmisthefox.inv.content.InventoryProvider;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import com.etmisthefox.solacecore.models.Punishment;
import com.etmisthefox.solacecore.utils.TimeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public record MainMenu(Database database, LanguageManager lang, InventoryManager inventoryManager, Player target) implements InventoryProvider {

    public static SmartInventory getInventory(Database database, LanguageManager lang, InventoryManager inventoryManager, Player target) {
        return SmartInventory.builder()
                .id("mainMenu")
                .provider(new MainMenu(database, lang, inventoryManager, target))
                .size(6, 9)
                .title("Main Menu")
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

        // Close -> Barrier
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.displayName(Component.text("Close"));
        barrier.setItemMeta(barrierMeta);
        contents.set(5, 4, ClickableItem.of(barrier, e -> player.closeInventory()));

        // Player's Head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.displayName(Component.text(target.getName()));
        playerHeadMeta.setOwningPlayer(target);
        playerHead.setItemMeta(playerHeadMeta);
        contents.set(1, 4, ClickableItem.empty(playerHead));

        // Build punishments lore for adjacent paper
        List<Punishment> punishments = new ArrayList<>();
        try {
            punishments = database.getPunishmentsByName(target.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Component> punishLore = new ArrayList<>();
        if (punishments.isEmpty()) {
            punishLore.add(Component.text("Žádné tresty."));
        } else {
            punishLore.add(Component.text("Tresty:"));
            int maxLines = 15;
            int count = 0;
            for (Punishment p : punishments) {
                if (count >= maxLines) break;
                StringBuilder sb = new StringBuilder();
                String type = p.getPunishmentType() != null ? p.getPunishmentType().toUpperCase() : "UNKNOWN";
                sb.append(type);
                if (p.getReason() != null && !p.getReason().isEmpty()) {
                    sb.append(" - ").append(p.getReason());
                }
                if (p.getOperator() != null && !p.getOperator().isEmpty()) {
                    sb.append(" (").append(p.getOperator()).append(")");
                }
                if (p.getDuration() != null && p.getDuration() > 0) {
                    sb.append(" [").append(TimeUtil.formatDuration(p.getDuration())).append("]");
                }
                sb.append(p.getIsActive() ? " [AKTIVNÍ]" : " [NEAKTIVNÍ]");
                punishLore.add(Component.text(sb.toString()));
                count++;
            }
            if (punishments.size() > maxLines) {
                punishLore.add(Component.text("… a další " + (punishments.size() - maxLines) + " …"));
            }
        }

        // Paper with punishments next to the head
        ItemStack punishPaper = new ItemStack(Material.PAPER);
        ItemMeta punishMeta = punishPaper.getItemMeta();
        punishMeta.displayName(Component.text("Tresty hráče", NamedTextColor.YELLOW));
        punishMeta.lore(punishLore);
        punishPaper.setItemMeta(punishMeta);
        contents.set(1, 5, ClickableItem.empty(punishPaper));

        // Moderation items (consistent block type: CONCRETE variants)
        // Kick -> White Concrete
        ItemStack kickItem = new ItemStack(Material.WHITE_CONCRETE);
        ItemMeta kickMeta = kickItem.getItemMeta();
        kickMeta.displayName(Component.text("Kick Player", NamedTextColor.WHITE));
        kickItem.setItemMeta(kickMeta);
        contents.set(2, 2, ClickableItem.of(kickItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.KICK).open(player)));

        // Ban -> Red Concrete
        ItemStack banItem = new ItemStack(Material.RED_CONCRETE);
        ItemMeta banMeta = banItem.getItemMeta();
        banMeta.displayName(Component.text("Ban Player", NamedTextColor.RED));
        banItem.setItemMeta(banMeta);
        contents.set(2, 3, ClickableItem.of(banItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.BAN).open(player)));

        // Tempban -> Orange Concrete
        ItemStack tempbanItem = new ItemStack(Material.ORANGE_CONCRETE);
        ItemMeta tempbanMeta = tempbanItem.getItemMeta();
        tempbanMeta.displayName(Component.text("Tempban Player", NamedTextColor.GOLD));
        tempbanItem.setItemMeta(tempbanMeta);
        contents.set(2, 4, ClickableItem.of(tempbanItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.TEMPBAN).open(player)));

        // IP Ban -> Black Concrete
        ItemStack ipbanItem = new ItemStack(Material.BLACK_CONCRETE);
        ItemMeta ipbanMeta = ipbanItem.getItemMeta();
        ipbanMeta.displayName(Component.text("IP Ban Player", NamedTextColor.DARK_GRAY));
        ipbanItem.setItemMeta(ipbanMeta);
        contents.set(2, 5, ClickableItem.of(ipbanItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.IPBAN).open(player)));

        // Mute -> Gray Concrete
        ItemStack muteItem = new ItemStack(Material.GRAY_CONCRETE);
        ItemMeta muteMeta = muteItem.getItemMeta();
        muteMeta.displayName(Component.text("Mute Player", NamedTextColor.GRAY));
        muteItem.setItemMeta(muteMeta);
        contents.set(3, 2, ClickableItem.of(muteItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.MUTE).open(player)));

        // Tempmute -> Light Gray Concrete
        ItemStack tempmuteItem = new ItemStack(Material.LIGHT_GRAY_CONCRETE);
        ItemMeta tempmuteMeta = tempmuteItem.getItemMeta();
        tempmuteMeta.displayName(Component.text("Tempmute Player", NamedTextColor.GRAY));
        tempmuteItem.setItemMeta(tempmuteMeta);
        contents.set(3, 3, ClickableItem.of(tempmuteItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.TEMPMUTE).open(player)));

        // Warn -> Yellow Concrete
        ItemStack warnItem = new ItemStack(Material.YELLOW_CONCRETE);
        ItemMeta warnMeta = warnItem.getItemMeta();
        warnMeta.displayName(Component.text("Warn Player", NamedTextColor.YELLOW));
        warnItem.setItemMeta(warnMeta);
        contents.set(3, 4, ClickableItem.of(warnItem, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.WARN).open(player)));
    }
}
