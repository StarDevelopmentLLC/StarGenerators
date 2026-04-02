package com.stardevllc.stargenerators.model;

import com.stardevllc.Cuboid;
import com.stardevllc.Position;
import com.stardevllc.starlib.objects.Nameable;
import com.stardevllc.starlib.objects.key.*;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.Collection;

public interface Generator<E extends GeneratorEntry> extends Nameable, Keyable {
    Position getBoundsMin();
    
    Position getBoundsMax();
    
    World getWorld();
    
    Cuboid getRegion();
    
    void init(World world);
    
    void start();
    
    void stop();
    
    E getEntry(Key key);
    
    Collection<E> getEntries();
    
    boolean hasEntry(Key key);
    
    @Override
    void setKey(Key key);
    
    @Override
    default boolean supportsSettingKey() {
        return true;
    }
    
    default boolean contains(Location location) {
        if (location == null) {
            return false;
        }
        
        Cuboid region = getRegion();
        if (region == null) {
            return false;
        }
        
        return region.contains(location);
    }
    
    default boolean contains(Entity entity) {
        if (entity == null) {
            return false;
        }
        
        return contains(entity.getLocation());
    }
}