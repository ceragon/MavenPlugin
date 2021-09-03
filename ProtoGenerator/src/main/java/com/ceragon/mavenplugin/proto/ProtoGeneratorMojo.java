package com.ceragon.mavenplugin.proto;

import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import com.ceragon.mavenplugin.proto.bean.config.ProtoTotalMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryProtoBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoFileDescPojo;
import com.ceragon.mavenplugin.proto.constant.ContextKey;
import com.ceragon.mavenplugin.proto.core.DescriptorLoader;
import com.ceragon.mavenplugin.proto.core.MsgCodeBuild;
import com.ceragon.mavenplugin.proto.core.ProtocBuild;
import com.ceragon.mavenplugin.util.MavenBuildContext;
import com.ceragon.mavenplugin.util.PathFormat;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
public class ProtoGeneratorMojo extends AbstractMojo {

    @Parameter(property = "protocVersion", readonly = true)
    private String protocVersion;
    @Parameter(property = "inputDirectories", readonly = true)
    private File[] inputDirectories;
    @Parameter(property = "outputTargets", readonly = true)
    private OutputTarget[] outputTargets;

    @Parameter(property = "totalMsg", readonly = true)
    private ProtoTotalMsgBuildConfig[] totalMsg;
    @Parameter(property = "everyMsg", readonly = true)
    private ProtoEveryMsgBuildConfig[] everyMsg;
    @Parameter(property = "everyProto", readonly = true)
    private ProtoEveryProtoBuildConfig[] everyProto;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        MavenProject project = (MavenProject) getPluginContext().get("project");
        BuildContext context = new MavenBuildContext();
        context.setValue(ContextKey.LOG, log);
        context.setValue(ContextKey.PROJECT, project);
        ThreadBuildContext.setThreadBuildContext(context);
        prepareExecute();
        ProtocBuild protocBuild = ProtocBuild.builder().protocVersion(protocVersion)
                .inputDirectories(inputDirectories).includeStdTypes(true).includeImports(false)
                .build();
        List<OutputTarget> outputTargets = new ArrayList<>(Arrays.asList(this.outputTargets));
        OutputTarget descriptorTarget = new OutputTarget();
        descriptorTarget.setType("descriptor");
        outputTargets.add(descriptorTarget);
        // 生成目标proto格式，以及描述信息
        protocBuild.process(outputTargets);
        // 加载描述信息
        List<ProtoFileDescPojo> protoFileDescPojoList = DescriptorLoader.loadDesc(descriptorTarget.getOutputDirectory());

        MsgCodeBuild msgCodeBuild = new MsgCodeBuild(protoFileDescPojoList, new PathFormat(project));

        if (project.getBuild().getResources().isEmpty()) {
            log.error("the resources is empty");
            return;
        }
        String resourceRoot = project.getBuild().getResources().get(0).getDirectory();

        if (!msgCodeBuild.buildTotalMsgCode(resourceRoot, totalMsg)) {
            throw new MojoFailureException("build totalMsg code error");
        }
        if (!msgCodeBuild.buildEveryMsgCode(resourceRoot, everyMsg)) {
            throw new MojoFailureException("build everyMsg code error");
        }
        if (!msgCodeBuild.buildEveryProtoCode(resourceRoot, everyProto)) {
            throw new MojoFailureException("build everyProto code error");
        }
    }

    private void prepareExecute() {
        if ("Mac OS X".equals(System.getProperty("os.name"))
                && "aarch64".equals(System.getProperty("os.arch"))) {
            // 苹果m1电脑，改为x86_64 架构
            System.setProperty("os.arch", "x86_64");
        }
    }
}
