package com.stardevllc.staritemgenerators.model;

import com.stardevllc.Position;
import com.stardevllc.itembuilder.common.ItemBuilder;
import com.stardevllc.staritemgenerators.model.listener.ItemPickupListener;
import com.stardevllc.staritemgenerators.model.listener.ItemSpawnListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemEntry {
    
    public enum Flag {
        /**
         * Controls if the item can despawn
         */
        PERSISTENT,
        
        /**
         * Controls if the item can be destroyed
         */
        INVULNERABLE,
        
        /**
         * Controls if the item can be picked up by inventories
         */
        INVENTORY_PICKUP
    }
    
    /**
     * A unique identifier for the item entry. This is per generator
     */
    protected String id;
    
    /**
     * The builder used to create items
     */
    protected ItemBuilder<?, ?> builder;
    
    /**
     * The cooldown in milliseconds before the next item spawns
     */
    protected long cooldown;
    
    /**
     * The max amount of items that can be within the generator bounds
     */
    protected int maxItems;
    
    /**
     * The base spawn position for the items
     */
    protected Position spawnPosition;
    
    /**
     * The boolean based flags for the entry
     */
    protected final Set<Flag> flags = EnumSet.noneOf(Flag.class);
    
    private final List<ItemPickupListener> itemPickupListeners = new ArrayList<>();
    private final List<ItemSpawnListener> itemSpawnListeners = new ArrayList<>();
    
    public ItemEntry(String id, ItemBuilder<?, ?> builder, long cooldown, int maxItems, Position spawnPosition, Flag... flags) {
        this.id = id;
        this.builder = builder;
        this.cooldown = cooldown;
        this.maxItems = maxItems;
        this.spawnPosition = spawnPosition;
        if (flags != null) {
            this.flags.addAll(List.of(flags));
        }
    }
    
    public ItemEntry(String id, ItemBuilder<?, ?> builder, long cooldown, int maxItems, Position spawnPosition, List<Flag> flags) {
        this.id = id;
        this.builder = builder;
        this.cooldown = cooldown;
        this.maxItems = maxItems;
        this.spawnPosition = spawnPosition;
        if (flags != null) {
            this.flags.addAll(flags);
        }
    }
    
    public void addSpawnListener(ItemSpawnListener listener) {
        this.itemSpawnListeners.add(listener);
    }
    
    public void handleItemSpawn(Item item, ItemEntry itemEntry, ItemGenerator generator) {
        for (ItemSpawnListener listener : this.itemSpawnListeners) {
            listener.onSpawn(item, itemEntry, generator);
        }
    }
    
    public void addPickupListener(ItemPickupListener listener) {
        this.itemPickupListeners.add(listener);
    }
    
    public void handleItemPickup(LivingEntity entity, Item item, ItemEntry itemEntry, ItemGenerator generator) {
        for (ItemPickupListener listener : this.itemPickupListeners) {
            listener.onPickup(entity, item, itemEntry, generator);
        }
    }
    
    public List<Item> spawn(World world, ItemGenerator generator) {
        Location location = spawnPosition.toBlockLocation(world).add(0.5, 0, 0.5);
        ItemStack itemStack = createItemStack();
        int amount = itemStack.getAmount();
        itemStack.setAmount(1);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Item item = world.dropItem(location, itemStack);
            item.setVelocity(new Vector());
            handleItemSpawn(item, this, generator);
            items.add(item);
        }
        
        return items;
    }
    
    public String getId() {
        return id;
    }
    
    public ItemBuilder<?, ?> getBuilder() {
        return builder;
    }
    
    public ItemStack createItemStack() {
        return builder.build();
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public int getMaxItems() {
        return maxItems;
    }
    
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
    }
    
    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }
    
    public Position getSpawnPosition() {
        return spawnPosition;
    }
    
    public Set<Flag> getFlags() {
        return EnumSet.copyOf(this.flags);
    }
    
    public boolean hasFlag(Flag flag) {
        return this.flags.contains(flag);
    }
}