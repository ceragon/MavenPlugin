package com.ceragon.mavenplugin.table;

import com.ceragon.mavenplugin.table.bean.TableConfig;
import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.table.language.LanguageService;
import lombok.extern.slf4j.Slf4j;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Function;

@Mojo(
        name = "generator",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE,
        threadSafe = true
)
public class TableGeneratorMojo extends AbstractMojo {

    @Parameter(property = "tableSourceRoot", defaultValue = "${project.basedir}/src/main/resources", readonly = true)
    private File tableSourceDir;

    @Parameter(property = "outputDirectory", required = true, readonly = true)
    private File outputDirectory;

    @Parameter(property = "tableConfigPath", defaultValue = "tableConfig.yml", readonly = true)
    public String tableConfigPath;
    private Log log;
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.log = getLog();
        this.project = (MavenProject) getPluginContext().get("project");
        log.info("Generating");
        TableConfig tableConfig = loadTableConfig();
        if (tableConfig == null) {
            throw new MojoFailureException("can't find the tableConfig!Please create the tableConfig.yml in resource dir or set the tableConfigPath in pom.xml");
        }



        //配置velocity的资源加载路径
        ConfigContext context;
        try {
            context = new ConfigContext(tableSourceDir, outputDirectory, log);
        } catch (Throwable e) {
            throw new MojoFailureException("init config failed", e);
        }
        log.info("config init ok!");
        generate(context.getLangFilePath(),
                (yamlData) -> LanguageService.getInstance().generate(yamlData, context));
        log.info("generate lang file ok!");
        log.info("generate finish!");
    }

    private TableConfig loadTableConfig() {
        String sourceRoot = project.getBuild().getOutputDirectory();
        File sourceFile = new File(sourceRoot + File.separator + tableConfigPath);
        if (!sourceFile.exists()) {
            return null;
        }
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileReader(sourceFile), TableConfig.class);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private void generate(String path, Function<Object, Boolean> consumer) throws MojoFailureException {
        File file = new File(path);
        if (file.exists()) {
            Yaml yaml = new Yaml();
            try (FileInputStream fis = new FileInputStream(file)) {
                Object yamlData = yaml.load(fis);
                if (!consumer.apply(yamlData)) {
                    throw new MojoFailureException(String.format("generate file %s failed", path));
                }
            } catch (FileNotFoundException e) {
                throw new MojoFailureException("the language file is not found!", e);
            } catch (IOException e) {
                throw new MojoFailureException("the language file is can't read!", e);
            }
        }
    }

}
