package com.stardevllc.stargenerators.model;

import com.stardevllc.starlib.objects.key.Keyable;

public interface GeneratorEntry extends Keyable {
    long getCooldown();
    
    void setCooldown(long cooldown);
}
