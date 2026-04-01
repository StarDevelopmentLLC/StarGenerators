package com.stardevllc.stargenerators.model.listener;

import com.stardevllc.stargenerators.model.SpawnedItem;
import org.bukkit.entity.LivingEntity;

@FunctionalInterface
public interface ItemPickupListener extends ItemEntryListener {
    void onPickup(LivingEntity entity, SpawnedItem item);
}
