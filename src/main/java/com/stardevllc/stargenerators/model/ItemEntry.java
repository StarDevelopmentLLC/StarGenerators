package com.stardevllc.stargenerators.model;

import com.stardevllc.minecraft.itembuilder.ItemBuilder;
import com.stardevllc.starlib.objects.key.Key;
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
    protected Key key;
    
    /**
     * The builder used to create items
     */
    protected final ItemBuilder<?, ?> builder;
    
    /**
     * The boolean based flags for the entry
     */
    protected final Set<Flag> flags = EnumSet.noneOf(Flag.class);
    
    public ItemEntry(ItemBuilder<?, ?> builder, Flag... flags) {
        this.builder = builder;
        if (flags != null) {
            this.flags.addAll(List.of(flags));
        }
    }
    
    public ItemEntry(ItemBuilder<?, ?> builder, List<Flag> flags) {
        this.builder = builder;
        this.flags.addAll(flags);
    }
    
    @Override
    public Key getKey() {
        return key;
    }
    
    @Override
    public void setKey(Key key) {
        this.key = key;
    }
    
    public ItemBuilder<?, ?> getBuilder() {
        return builder;
    }
    
    public ItemStack createItemStack(boolean saveData) {
        ItemStack itemStack = builder.build();
        if (saveData) {
            NBT.modify(itemStack, nbt -> {
                nbt.setString(NBT_KEY, getKey().toString());
            });
        }
        return itemStack;
    }
    
    public ItemStack createItemStack() {
        return createItemStack(true);
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