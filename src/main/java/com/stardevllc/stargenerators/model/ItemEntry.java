package com.stardevllc.stargenerators.model;

import com.stardevllc.itembuilder.common.ItemBuilder;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.objects.key.impl.StringKey;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemEntry implements GeneratorEntry {
    
    public static final String NBT_KEY = "itementrykey";
    
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
        INVENTORY_PICKUP,
        
        /**
         * Controls if the data put on the item in world is kept when being picked up
         */
        KEEP_DATA
    }
    
    /**
     * A unique identifier for the item entry. This is per generator
     */
    protected final Key key;
    
    /**
     * The builder used to create items
     */
    protected final ItemBuilder<?, ?> builder;
    
    /**
     * The max amount of items that can be within the generator bounds
     */
    protected int maxItems;
    
    /**
     * The boolean based flags for the entry
     */
    protected final Set<Flag> flags = EnumSet.noneOf(Flag.class);
    
    public ItemEntry(Key key, ItemBuilder<?, ?> builder) {
        this.key = key;
        this.builder = builder;
    }
    
    public ItemEntry(String key, ItemBuilder<?, ?> builder, int maxItems, Flag... flags) {
        this.key = new StringKey(key);
        this.builder = builder;
        this.maxItems = maxItems;
        if (flags != null) {
            this.flags.addAll(List.of(flags));
        }
    }
    
    public ItemEntry(String key, ItemBuilder<?, ?> builder, int maxItems, List<Flag> flags) {
        this.key = new StringKey(key);
        this.builder = builder;
        this.maxItems = maxItems;
        if (flags != null) {
            this.flags.addAll(flags);
        }
    }
    
    @Override
    public Key getKey() {
        return key;
    }
    
    public ItemBuilder<?, ?> getBuilder() {
        return builder;
    }
    
    public ItemStack createItemStack() {
        ItemStack itemStack = builder.build();
        NBT.modify(itemStack, nbt -> {
            nbt.setString(NBT_KEY, getKey().toString());
        });
        return itemStack;
    }
    
    public int getMaxItems() {
        return maxItems;
    }
    
    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }
    
    public Set<Flag> getFlags() {
        return EnumSet.copyOf(this.flags);
    }
    
    public boolean hasFlag(Flag flag) {
        return this.flags.contains(flag);
    }
    
    public void addFlag(Flag flag) {
        this.flags.add(flag);
    }
}