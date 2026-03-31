package com.stardevllc.stargenerators;

import com.stardevllc.stargenerators.model.*;
import com.stardevllc.starlib.clock.ClockManager;
import com.stardevllc.starlib.objects.key.Keys;
import com.stardevllc.starlib.registry.IRegistry;
import com.stardevllc.starlib.registry.Registries;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class StarGenerators {
    private StarGenerators() {
    }
    
    private static JavaPlugin plugin;
    
    @SuppressWarnings("rawtypes")
    public static final IRegistry<Generator> REGISTRY = Registries.create(Generator.class)
            .withSupplier(HashMap::new)
            .withId(Keys.of("stargenerators:generators"))
            .asGlobal()
            .build();
    
    public static final IRegistry<ItemGenerator> ITEM_GENERATORS = Registries.create(ItemGenerator.class)
            .withSupplier(HashMap::new)
            .withId(Keys.of("stargenerators:item_generators"))
            .withParent(REGISTRY)
            .asGlobal()
            .build();
    
    private static ClockManager clockManager;
    
    public static void init(JavaPlugin plugin) {
        if (StarGenerators.plugin != null) {
            plugin.getLogger().severe("StarGenerators has already been initialized by " + StarGenerators.plugin.getName());
            return;
        }
        
        StarGenerators.plugin = plugin;
        
        RegisteredServiceProvider<ClockManager> cmreg = Bukkit.getServicesManager().getRegistration(ClockManager.class);
        if (cmreg == null || cmreg.getProvider() == null) {
            clockManager = new ClockManager(50);
            Bukkit.getScheduler().runTaskTimer(plugin, clockManager.getRunnable(), 1L, 1L);
        } else {
            clockManager = cmreg.getProvider();
        }
        
        //TODO Load from files (After ItemBuilder saving and loading is properly implemented
    }
    
    public static ClockManager getClockManager() {
        return clockManager;
    }
    
    public static boolean handleItemPickup(LivingEntity entity, Item item, int remaining) {
        for (ItemGenerator entry : ITEM_GENERATORS.values()) {
            for (SpawnedItem spawnedItem : entry.getSpawnedItems()) {
                if (item.equals(spawnedItem.item())) {
                    spawnedItem.entry().handleItemPickup(entity, item, spawnedItem.entry(), entry);
                }
            }
            
            entry.removedSpawnedItem(item);
        }
        
        return false;
    }
}