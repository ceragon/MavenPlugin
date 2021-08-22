package com.ceragon.mavenplugin.rpc;

import com.ceragon.mavenplugin.util.ClassUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;


@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class RPCGenerator extends AbstractMojo {
    //    @Parameter(defaultValue = "${project}")
//    public MavenProject project;
//    @Parameter(defaultValue = "${repositorySystemSession}")
//    private RepositorySystemSession repoSession;
//    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
//    private ArtifactRepository localRepository;
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compilePath;
    @Parameter(property = "rpcDefine", required = true, readonly = true)
    private File tableSourceDir;
    private Log log;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        try {
            ClassLoader classLoader = getClassLoader();
            ClassUtil classUtil = new ClassUtil(log, classLoader);
            log.info("Generating");
            Class<?> annotation = classLoader.loadClass("com.ceragon.monkeygame.rpc.RpcDefine");
            log.info("annotation:" + annotation);
            Set<Class<?>> rpcList = classUtil.scan("com.ceragon.monkeygame", annotation);
            log.info("rpcList:" + rpcList);
        } catch (ClassNotFoundException e) {
            throw new MojoFailureException("load class failed", e);
        }
        log.info("Finishing");
    }

    private ClassLoader getClassLoader() {
        try {
            // 所有的类路径环境，也可以直接用 compilePath
//            List<String> classpathElements = project.getCompileClasspathElements();
//            classpathElements.add(project.getBuild().getOutputDirectory());
//            classpathElements.add(project.getBuild().getTestOutputDirectory());
            // 转为 URL 数组
            URL[] urls = new URL[compilePath.size()];
            for (int i = 0; i < compilePath.size(); ++i) {
                urls[i] = new File(compilePath.get(i)).toPath().toUri().toURL();
            }
            // 自定义类加载器
            return new URLClassLoader(urls, this.getClass().getClassLoader());
        } catch (Exception e) {
            getLog().debug("Couldn't get the classloader.");
            return this.getClass().getClassLoader();
        }
    }


}
