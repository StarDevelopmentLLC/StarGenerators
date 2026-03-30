package com.stardevllc.stargenerators.model;

import com.stardevllc.starlib.clock.ClockManager;
import com.stardevllc.starlib.injector.FieldInjector;
import com.stardevllc.starlib.injector.SimpleFieldInjector;
import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.objects.key.Keys;
import com.stardevllc.starlib.registry.*;

import java.util.HashMap;
import java.util.Set;

public class GeneratorRegistry extends AbstractRegistry<ItemGenerator> {
    
    private final ClockManager clockManager;
    
    private final FieldInjector injector;
    
    public GeneratorRegistry(ClockManager clockManager) {
        super(ItemGenerator.class, Keys.of("itemgenerators"), "Item Generators", new HashMap<>(), null, false, null, Set.of());
        this.clockManager = clockManager;
        
        this.injector = new SimpleFieldInjector();
        this.injector.set(this);
        this.injector.set(clockManager);
    }
    
    @Override
    protected void callAdditionalRegisterActions(Key key, ItemGenerator value, ItemGenerator oldValue) {
        injector.inject(value);
    }
    
    public ClockManager getClockManager() {
        return clockManager;
    }
}