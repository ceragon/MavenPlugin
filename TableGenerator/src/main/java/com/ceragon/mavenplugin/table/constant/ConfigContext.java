package com.ceragon.mavenplugin.table.constant;

import com.ceragon.mavenplugin.table.util.PropsUtil;
import lombok.Data;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Data
public class ConfigContext {
    private final static String LANGUAGE_NAME = "language.yml";
    private final Log log;
    private final File tableSourceDir;
    private final File outputDirectory;
    private final String langFilePath;
    private final String langPackageName;
    private final String langClassName;

    public ConfigContext(File tableSourceDir, File outputDirectory, Log log) throws IOException {
        this.tableSourceDir = tableSourceDir;
        this.outputDirectory = outputDirectory;
        this.log = log;
        this.langFilePath = this.tableSourceDir + File.separator + LANGUAGE_NAME;
        Properties properties = new Properties();
        properties.load(ConfigContext.class.getResourceAsStream("/generator.properties"));
        langPackageName = properties.getProperty("lang.package.name");
        langClassName = properties.getProperty("lang.class.name");
    }

}
