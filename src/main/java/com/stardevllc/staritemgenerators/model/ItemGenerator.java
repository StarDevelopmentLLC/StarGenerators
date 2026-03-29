package com.stardevllc.staritemgenerators.model;

import com.stardevllc.Cuboid;
import com.stardevllc.Position;
import com.stardevllc.starlib.clock.ClockManager;
import com.stardevllc.starlib.clock.callback.CallbackPeriod;
import com.stardevllc.starlib.clock.clocks.Stopwatch;
import com.stardevllc.starlib.collections.observable.list.ObservableArrayList;
import com.stardevllc.starlib.collections.observable.list.ObservableList;
import com.stardevllc.starlib.injector.Inject;
import com.stardevllc.starlib.values.property.BooleanProperty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.util.*;

public class ItemGenerator {
    /**
     * A unique identifier for the generator itself <br>
     * It is best to auto-generate this id and have the entry ids be readable
     */
    protected String id;
    
    /**
     * The entries that control what items spawn in the generator
     */
    protected final ObservableList<ItemEntry> itemEntries;
    
    private final List<ItemEntryHolder> holders = new ArrayList<>();
    
    /**
     * The min and max positions for the generator. This is Bukkit World independent, useful for minigames
     */
    protected Position boundsMin, boundsMax;
    
    /**
     * Since stopwatches are infinite clocks using longs to track time, we can use it to control timings of the item generation
     */
    protected Stopwatch stopwatch;
    
    @Inject
    protected ClockManager clockManager;
    
    protected World world;
    
    protected final BooleanProperty initProperty;
    
    protected final Set<SpawnedItem> spawnedItems = new HashSet<>();
    
    protected Cuboid region;
    
    private static final class ItemEntryHolder {
        private final ItemEntry itemEntry;
        private final CallbackPeriod period;
        private UUID callbackId;
        
        public ItemEntryHolder(ItemEntry e) {
            this.itemEntry = e;
            this.period = itemEntry::getCooldown;
        }
    }
    
    public ItemGenerator(String id, List<ItemEntry> itemEntries, Position boundsMin, Position boundsMax) {
        this.id = id;
        this.itemEntries = new ObservableArrayList<>(itemEntries);
        this.initProperty = new BooleanProperty(this, "init", false);
        this.itemEntries.addListener(c -> {
            ItemEntry itemEntry = c.added();
            if (itemEntry != null) {
                ItemEntryHolder holder = new ItemEntryHolder(c.added());
                this.holders.add(holder);
                
                holder.callbackId = this.stopwatch.addRepeatingCallback(snapshot -> {
                    int currentItemCount = getSpawnedItemsCount(itemEntry.getId());
                    if (currentItemCount < itemEntry.getMaxItems()) {
                        for (Item item : itemEntry.spawn(world, ItemGenerator.this)) {
                            addSpawnedItem(itemEntry.getId(), item);
                        }
                    }
                }, holder.period);
            } else if (c.removed() != null) {
                Iterator<ItemEntryHolder> iterator = this.holders.iterator();
                while (iterator.hasNext()) {
                    ItemEntryHolder holder = iterator.next();
                    if (holder.itemEntry.getId().equals(c.removed().getId())) {
                        this.stopwatch.removeCallback(holder.callbackId);
                    }
                    iterator.remove();
                }
            }
        });
        
        this.boundsMin = boundsMin;
        this.boundsMax = boundsMax;
    }
    
    public void init(World world) {
        this.world = world;
        this.initProperty.set(true);
        this.region = new Cuboid(new Location(world, this.boundsMin.getBlockX(), this.boundsMin.getBlockY(), this.boundsMin.getBlockZ()), new Location(world, this.boundsMax.getBlockX(), this.boundsMax.getBlockY(), this.boundsMax.getBlockZ()));
        this.stopwatch = this.clockManager.createStopwatch(0, 0);
    }
    
    public Stopwatch getStopwatch() {
        return stopwatch;
    }
    
    public long getNextSpawn(ItemEntry entry) {
        for (ItemEntryHolder holder : this.holders) {
            if (holder.itemEntry.getId().equals(entry.getId())) {
                long nextRun = this.stopwatch.getNextRun(holder.callbackId);
                return nextRun - this.stopwatch.getTime();
            }
        }
        
        return 0;
    }
    
    public void start() {
        this.stopwatch.unpause();
    }
    
    public void stop() {
        this.stopwatch.pause();
    }
    
    public Cuboid getRegion() {
        return region;
    }
    
    public boolean contains(Location location) {
        if (location == null) {
            return false;
        }
        
        if (region == null) {
            return false;
        }
        
        return region.contains(location);
    }
    
    public boolean contains(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        return contains(entity.getLocation());
    }
    
    public Set<SpawnedItem> getSpawnedItems() {
        return new HashSet<>(spawnedItems);
    }
    
    public void addSpawnedItem(String entryId, Item item) {
        ItemEntry entry = getItemEntry(entryId);
        if (entry == null) {
            return;
        }
        
        SpawnedItem spawnedItem = new SpawnedItem(item, this, entry);
        this.spawnedItems.add(spawnedItem);
    }
    
    public void removedSpawnedItem(Item item) {
        this.spawnedItems.removeIf(spawnedItem -> spawnedItem.item().equals(item));
    }
    
    public int getSpawnedItemsCount(String entryId) {
        int count = 0;
        for (SpawnedItem spawnedItem : this.spawnedItems) {
            if (spawnedItem.entry().getId().equalsIgnoreCase(entryId)) {
                count++;
            }
        }
        
        return count;
    }
    
    public int getSpawnedItemsCount() {
        return this.spawnedItems.size();
    }
    
    public String getId() {
        return id;
    }
    
    public void addItemEntry(ItemEntry itemEntry) {
        this.itemEntries.add(itemEntry);
    }
    
    public List<ItemEntry> getItemEntries() {
        return new ArrayList<>(itemEntries);
    }
    
    public Position getBoundsMin() {
        return boundsMin;
    }
    
    public Position getBoundsMax() {
        return boundsMax;
    }
    
    public ClockManager getClockManager() {
        return clockManager;
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
    
    public ItemEntry getItemEntry(String entryId) {
        for (ItemEntry itemEntry : this.itemEntries) {
            if (itemEntry.getId().equalsIgnoreCase(entryId)) {
                return itemEntry;
            }
        }
        
        return null;
    }
    
    public boolean hasEntry(String entryId) {
        for (ItemEntry itemEntry : this.itemEntries) {
            if (itemEntry.getId().equalsIgnoreCase(entryId)) {
                return true;
            }
        }
        
        return false;
    }
}