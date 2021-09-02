package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.config.ProtoAllMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEachMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoFileDescPojo;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoMessageDescPojo;
import com.ceragon.mavenplugin.proto.constant.ContextKey;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.PathFormat;
import lombok.Value;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Value
public class MsgCodeBuild {
    List<ProtoFileDescPojo> protoFileDescPojoList;
    PathFormat pathFormat;

    public boolean buildTotalMsgCode(ProtoAllMsgBuildConfig[] configList) {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        MavenProject project = (MavenProject) context.getValue(ContextKey.PROJECT);
        String resourceRoot = project.getBuild().getResources().get(0).getDirectory();
        Map<String, Object> content = new HashMap<>();
        List<ProtoMessageDescPojo> messageDescPojoList = protoFileDescPojoList.stream().flatMap(pojo -> pojo.getMessageList().stream())
                .collect(Collectors.toList());
        content.put("totalMsgList", messageDescPojoList);
        content.put("totalMsgGroupList", protoFileDescPojoList);
        return Arrays.stream(configList).allMatch(config -> buildTotalMsgCodeOne(log, resourceRoot, config, content));
    }

    private boolean buildTotalMsgCodeOne(Log log, String resourceRoot, ProtoAllMsgBuildConfig config, Map<String, Object> content) {
        String sourceName = config.getVmFile();
        String destPath = pathFormat.format(config.getTargetFile());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


    public boolean buildEveryMsgCode(ProtoEachMsgBuildConfig[] everyMsg) {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        MavenProject project = (MavenProject) context.getValue(ContextKey.PROJECT);
        String resourceRoot = project.getBuild().getResources().get(0).getDirectory();
        Map<String, Object> content = new HashMap<>();

        Arrays.stream(everyMsg).allMatch(config ->);

        List<ProtoMessageDescPojo> messageDescPojoList = protoFileDescPojoList.stream().flatMap(pojo -> pojo.getMessageList().stream())
                .collect(Collectors.toList());
        content.put("totalMsgList", messageDescPojoList);
        content.put("totalMsgGroupList", protoFileDescPojoList);
        return false;
    }

    private boolean buildEveryMsgCodeOne(Log log, String resourceRoot, ProtoEachMsgBuildConfig config) {
        String sourceName = config.getVmFile();
        String destPath = pathFormat.format(config.getTargetFile());
        return this.protoFileDescPojoList.stream()
                .filter(pojo -> config.getMsgEndWith().matches(pojo.getName()))
                .allMatch(pojo -> buildEveryMsgCodeOne0(log, resourceRoot, config));
    }

    private boolean buildEveryMsgCodeOne0(Log log, String resourceRoot, ProtoEachMsgBuildConfig config) {
        try {
            String destPath = pathFormat.format(config.getTargetFile(),"MsgName", );
            CodeGenTool.createCodeByPath(resourceRoot, config.getVmFile(), destPath, true, content);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

}
