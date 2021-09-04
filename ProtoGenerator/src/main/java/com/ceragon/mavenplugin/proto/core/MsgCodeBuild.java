package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoEveryProtoBuildConfig;
import com.ceragon.mavenplugin.proto.bean.config.ProtoTotalMsgBuildConfig;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoFileDescPojo;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoMessageDescPojo;
import com.ceragon.mavenplugin.proto.constant.ContextKey;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.PathFormat;
import com.ceragon.mavenplugin.util.VelocityUtil;
import lombok.Value;
import org.apache.maven.plugin.logging.Log;
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

    //region -------- 使用全部消息的集合生成代码 ------------
    public boolean buildTotalMsgCode(String resourceRoot, ProtoTotalMsgBuildConfig[] configList) {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        Map<String, Object> content = new HashMap<>();
        List<ProtoMessageDescPojo> messageDescPojoList = protoFileDescPojoList.stream().flatMap(pojo -> pojo.getMessageList().stream())
                .collect(Collectors.toList());
        content.put("totalMsgList", messageDescPojoList);
        content.put("totalMsgGroupList", protoFileDescPojoList);
        return Arrays.stream(configList).allMatch(config -> processTotalMsgCode(log, resourceRoot, config, content));
    }

    private boolean processTotalMsgCode(Log log, String resourceRoot, ProtoTotalMsgBuildConfig config, Map<String, Object> content) {
        String sourceName = config.getVmFile();
        String destPath = pathFormat.format(config.getTargetFile());
        try {
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, config.isOverwrite(), content);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
    //endregion

    //region -------- 使用every消息的集合生成代码 ------------
    public boolean buildEveryMsgCode(String resourceRoot, ProtoEveryMsgBuildConfig[] everyMsg) {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        return Arrays.stream(everyMsg).allMatch(config -> processEveryMsgCodeConfig(log, resourceRoot, config));
    }

    private boolean processEveryMsgCodeConfig(Log log, String resourceRoot, ProtoEveryMsgBuildConfig config) {
        return this.protoFileDescPojoList.stream()
                .filter(pojo -> config.getProtoNameMatch().stream().anyMatch(protoNameMatch -> pojo.getName().matches(protoNameMatch)))
                .allMatch(pojo -> processEveryMsgCodePojoAndConfig(log, resourceRoot, pojo, config));
    }

    private boolean processEveryMsgCodePojoAndConfig(Log log, String resourceRoot, ProtoFileDescPojo pojo, ProtoEveryMsgBuildConfig config) {
        return pojo.getMessageList().stream()
                .filter(protoMessagePojo -> config.getMsgNameMatch().stream().anyMatch(msgNameMatch -> protoMessagePojo.getName().matches(msgNameMatch)))
                .allMatch(protoMessagePojo -> processEveryMsgCode(log, resourceRoot, protoMessagePojo, config));
    }

    private boolean processEveryMsgCode(Log log, String resourceRoot, ProtoMessageDescPojo protoMessagePojo, ProtoEveryMsgBuildConfig config) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("msg", protoMessagePojo);
            content.put("util", VelocityUtil.getInstance());
            String destPath = pathFormat.format(config.getTargetFile(), "MsgName", protoMessagePojo.getName());
            CodeGenTool.createCodeByPath(resourceRoot, config.getVmFile(), destPath, config.isOverwrite(), content);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    //endregion

    public boolean buildEveryProtoCode(String resourceRoot, ProtoEveryProtoBuildConfig[] everyProto) {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        return Arrays.stream(everyProto).allMatch(config -> processEveryProtoCodeConfig(log, resourceRoot, config));
    }

    private boolean processEveryProtoCodeConfig(Log log, String resourceRoot, ProtoEveryProtoBuildConfig config) {
        return this.protoFileDescPojoList.stream()
                .filter(pojo -> config.getProtoNameMatch().stream().anyMatch(protoNameMatch -> pojo.getName().matches(protoNameMatch)))
                .allMatch(pojo -> processEveryProtoCode(log, resourceRoot, pojo, config));
    }

    private boolean processEveryProtoCode(Log log, String resourceRoot, ProtoFileDescPojo protoFileDescPojo, ProtoEveryProtoBuildConfig config) {
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("proto", protoFileDescPojo);
            content.put("util", VelocityUtil.getInstance());
            String destPath = pathFormat.format(config.getTargetFile());
            CodeGenTool.createCodeByPath(resourceRoot, config.getVmFile(), destPath, config.isOverwrite(), content);
            return true;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }


}
