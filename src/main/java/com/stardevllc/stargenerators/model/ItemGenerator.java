package com.stardevllc.stargenerators.model;

import com.stardevllc.Cuboid;
import com.stardevllc.Position;
import com.stardevllc.stargenerators.StarGenerators;
import com.stardevllc.starlib.clock.callback.CallbackPeriod;
import com.stardevllc.starlib.clock.clocks.Stopwatch;
import com.stardevllc.starlib.collections.observable.map.ObservableHashMap;
import com.stardevllc.starlib.collections.observable.map.ObservableMap;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.values.property.BooleanProperty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Item;

import java.util.*;

public class ItemGenerator implements Generator<ItemEntry> {
    /**
     * A unique identifier for the generator itself <br>
     * It is best to auto-generate this id and have the entry ids be readable
     */
    protected Key key;
    
    protected String name;
    
    protected final ObservableMap<Key, ItemEntry> entries = new ObservableHashMap<>();
    
    private final List<ItemEntryHolder> holders = new ArrayList<>();
    
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
        private UUID callbackId;
        
        public ItemEntryHolder(ItemEntry e) {
            this.itemEntry = e;
            this.period = itemEntry::getCooldown;
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
        
        this.entries.addListener(c -> {
            ItemEntry itemEntry = c.added();
            if (itemEntry != null) {
                ItemEntryHolder holder = new ItemEntryHolder(c.added());
                this.holders.add(holder);
                
                holder.callbackId = this.stopwatch.addRepeatingCallback(snapshot -> {
                    int currentItemCount = getSpawnedItemsCount(itemEntry.getKey());
                    if (currentItemCount < itemEntry.getMaxItems()) {
                        for (Item item : itemEntry.spawn(world, ItemGenerator.this)) {
                            addSpawnedItem(itemEntry.getKey(), item);
                        }
                    }
                }, holder.period);
            } else if (c.removed() != null) {
                Iterator<ItemEntryHolder> iterator = this.holders.iterator();
                while (iterator.hasNext()) {
                    ItemEntryHolder holder = iterator.next();
                    if (holder.itemEntry.getKey().equals(c.removed().getKey())) {
                        this.stopwatch.removeCallback(holder.callbackId);
                    }
                    iterator.remove();
                }
            }
        });
        
        
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
        for (ItemEntryHolder holder : this.holders) {
            if (holder.itemEntry.getKey().equals(entry.getKey())) {
                long nextRun = this.stopwatch.getNextRun(holder.callbackId);
                return nextRun - this.stopwatch.getTime();
            }
        }
        
        return 0;
    }
    
    @Override
    public void start() {
        this.stopwatch.unpause();
    }
    
    @Override
    public void stop() {
        this.stopwatch.pause();
    }
    
    @Override
    public void addEntry(ItemEntry entry) {
        this.entries.put(entry.getKey(), entry);
    }
    
    @Override
    public ItemEntry getEntry(Key id) {
        return this.entries.get(id);
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
    
    public void addSpawnedItem(Key entryId, Item item) {
        ItemEntry entry = getEntry(entryId);
        if (entry == null) {
            return;
        }
        
        SpawnedItem spawnedItem = new SpawnedItem(item, this, entry);
        this.spawnedItems.add(spawnedItem);
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