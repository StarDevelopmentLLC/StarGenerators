package com.stardevllc.stargenerators;

import com.stardevllc.plugin.ExtendedJavaPlugin;
import com.stardevllc.stargenerators.command.ItemGeneratorCommandOld;
import com.stardevllc.stargenerators.listener.GeneratorListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class StarGeneratorsPlugin extends ExtendedJavaPlugin implements Listener {
    
    public void onEnable() {
        super.onEnable();
        
        StarGenerators.init(this);
        
        registerCommand("itemgenerator", new ItemGeneratorCommandOld(this));
        registerListeners(this, new GeneratorListener());
    }
    
    @EventHandler 
    public void onItemPickup(PlayerPickupItemEvent e) {
        StarGenerators.handleItemPickup(e.getPlayer(), e.getItem(), e.getRemaining());
    }
}