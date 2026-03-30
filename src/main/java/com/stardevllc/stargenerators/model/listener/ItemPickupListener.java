package com.stardevllc.stargenerators.model.listener;

import com.stardevllc.stargenerators.model.ItemEntry;
import com.stardevllc.stargenerators.model.ItemGenerator;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ItemPickupListener extends ItemEntryListener {
    void onPickup(LivingEntity entity, Item item, ItemEntry entry, ItemGenerator generator);
}
