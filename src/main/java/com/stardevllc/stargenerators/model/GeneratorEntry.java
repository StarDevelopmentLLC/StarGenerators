package com.stardevllc.stargenerators.model;

import com.stardevllc.starlib.objects.key.Key;
import com.stardevllc.starlib.objects.key.Keyable;

public interface GeneratorEntry extends Keyable {
    @Override
    void setKey(Key key);
    
    @Override
    default boolean supportsSettingKey() {
        return true;
    }
}
