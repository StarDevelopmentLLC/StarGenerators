package com.stardevllc.staritemgenerators;

import com.stardevllc.plugin.ExtendedJavaPlugin;
import com.stardevllc.staritemgenerators.command.ItemGeneratorCommand;
import com.stardevllc.staritemgenerators.listener.GeneratorListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class StarItemGeneratorsPlugin extends ExtendedJavaPlugin implements Listener {
    
    public void onEnable() {
        super.onEnable();
        
        StarItemGenerators.init(this);
        
        registerCommand("itemgenerator", new ItemGeneratorCommand(this, StarItemGenerators.getGeneratorRegistry()));
        registerListeners(this, new GeneratorListener(StarItemGenerators.getGeneratorRegistry()));
    }
    
    @EventHandler 
    public void onItemPickup(PlayerPickupItemEvent e) {
        StarItemGenerators.handleItemPickup(e.getPlayer(), e.getItem(), e.getRemaining());
    }
}