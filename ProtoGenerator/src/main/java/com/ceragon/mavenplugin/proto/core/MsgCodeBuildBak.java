package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.MsgDesc;
import com.ceragon.mavenplugin.proto.bean.ProtoMsgInfo;
import com.ceragon.mavenplugin.proto.bean.config.ProtoTotalMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryProtoBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryMsgBuildConfig;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.PathFormat;

import org.apache.maven.project.MavenProject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class MsgCodeBuildBak {
    MavenProject project;
    String resourceRoot;
    List<ProtoMsgInfo> allProtoMsgInfos;
    Map<String, Object> content;
    List<Exception> exceptions;
    PathFormat pathFormat;

    public MsgCodeBuildBak(MavenProject project, String resourceRoot, List<ProtoMsgInfo> allProtoMsgInfos, Map<String, Object> content, List<Exception> exceptions) {
        this.project = project;
        this.resourceRoot = resourceRoot;
        this.allProtoMsgInfos = allProtoMsgInfos;
        this.content = content;
        this.exceptions = exceptions;
        this.pathFormat = new PathFormat(project);
    }

    public void setContent(Map<String, Object> content) {
        this.content = content;
    }

    public void processAllMsg(ProtoTotalMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = pathFormat.format(config.getTargetFile());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    public void processEachMsg(ProtoEveryMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        this.allProtoMsgInfos.forEach(protoMsgInfo -> {
            // 检查是否匹配
            if (!config.getProtoNameMatch().contains(protoMsgInfo.getClassName())) {
                return;
            }
            protoMsgInfo.getMsgIdAndNames().forEach((msgId, desc) -> {
//                if (!desc.getName().endsWith(config.getMsgNameMatch())) {
//                    return;
//                }
                content.put("name", desc.getName());
                content.put("fullName", desc.getFullName());
                try {
                    String destPath = pathFormat.format(config.getTargetFile(), "MsgName", desc.getName());
                    CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, false, content);
                } catch (IOException e) {
                    exceptions.add(e);
                }
            });

        });

    }


//    public void processEachClass(ProtoEveryProtoBuildConfig config) {
//        String sourceName = config.getVmFile();
//        String destPath = pathFormat.format(config.getTargetFile());
//        this.allProtoMsgInfos.forEach(protoMsgInfo -> {
//            // 检查是否匹配
//            if (!config.getClassMatch().contains(protoMsgInfo.getClassName())) {
//                return;
//            }
//            content.put("nameList", protoMsgInfo.getMsgIdAndNames().values().stream()
//                    .map(MsgDesc::getName)
//                    .filter(name -> name.endsWith(config.getMsgEndWith()))
//                    .collect(Collectors.toList()));
//            try {
//                CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
//            } catch (IOException e) {
//                exceptions.add(e);
//            }
//        });
//    }
}
