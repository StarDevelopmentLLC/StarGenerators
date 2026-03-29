package com.stardevllc.staritemgenerators;

import com.stardevllc.staritemgenerators.model.*;
import com.stardevllc.starlib.clock.ClockManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class StarItemGenerators {
    private StarItemGenerators() {
    }
    
    private static JavaPlugin plugin;
    
    private static GeneratorRegistry generatorRegistry;
    
    public static void init(JavaPlugin plugin) {
        if (StarItemGenerators.plugin != null) {
            plugin.getLogger().severe("StarItemGenerators has already been initialized by " + StarItemGenerators.plugin.getName());
            return;
        }
        
        StarItemGenerators.plugin = plugin;
        
        ClockManager clockManager;
        
        RegisteredServiceProvider<ClockManager> cmreg = Bukkit.getServicesManager().getRegistration(ClockManager.class);
        if (cmreg == null || cmreg.getProvider() == null) {
            clockManager = new ClockManager(50);
            Bukkit.getScheduler().runTaskTimer(plugin, clockManager.getRunnable(), 1L, 1L);
        } else {
            clockManager = cmreg.getProvider();
        }
        
        StarItemGenerators.generatorRegistry = new GeneratorRegistry(clockManager);
        Bukkit.getServer().getServicesManager().register(GeneratorRegistry.class, generatorRegistry, plugin, ServicePriority.Normal);
        
        //TODO Load from files (After ItemBuilder saving and loading is properly implemented
    }
    
    public static boolean handleItemPickup(LivingEntity entity, Item item, int remaining) {
        for (ItemGenerator entry : generatorRegistry.values()) {
            for (SpawnedItem spawnedItem : entry.getSpawnedItems()) {
                if (item.equals(spawnedItem.item())) {
                    spawnedItem.entry().handleItemPickup(entity, item, spawnedItem.entry(), entry);
                }
            }
            
            entry.removedSpawnedItem(item);
        }
        
        return false;
    }
    
    public static GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }
}