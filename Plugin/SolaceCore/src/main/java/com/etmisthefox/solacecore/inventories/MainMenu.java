package com.etmisthefox.solacecore.inventories;

import com.etmisthefox.inv.ClickableItem;
import com.etmisthefox.inv.InventoryManager;
import com.etmisthefox.inv.SmartInventory;
import com.etmisthefox.inv.content.InventoryContents;
import com.etmisthefox.inv.content.InventoryProvider;
import com.etmisthefox.solacecore.commands.KickCommand;
import com.etmisthefox.solacecore.database.Database;
import com.etmisthefox.solacecore.enums.PunishmentType;
import com.etmisthefox.solacecore.managers.LanguageManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

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
        blackStainedGlassPaneMeta.setDisplayName(" ");
        blackStainedGlassPane.setItemMeta(blackStainedGlassPaneMeta);
        contents.fillBorders(ClickableItem.empty(blackStainedGlassPane));

        // Close -> Barrier
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        barrierMeta.setDisplayName("Close");
        barrier.setItemMeta(barrierMeta);
        contents.set(5, 4, ClickableItem.of(barrier, e -> player.closeInventory()));

        // Player's Head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta playerHeadMeta = (SkullMeta) playerHead.getItemMeta();
        playerHeadMeta.setDisplayName(target.getName());
        playerHeadMeta.setOwningPlayer(target);
        playerHead.setItemMeta(playerHeadMeta);
        contents.set(1, 4, ClickableItem.empty(playerHead));

        // Kick -> Paper
        ItemStack paperKick = new ItemStack(Material.PAPER);
        ItemMeta paperKickMeta = paperKick.getItemMeta();
        paperKickMeta.setDisplayName("Kick Player");
        paperKick.setItemMeta(paperKickMeta);
        contents.set(2, 2, ClickableItem.of(paperKick, e -> PunishmentMenu.getInventory(database, lang, inventoryManager, target, PunishmentType.KICK).open(player)));
    }
}

