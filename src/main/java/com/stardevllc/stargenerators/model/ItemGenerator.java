package com.stardevllc.stargenerators.model;

import com.stardevllc.minecraft.Cuboid;
import com.stardevllc.minecraft.Position;
import com.stardevllc.minecraft.itembuilder.ItemBuilder;
import com.stardevllc.stargenerators.StarGenerators;
import com.stardevllc.stargenerators.model.listener.ItemPickupListener;
import com.stardevllc.stargenerators.model.listener.ItemSpawnListener;
import com.stardevllc.starcore.ItemBuilders;
import com.stardevllc.starlib.clock.callback.CallbackPeriod;
import com.stardevllc.starlib.clock.clocks.Stopwatch;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.values.property.BooleanProperty;
import de.tr7zw.nbtapi.NBT;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemGenerator implements Generator<ItemEntry> {
    
    public static final String NBT_KEY = "itemgeneratorkey";
    
    /**
     * A unique identifier for the generator itself <br>
     * It is best to auto-generate this id and have the entry ids be readable
     */
    protected Key key;
    
    protected String name;
    
    protected final Map<Key, ItemEntry> entries = new HashMap<>();
    
    protected final Map<Key, ItemEntryHolder> holders = new HashMap<>();
    
    /**
     * The min and max positions for the generator. This is Bukkit World independent, useful for minigames
     */
    protected Position boundsMin, boundsMax;
    
    /**
     * Since stopwatches are infinite clocks using longs to track time, we can use it to control timings of the item generation
     */
    protected final Stopwatch stopwatch;
    
    protected World world;
    
    protected final BooleanProperty initProperty;
    
    protected final Set<SpawnedItem> spawnedItems = new HashSet<>();
    
    protected Cuboid region;
    
    @Override
    public String getName() {
        return name;
    }
    
    protected static final class ItemEntryHolder {
        protected final ItemEntry itemEntry;
        protected final CallbackPeriod period;
        protected Position position;
        protected long cooldown;
        protected int maxItems;
        protected int stackSize;
        protected UUID callbackId;
        
        protected final List<ItemPickupListener> pickupListeners = new ArrayList<>();
        protected final List<ItemSpawnListener> spawnListeners = new ArrayList<>();
        
        public ItemEntryHolder(ItemEntry e) {
            this.itemEntry = e;
            this.period = () -> cooldown;
        }
    }
    
    public ItemGenerator(String name, Position boundsMin, Position boundsMax) {
        this(name, null, boundsMin, boundsMax);
    }
    
    public ItemGenerator(String name, Collection<ItemEntry> itemEntries, Position boundsMin, Position boundsMax) {
        this.name = name;
        if (itemEntries != null) {
            for (ItemEntry itemEntry : itemEntries) {
                this.entries.put(itemEntry.getKey(), itemEntry);
            }
        }
        
        this.initProperty = new BooleanProperty(this, "init", false);
        
        this.stopwatch = StarGenerators.getClockManager().createStopwatch(0, 0);
        
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
    }
    
    @Override
    public void init(World world) {
        this.world = world;
        this.initProperty.set(true);
        this.region = new Cuboid(new Location(world, this.boundsMin.getBlockX(), this.boundsMin.getBlockY(), this.boundsMin.getBlockZ()), new Location(world, this.boundsMax.getBlockX(), this.boundsMax.getBlockY(), this.boundsMax.getBlockZ()));
    }
    
    public Stopwatch getStopwatch() {
        return stopwatch;
    }
    
    public long getNextSpawn(ItemEntry entry) {
        ItemEntryHolder holder = this.holders.get(entry.getKey());
        if (holder == null) {
            return 0;
        }
        
        long nextRun = this.stopwatch.getNextRun(holder.callbackId);
        return nextRun - this.stopwatch.getTime();
    }
    
    @Override
    public void start() {
        this.stopwatch.unpause();
    }
    
    @Override
    public void stop() {
        this.stopwatch.pause();
    }
    
    public void setEntryValues(Key entryKey, long cooldown, int maxItems, int stackSize) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            holder.cooldown = cooldown;
            holder.maxItems = maxItems;
            holder.stackSize = stackSize;
        }
    }
    
    public void setEntryValues(ItemEntry entry, long cooldown, int maxItems, int stackSize) {
        if (entry != null) {
            setEntryValues(entry.getKey(), cooldown, maxItems, stackSize);
        }
    }
    
    public void addEntry(ItemEntry entry, Position position, long cooldown, int maxItems, int stackSize) {
        ItemEntryHolder holder = new ItemEntryHolder(entry);
        holder.position = position;
        holder.cooldown = cooldown;
        holder.maxItems = maxItems;
        holder.stackSize = stackSize;
        this.holders.put(entry.getKey(), holder);
        
        holder.callbackId = this.stopwatch.addRepeatingCallback(snapshot -> spawnItems(holder), holder.period);
        this.entries.put(entry.getKey(), entry);
    }
    
    private void spawnItems(ItemEntryHolder holder) {
        int currentItemCount = getSpawnedItemsCount(holder.itemEntry.getKey());
        Location location = holder.position.toBlockLocation(world).add(0.5, 0, 0.5);
        
        ItemStack itemStack = holder.itemEntry.createItemStack();
        NBT.modify(itemStack, nbt -> {
            nbt.setString(NBT_KEY, getKey().toString());
        });
        itemStack.setAmount(1);
        
        for (int i = 0; i < holder.stackSize && currentItemCount < holder.maxItems; i++) {
            Item item = world.dropItem(location, itemStack);
            item.setVelocity(new Vector());
            SpawnedItem spawnedItem = this.addSpawnedItem(holder.itemEntry.getKey(), item);
            holder.spawnListeners.forEach(l -> l.onSpawn(spawnedItem));
            currentItemCount++;
        }
    }
    
    public void handleItemPickup(LivingEntity entity, Item item) {
        for (SpawnedItem spawnedItem : this.spawnedItems) {
            if (spawnedItem.item().equals(item)) {
                ItemEntryHolder holder = this.holders.get(spawnedItem.entry().getKey());
                if (holder == null) {
                    return;
                }
                
                holder.pickupListeners.forEach(l -> l.onPickup(entity, spawnedItem));
                
                if (!spawnedItem.entry().hasFlag(ItemEntry.Flag.KEEP_DATA)) {
                    NBT.modify(item.getItemStack(), nbt -> {
                        nbt.removeKey(ItemEntry.NBT_KEY);
                        nbt.removeKey(ItemGenerator.NBT_KEY);
                    });
                    
                    ItemBuilder<?, ?> itemBuilder = ItemBuilders.of(item.getItemStack());
                    itemBuilder.clearCustomNBT();
                    item.setItemStack(itemBuilder.build());
                }
            }
        }
    }
    
    @Override
    public ItemEntry getEntry(Key id) {
        return this.entries.get(id);
    }
    
    public int getStackSize(Key entryKey) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            return holder.stackSize;
        }
        return 0;
    }
    
    public int getStackSize(ItemEntry itemEntry) {
        return getStackSize(itemEntry.getKey());
    }
    
    public void setStackSize(Key entryKey, int stackSize) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            holder.stackSize = stackSize;
        }
    }
    
    public void setStackSize(ItemEntry itemEntry, int stackSize) {
        setStackSize(itemEntry.getKey(), stackSize);
    }
    
    public int getMaxItems(Key entryKey) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            return holder.maxItems;
        }
        
        return 0;
    }
    
    public int getMaxItems(ItemEntry entry) {
        return getMaxItems(entry.getKey());
    }
    
    public void setMaxItems(Key entryKey, int maxItems) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            holder.maxItems = maxItems;
        }
    }
    
    public void setMaxItems(ItemEntry entry, int maxItems) {
        setMaxItems(entry.getKey(), maxItems);
    }
    
    public void addPickupListener(Key entryKey, ItemPickupListener listener) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            holder.pickupListeners.add(listener);
        }
    }
    
    public void addPickupListener(ItemEntry entry, ItemPickupListener listener) {
        addPickupListener(entry.getKey(), listener);
    }
    
    public void addSpawnListener(Key entryKey, ItemSpawnListener listener) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder != null) {
            holder.spawnListeners.add(listener);
        }
    }
    
    public void addSpawnListener(ItemEntry entry, ItemSpawnListener listener) {
        addSpawnListener(entry.getKey(), listener);
    }
    
    public Position getSpawnPosition(Key entryKey) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder == null) {
            return null;
        }
        
        return holder.position;
    }
    
    public Position getSpawnPosition(ItemEntry itemEntry) {
        return getSpawnPosition(itemEntry.getKey());
    }
    
    public void setSpawnPosition(Key entryKey, Position position) {
        if (position == null) {
            return;
        }
        
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder == null) {
            return;
        }
        
        holder.position = position;
    }
    
    public void setSpawnPosition(ItemEntry itemEntry, Position position) {
        setSpawnPosition(itemEntry.getKey(), position);
    }
    
    public long getEntryCooldown(Key entryKey) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder == null) {
            return 0;
        }
        
        return holder.cooldown;
    }
    
    public long getEntryCooldown(ItemEntry itemEntry) {
        return getEntryCooldown(itemEntry.getKey());
    }
    
    public void setEntryCooldown(Key entryKey, long cooldown) {
        ItemEntryHolder holder = this.holders.get(entryKey);
        if (holder == null) {
            return;
        }
        
        holder.cooldown = cooldown;
    }
    
    public void setEntryCooldown(ItemEntry itemEntry, long cooldown) {
        setEntryCooldown(itemEntry.getKey(), cooldown);
    }
    
    @Override
    public Collection<ItemEntry> getEntries() {
        return new ArrayList<>(this.entries.values());
    }
    
    public Cuboid getRegion() {
        return region;
    }
    
    public Set<SpawnedItem> getSpawnedItems() {
        return new HashSet<>(spawnedItems);
    }
    
    public Set<SpawnedItem> getSpawnedItems(Key itemKey) {
        Set<SpawnedItem> spawnedItems = new HashSet<>();
        for (SpawnedItem spawnedItem : this.spawnedItems) {
            if (spawnedItem.entry().getKey().equals(itemKey)) {
                spawnedItems.add(spawnedItem);
            }
        }
        
        return spawnedItems;
    }
    
    public SpawnedItem addSpawnedItem(Key entryId, Item item) {
        ItemEntry entry = getEntry(entryId);
        if (entry == null) {
            return null;
        }
        
        SpawnedItem spawnedItem = new SpawnedItem(item, this, entry);
        this.spawnedItems.add(spawnedItem);
        return spawnedItem;
    }
    
    public void removedSpawnedItem(Item item) {
        this.spawnedItems.removeIf(spawnedItem -> spawnedItem.item().equals(item));
    }
    
    public int getSpawnedItemsCount(Key entryId) {
        int count = 0;
        for (SpawnedItem spawnedItem : this.spawnedItems) {
            if (spawnedItem.entry().getKey().equals(entryId)) {
                count++;
            }
        }
        
        return count;
    }
    
    public int getSpawnedItemsCount() {
        return this.spawnedItems.size();
    }
    
    @Override
    public Key getKey() {
        return key;
    }
    
    @Override
    public void setKey(Key key) {
        this.key = key;
    }
    
    public Position getBoundsMin() {
        return boundsMin;
    }
    
    public Position getBoundsMax() {
        return boundsMax;
    }
    
    public boolean isInitialized() {
        return this.initProperty.get();
    }
    
    public boolean isRunning() {
        return !this.stopwatch.isPaused() && !this.stopwatch.isCancelled();
    }
    
    public World getWorld() {
        return world;
    }
    
    public boolean hasEntry(Key entryId) {
        return this.entries.containsKey(entryId);
    }
}