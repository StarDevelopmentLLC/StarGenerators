package com.stardevllc.stargenerators.model.listener;

import com.stardevllc.stargenerators.model.ItemEntry;
import com.stardevllc.stargenerators.model.ItemGenerator;
import org.bukkit.entity.Item;

@FunctionalInterface
public interface ItemSpawnListener extends ItemEntryListener {
    void onSpawn(Item item, ItemEntry entry, ItemGenerator generator);
}