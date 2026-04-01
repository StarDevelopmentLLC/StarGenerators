package com.stardevllc.stargenerators.listener;

import com.stardevllc.stargenerators.StarGenerators;
import com.stardevllc.stargenerators.model.*;
import com.stardevllc.stargenerators.model.ItemEntry.Flag;
import com.stardevllc.starlib.objects.key.Keys;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.entity.Item;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class GeneratorListener implements Listener {
    
    @EventHandler
    public void onItemMerge(ItemMergeEvent e) {
        ItemStack itemStack = e.getEntity().getItemStack();
        String key = NBT.get(itemStack, nbt -> {
            return nbt.getString(ItemEntry.NBT_KEY);
        });
        
        if (key == null || key.isBlank()) {
            return;
        }
        
        ItemEntry itemEntry = StarGenerators.ITEMS.get(key);
        if (itemEntry != null) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onInventoryItemPickup(InventoryPickupItemEvent e) {
        handleItemEvent(e.getItem(), Flag.INVENTORY_PICKUP, e);
    }
    
    @EventHandler
    public void onItemDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item entity)) {
            return;
        }
        
        handleItemEvent(entity, Flag.INVULNERABLE, e);
    }
    
    private void handleItemEvent(Item itemEntity, Flag flag, Cancellable e) {
        ItemStack itemStack = itemEntity.getItemStack();
        String itemKey = NBT.get(itemStack, nbt -> {
            return nbt.getString(ItemEntry.NBT_KEY);
        });
        
        if (itemKey == null || itemKey.isBlank()) {
            return;
        }
        
        ItemEntry itemEntry = StarGenerators.ITEMS.get(itemKey);
        if (itemEntry == null) {
            return;
        }
        
        String generatorKey = NBT.get(itemStack, nbt -> {
            return nbt.getString(ItemGenerator.NBT_KEY);
        });
        
        if (generatorKey == null || generatorKey.isBlank()) {
            return;
        }
        
        ItemGenerator itemGenerator = StarGenerators.ITEM_GENERATORS.get(generatorKey);
        if (itemGenerator == null) {
            return;
        }
        
        if (!itemGenerator.hasEntry(Keys.of(itemKey))) {
            return;
        }
        
        Set<SpawnedItem> spawnedItems = itemGenerator.getSpawnedItems(Keys.of(itemKey));
        for (SpawnedItem spawnedItem : spawnedItems) {
            if (spawnedItem.item().equals(itemEntity)) {
                if (spawnedItem.entry().hasFlag(flag)) {
                    e.setCancelled(true);
                } else {
                    itemGenerator.removedSpawnedItem(itemEntity);
                }
                return;
            }
        }
    }
    
    @EventHandler
    public void onItemDespawn(ItemDespawnEvent e) {
        handleItemEvent(e.getEntity(), Flag.PERSISTENT, e);
    }
}