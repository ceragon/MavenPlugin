package com.ceragon.mavenplugin.util;

import org.apache.maven.project.MavenProject;

import java.util.Map;

public class PathFormat {
    private final Map<String, Object> basicVar;

    public PathFormat(MavenProject project) {
        String projectSrcDir = project.getBuild().getSourceDirectory();
        String projectBaseDir = project.getBasedir().getAbsolutePath();
        this.basicVar = Map.of("project.src.dir", projectSrcDir, "project.base.dir", projectBaseDir);
    }


    public String format(String sourcePath) {
        return format(sourcePath, null);
    }

    public String format(String sourcePath, String key, Object value) {
        return format(sourcePath, Map.of(key, value, key.toLowerCase(), value, key.toUpperCase(), value));
    }

    public String format(String sourcePath, Map<String, Object> params) {
        sourcePath = StringUtils.formatKV(sourcePath, basicVar);
        if (params == null) {
            return sourcePath;
        }
        return StringUtils.formatKV(sourcePath, params);
    }
}
