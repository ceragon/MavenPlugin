package com.ceragon.mavenplugin.table.bean;

import lombok.Builder;
import lombok.Value;
import org.apache.maven.plugin.logging.Log;

@Value
@Builder
public class Environment {
    public final static String ENV = "env";
    Log log;
    String sourceDir;
}
