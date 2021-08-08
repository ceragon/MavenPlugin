package com.ceragon.mavenplugin.table;

import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.table.language.LanguageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Function;

@Mojo(name = "generator")
public class TableGeneratorMojo extends AbstractMojo {

    @Parameter(property = "tableSourceRoot", required = true, readonly = true)
    private File tableSourceDir;

    @Parameter(property = "outputDirectory", required = true, readonly = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        log.info("Generating");
        //配置velocity的资源加载路径
        ConfigContext context;
        try {
            context = new ConfigContext(tableSourceDir, outputDirectory,log);
        } catch (Throwable e) {
            throw new MojoFailureException("init config failed", e);
        }
        log.info("config init ok!");
        generate(context.getLangFilePath(),
                (yamlData) -> LanguageService.getInstance().generate(yamlData, context));
        log.info("generate lang file ok!");
        log.info("generate finish!");
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

    public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
        TableGeneratorMojo mojo = new TableGeneratorMojo();
        mojo.tableSourceDir = new File("/Users/ceragon/IdeaProjects/gitee/MonkeyGame/TableModule/src/main/resources/model");
        mojo.outputDirectory = new File("/Users/ceragon/IdeaProjects/gitee/MonkeyGame/TableModule/src/main/java");
        mojo.execute();
    }
}
