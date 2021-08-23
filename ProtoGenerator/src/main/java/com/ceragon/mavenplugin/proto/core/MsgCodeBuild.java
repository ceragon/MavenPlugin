package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.config.PathType;
import com.ceragon.mavenplugin.proto.bean.config.ProtoAllMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEachMsgBuildConfig;
import com.ceragon.mavenplugin.util.CodeGenTool;
import lombok.AllArgsConstructor;
import lombok.Value;

import org.apache.maven.project.MavenProject;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Value
@AllArgsConstructor
public class MsgCodeBuild {
    MavenProject project;
    String resourceRoot;
    Map<String, Object> content;
    List<Exception> exceptions;

    private String buildTargetPath(String destPath, PathType pathType) {
//        String destPath = vmInfo.getTargetFile();
        switch (pathType) {
        case src: {
            String baseSrcDir = this.project.getBuild().getSourceDirectory();
            return baseSrcDir + File.separator + destPath;
        }
        case projectPath: {
            String projectBaseDir = this.project.getBasedir().getAbsolutePath();
            return projectBaseDir + File.separator + destPath;
        }
        default: {
            return destPath;
        }
        }
    }

    public void processAllMsg(ProtoAllMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = buildTargetPath(config.getTargetFile(), config.getTargetPathType());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath,true, content);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    public void processEachMsg(ProtoEachMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = buildTargetPath(config.getTargetFile(), config.getTargetPathType());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, false, content);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }
}
