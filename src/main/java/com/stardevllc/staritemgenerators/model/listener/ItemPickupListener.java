package com.stardevllc.staritemgenerators.model.listener;

import com.stardevllc.staritemgenerators.model.ItemEntry;
import com.stardevllc.staritemgenerators.model.ItemGenerator;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ItemPickupListener extends ItemEntryListener {
    void onPickup(LivingEntity entity, Item item, ItemEntry entry, ItemGenerator generator);
}
