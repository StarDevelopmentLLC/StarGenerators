package com.stardevllc.stargenerators.model.listener;

import com.stardevllc.stargenerators.model.SpawnedItem;

@FunctionalInterface
public interface ItemSpawnListener extends ItemEntryListener {
    void onSpawn(SpawnedItem item);
}