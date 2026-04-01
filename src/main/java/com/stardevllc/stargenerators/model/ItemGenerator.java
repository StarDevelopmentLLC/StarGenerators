package com.stardevllc.stargenerators.model;

import com.stardevllc.Cuboid;
import com.stardevllc.Position;
import com.stardevllc.stargenerators.StarGenerators;
import com.stardevllc.stargenerators.model.listener.ItemPickupListener;
import com.stardevllc.stargenerators.model.listener.ItemSpawnListener;
import com.stardevllc.starlib.clock.callback.CallbackPeriod;
import com.stardevllc.starlib.clock.clocks.Stopwatch;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.values.property.BooleanProperty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class ItemGenerator implements Generator<ItemEntry> {
    /**
     * A unique identifier for the generator itself <br>
     * It is best to auto-generate this id and have the entry ids be readable
     */
    protected Key key;
    
    protected String name;
    
    protected final Map<Key, ItemEntry> entries = new HashMap<>();
    
    private final Map<Key, ItemEntryHolder> holders = new HashMap<>();
    
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
    
    private static final class ItemEntryHolder {
        private final ItemEntry itemEntry;
        private final CallbackPeriod period;
        private Position position;
        private long cooldown;
        private UUID callbackId;
        
        private final List<ItemPickupListener> pickupListeners = new ArrayList<>();
        private final List<ItemSpawnListener> spawnListeners = new ArrayList<>();
        
        public ItemEntryHolder(ItemEntry e) {
            this.itemEntry = e;
            this.period = () -> cooldown;
        }
    }
    
    public ItemGenerator(Key key, Position boundsMin, Position boundsMax) {
        this(key, null, boundsMin, boundsMax);
    }
    
    public ItemGenerator(Key id, Collection<ItemEntry> itemEntries, Position boundsMin, Position boundsMax) {
        this.key = id;
        
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
    
    public void addEntry(ItemEntry entry, Position position, long cooldown) {
        ItemEntryHolder holder = new ItemEntryHolder(entry);
        holder.position = position;
        holder.cooldown = cooldown;
        this.holders.put(entry.getKey(), holder);
        
        holder.callbackId = this.stopwatch.addRepeatingCallback(snapshot -> spawnItems(holder), holder.period);
        this.entries.put(entry.getKey(), entry);
    }
    
    private void spawnItems(ItemEntryHolder holder) {
        int currentItemCount = getSpawnedItemsCount(holder.itemEntry.getKey());
        Location location = holder.position.toBlockLocation(world).add(0.5, 0, 0.5);
        
        for (int i = 0; i < holder.itemEntry.getBuilder().getAmount(); i++) {
            if (currentItemCount < holder.itemEntry.getMaxItems()) {
                ItemStack itemStack = holder.itemEntry.createItemStack();
                int amount = itemStack.getAmount();
                itemStack.setAmount(1);
                Item item = world.dropItem(location, itemStack);
                item.setVelocity(new Vector());
                SpawnedItem spawnedItem = this.addSpawnedItem(holder.itemEntry.getKey(), item);
                holder.spawnListeners.forEach(l -> l.onSpawn(spawnedItem));
                currentItemCount++;
            }
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
            }
        }
    }
    
    @Override
    public ItemEntry getEntry(Key id) {
        return this.entries.get(id);
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