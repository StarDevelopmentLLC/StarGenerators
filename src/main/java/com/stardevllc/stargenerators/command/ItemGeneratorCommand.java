package com.stardevllc.stargenerators.command;

import com.stardevllc.minecraft.command.*;
import com.stardevllc.stargenerators.StarGeneratorsPlugin;
import com.stardevllc.starlib.objects.key.Key;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class ItemGeneratorCommand extends StarCommand<StarGeneratorsPlugin> {
    
    private Map<UUID, Key> selectedGenerators = new HashMap<>();
    private Map<UUID, Selection> positionSelection = new HashMap<>();
    
    public ItemGeneratorCommand(StarGeneratorsPlugin plugin) {
        super(plugin, "itemgenerator", "Manage the Item Based Generators", "stargenerators.itemgenerator", "ig", "itemgen", "igen");
        this.subCommands.add(new PositionSubCommand(plugin, this, 1));
        this.subCommands.add(new PositionSubCommand(plugin, this, 2));
        
        //creating generators, templates
        //removing generators, templates
        //editing generators, templates
        //creating items
        //removing items
        //editing items
    }
    
    private class PositionSubCommand extends SubCommand<StarGeneratorsPlugin> {
        public PositionSubCommand(StarGeneratorsPlugin plugin, ICommand<StarGeneratorsPlugin> parent, int posN) {
            super(plugin, parent, 0, "pos" + posN, "Sets position " + posN, "stargenerators.itemgenerator.position");
            this.executor = (p, sender, label, args, flagResults) -> {
                Player player = (Player) sender;
                positionSelection.computeIfAbsent(player.getUniqueId(), u -> new Selection());
                Selection selection = positionSelection.get(player.getUniqueId());
                Location loc = player.getLocation().clone();
                if (posN == 1) {
                    selection.setPos1(loc);
                } else if (posN == 2) {
                    selection.setPos2(loc);
                } else {
                    return false;
                }
                colors.coloredLegacy(sender, "&eSet position &b" + posN + " &eto &b" + loc.getBlockX() + "&e, &b" + loc.getBlockY() + "&e, &b" + loc.getBlockZ() + "&e.");
                return true;
            };
        }
    }
    
    private static class Selection {
        private Location pos1, pos2;
        
        public Location getPos1() {
            return pos1;
        }
        
        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }
        
        public Location getPos2() {
            return pos2;
        }
        
        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }
    }
}