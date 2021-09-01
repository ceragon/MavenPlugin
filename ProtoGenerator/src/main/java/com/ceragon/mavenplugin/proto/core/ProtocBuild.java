package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import com.ceragon.mavenplugin.proto.constant.ContextKey;
import com.ceragon.mavenplugin.proto.core.protoc.CommandUtil;
import com.ceragon.mavenplugin.proto.core.protoc.IncludeMavenType;
import com.ceragon.mavenplugin.proto.core.protoc.IncludeUtil;
import com.ceragon.mavenplugin.proto.core.protoc.OutputTargetUtil;
import com.ceragon.mavenplugin.util.StringUtils;
import com.github.os72.protocjar.ProtocVersion;
import lombok.Builder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Builder
public class ProtocBuild {
    private final static String extension = ".proto";
    private final static IncludeMavenType includeMavenType = IncludeMavenType.transitive;
    private static final String DEFAULT_INPUT_DIR = "/src/main/resources/".replace('/', File.separatorChar);

    String protocVersion;
    File[] inputDirectories;
    File[] includeDirectories;
    boolean includeStdTypes;
    boolean includeImports;

    public void process(List<OutputTarget> outputTargets) throws MojoExecutionException {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        final MavenProject project = (MavenProject) context.getValue(ContextKey.PROJECT);
        // 默认值
        if (StringUtils.isEmpty(protocVersion)) {
            protocVersion = ProtocVersion.PROTOC_VERSION.mVersion;
        }
        List<File> inputDirectoryList = new ArrayList<>();
        if (inputDirectories == null || inputDirectories.length == 0) {
            File inputDir = new File(project.getBasedir().getAbsolutePath() + DEFAULT_INPUT_DIR);
            inputDirectoryList.add(inputDir);
        } else {
            inputDirectoryList.addAll(Arrays.asList(inputDirectories));
        }

        context.setValue(ContextKey.PROTOC_VERSION, protocVersion);
        context.setValue(ContextKey.PROTO_EXTENSION, extension);
        context.setValue(ContextKey.INCLUDE_IMPORTS, includeImports);

        // 初始化outputTarget
        outputTargets.forEach(outputTarget -> OutputTargetUtil.initTarget(project, outputTarget));

        // 封装protoc的程序完整路径
        String protocCommand = CommandUtil.buildProtocExe(log, protocVersion);

        List<File> includeDirectoryList = IncludeUtil.buildIncludePath(includeStdTypes, includeMavenType, includeDirectories);

        for (OutputTarget target : outputTargets) {
            OutputTargetUtil.preprocessTarget(target);
        }

        for (OutputTarget target : outputTargets) {
            OutputTargetUtil.processTarget(protocCommand, inputDirectoryList, includeDirectoryList, target);
        }
    }


}
