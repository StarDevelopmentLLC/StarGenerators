package com.stardevllc.staritemgenerators.model.listener;

import com.stardevllc.staritemgenerators.model.ItemEntry;
import com.stardevllc.staritemgenerators.model.ItemGenerator;
import org.bukkit.entity.Item;

@FunctionalInterface
public interface ItemSpawnListener extends ItemEntryListener {
    void onSpawn(Item item, ItemEntry entry, ItemGenerator generator);
}