package com.ceragon.mavenplugin.table;

import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.bean.config.TableConfig;
import com.ceragon.mavenplugin.table.build.KeyDataMapBuild;
import com.ceragon.mavenplugin.table.constant.ConfigContext;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.ceragon.mavenplugin.table.load.LoadTableData;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mojo(
        name = "generator",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true
)
public class TableGeneratorMojo extends AbstractMojo {
    @Parameter(property = "loadScriptClass", readonly = true)
    String loadScriptClass;
    @Parameter(property = "loadScriptFile", readonly = true)
    File loadScriptFile;
    @Parameter(property = "tableSourceRoot", defaultValue = "${project.basedir}/src/main/resources", readonly = true)
    String tableSourceDir;
    @Parameter(property = "tableConfigFile", defaultValue = "tableConfig.yml", readonly = true)
    File tableConfigFile;
    private Log log;
    public static ThreadLocal<List<Exception>> exceptions = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.log = getLog();
        MavenProject project = (MavenProject) getPluginContext().get("project");
        log.info("Generating");
        TableConfig tableConfig = loadTableConfig();
        if (tableConfig == null) {
            throw new MojoFailureException("can't find the tableConfig!Please create the tableConfig.yml in resource dir or set the tableConfigPath in pom.xml");
        }
        ConfigContext context = ConfigContext.builder()
                .log(log)
                .project(project)
                .loadScriptClass(loadScriptClass)
                .loadScriptFile(loadScriptFile)
                .tableSourceDir(tableSourceDir)
                .build();
        List<TableData> allTableDatas;
        try {
            allTableDatas = new LoadTableData(context).loadTableData();
        } catch (Throwable e) {
            throw new MojoFailureException("load table failed", e);
        }

        KeyDataMapBuild keyDataMapBuild = new KeyDataMapBuild(project, allTableDatas);
        tableConfig.getEachTableKeyDataMap().forEach(keyDataMapBuild::processAllTable);
    }


    private TableConfig loadTableConfig() {
        if (!this.tableConfigFile.exists()) {
            log.error("the config file is not exist!");
            return null;
        }
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileReader(this.tableConfigFile), TableConfig.class);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
