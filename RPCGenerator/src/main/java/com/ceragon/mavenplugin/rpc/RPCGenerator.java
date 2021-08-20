package com.ceragon.mavenplugin.rpc;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.util.List;

@Mojo(name = "generator")
public class RPCGenerator extends AbstractMojo {
    @Parameter(defaultValue = "${project}")
    public MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compilePath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
