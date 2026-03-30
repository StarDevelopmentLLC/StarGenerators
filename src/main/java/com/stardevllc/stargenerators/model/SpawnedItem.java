package com.stardevllc.stargenerators.model;

import org.bukkit.entity.Item;

public record SpawnedItem(Item item, ItemGenerator generator, ItemEntry entry) {
}