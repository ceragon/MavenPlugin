package com.ceragon.mavenplugin.rpc;

import com.ceragon.mavenplugin.util.ClassUtil;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Mojo(name = "generator")
/**
 * @phase package
 * @requiresDependencyResolution compile
 */
public class RPCGenerator extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    public MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compilePath;
    private Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        MavenProject currentProject = project;
        for (int i = 0; i < 10; i++) {
            if (currentProject == null){
                break;
            }
            log.info("the project is :"+currentProject);
            currentProject = currentProject.getParent();
        }
        ClassLoader classLoader = getClassLoader(this.project);
        ClassUtil classUtil = new ClassUtil(log, classLoader);
        log.info("Generating");
        try {
            Class<?> annotation = classLoader.loadClass("com.ceragon.monkeygame.rpc.RpcDefine");
            log.info("annotation:" + annotation);
            log.info("test:" + classLoader.loadClass("com.ceragon.monkeygame.eventbus.EventChannelInactive"));
            Set<Class<?>> rpcList = classUtil.scan("com.ceragon.monkeygame", annotation);
            log.info("rpcList:" + rpcList);
        } catch (ClassNotFoundException e) {
            throw new MojoFailureException("load class failed", e);
        }
//        classUtil.scan("com.ceragon.monkeygame",)
//        try {
//            Class<?> type = getClassLoader(this.project).loadClass("com.ceragon.monkeygame.eventbus.EventChannelInactive");
//            getLog().info("Generating:" + type);
//        } catch (ClassNotFoundException e) {
//            throw new MojoFailureException("load class failed", e);
//        }
        log.info("Finishing");
    }

    private void addArtifactClasspath(MavenProject project, List<String> classpathElements) {
//        classpathElements.addAll(project.getArtifacts().stream().
//                map(artifact -> artifact.getFile().getAbsolutePath())
//                .collect(Collectors.toList()));
//        classpathElements.add(project.getArtifact().getFile().getAbsolutePath());
        log.info("Adding classpath " + project.getArtifact().getClass());
        MavenProject parent = project.getParent();
        if (parent == null) {
            return;
        }
        addArtifactClasspath(parent, classpathElements);
    }

    private ClassLoader getClassLoader(MavenProject project) {
        try {
            // 所有的类路径环境，也可以直接用 compilePath
            List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());
            classpathElements.add(project.getBuild().getTestOutputDirectory());
            addArtifactClasspath(project, classpathElements);
            // 转为 URL 数组
            URL[] urls = new URL[classpathElements.size()];
            for (int i = 0; i < classpathElements.size(); ++i) {
                urls[i] = new File(classpathElements.get(i)).toURL();
                log.info("the url is " + urls[i]);
            }

            // 自定义类加载器
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (Exception e) {
            getLog().debug("Couldn't get the classloader.");
            return this.getClass().getClassLoader();
        }
    }


}
