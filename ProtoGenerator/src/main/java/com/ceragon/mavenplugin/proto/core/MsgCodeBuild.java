package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.MsgDesc;
import com.ceragon.mavenplugin.proto.bean.ProtoMsgInfo;
import com.ceragon.mavenplugin.proto.bean.config.PathType;
import com.ceragon.mavenplugin.proto.bean.config.ProtoAllMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEachClassBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEachMsgBuildConfig;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.StringUtils;
import lombok.AllArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MsgCodeBuild {
    MavenProject project;
    String resourceRoot;
    List<ProtoMsgInfo> allProtoMsgInfos;
    Map<String, Object> content;
    List<Exception> exceptions;


    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

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
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    public void processEachMsg(ProtoEachMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = buildTargetPath(config.getTargetFile(), config.getTargetPathType());
        this.allProtoMsgInfos.forEach(protoMsgInfo -> {
            // 检查是否匹配
            if (!config.getClassMatch().contains(protoMsgInfo.getClassName())) {
                return;
            }
            protoMsgInfo.getMsgIdAndNames().forEach((msgId, desc) -> {
                if (!desc.getName().endsWith(config.getMsgEndWith())) {
                    return;
                }
                content.put("name", desc.getName());
                content.put("fullName", desc.getFullName());
                try {
                    String destPathFinal = StringUtils.format(destPath, desc.getName());
                    CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPathFinal, false, content);
                } catch (IOException e) {
                    exceptions.add(e);
                }
            });

        });

    }


    public void processEachClass(ProtoEachClassBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = buildTargetPath(config.getTargetFile(), config.getTargetPathType());
        this.allProtoMsgInfos.forEach(protoMsgInfo -> {
            // 检查是否匹配
            if (!config.getClassMatch().contains(protoMsgInfo.getClassName())) {
                return;
            }
            content.put("nameList", protoMsgInfo.getMsgIdAndNames().values().stream()
                    .map(MsgDesc::getName)
                    .filter(name -> name.endsWith(config.getMsgEndWith()))
                    .collect(Collectors.toList()));
            try {
                CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
            } catch (IOException e) {
                exceptions.add(e);
            }
        });
    }
}
