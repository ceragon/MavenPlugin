package com.ceragon.mavenplugin.util;

import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import java.util.HashMap;
import java.util.Map;

public class MavenBuildContext extends DefaultBuildContext {
    private final Map<String, Object> contextMap = new HashMap<>();

    @Override
    public Object getValue(String key) {
        return contextMap.get(key);
    }

    @Override
    public void setValue(String key, Object value) {
        this.contextMap.put(key, value);
    }
}
