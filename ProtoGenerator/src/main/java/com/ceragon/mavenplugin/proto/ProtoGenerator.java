package com.ceragon.mavenplugin.proto;

import com.ceragon.mavenplugin.proto.bean.ErrorMsg;
import com.ceragon.mavenplugin.proto.bean.MsgDesc;
import com.ceragon.mavenplugin.proto.bean.config.ProtoConfig;
import com.ceragon.mavenplugin.proto.bean.ProtoMsgInfo;
import com.ceragon.mavenplugin.proto.core.MsgCodeBuild;
import com.ceragon.mavenplugin.proto.core.MsgInfoLoad;
import com.ceragon.mavenplugin.util.ClassUtil;
import org.yaml.snakeyaml.Yaml;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;

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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.COMPILE)
public class ProtoGenerator extends AbstractMojo {
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    public List<String> compilePath;
    @Parameter(property = "protoPackage", required = true, readonly = true)
    public String protoPackage;
    @Parameter(property = "msgIdField", defaultValue = "msgId", readonly = true)
    public String msgIdField;
    @Parameter(property = "protoConfigPath", defaultValue = "protoConfig.yml", readonly = true)
    public String protoConfigPath;

    private Log log;
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        this.project = (MavenProject) getPluginContext().get("project");
        try {
            ClassUtil classUtil = new ClassUtil(log, compilePath);
            Set<Class<?>> protoClasses = classUtil.scan(protoPackage, null);
            List<ProtoMsgInfo> allProtoMsgInfos = new ArrayList<>();
            // 加载配置
            ProtoConfig protoConfig = loadProtoConfig();
            if (protoConfig == null) {
                throw new MojoFailureException("can't find the protoConfig!Please create the protoConfig.yml in resource dir or set the protoConfigPath in pom.xml");
            }
            // 封装所有的消息
            buildAllProtoMsgInfos(protoClasses, allProtoMsgInfos);
            // 检查消息列表是否合法
            List<ErrorMsg> errorMsgList = checkProtoMsg(allProtoMsgInfos);
            if (!errorMsgList.isEmpty()) {
                log.error("find repeat msg,please read follow list!");
                errorMsgList.forEach(errorMsg -> log.error(errorMsg.toString()));
                throw new MojoFailureException("find repeat msg!");
            }
            // 数据生成
            if (!buildProtoCode(protoConfig, allProtoMsgInfos)) {
                throw new MojoFailureException("build proto code failed");
            }
        } catch (MalformedURLException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private boolean buildProtoCode(ProtoConfig protoConfig, List<ProtoMsgInfo> allProtoMsgInfos) {

        String resourceRoot = this.project.getBuild().getOutputDirectory();
        List<Exception> exceptions = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        MsgCodeBuild build = new MsgCodeBuild(project, resourceRoot, allProtoMsgInfos, content, exceptions);
        content.put("infoList", allProtoMsgInfos);
        protoConfig.getAllMsg().forEach(build::processAllMsg);
        content.clear();
        protoConfig.getEachMsg().forEach(build::processEachMsg);
        content.clear();
        protoConfig.getEachClass().forEach(build::processEachClass);
        if (exceptions.isEmpty()) {
            return true;
        }
        for (Exception exception : exceptions) {
            log.error(exception.getMessage(), exception);
        }
        return false;
    }

    private ProtoConfig loadProtoConfig() {
        String sourceRoot = project.getBuild().getOutputDirectory();
        File sourceFile = new File(sourceRoot + File.separator + protoConfigPath);
        if (!sourceFile.exists()) {
            return null;
        }
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileReader(sourceFile), ProtoConfig.class);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    private List<ErrorMsg> checkProtoMsg(List<ProtoMsgInfo> allProtoMsgInfos) {
        Set<Integer> msgIdSet = new HashSet<>();
        List<ErrorMsg> errorMsgList = new ArrayList<>();
        allProtoMsgInfos.stream().sorted(Comparator.comparingInt(ProtoMsgInfo::getMaxMsgId))
                .forEach(info -> info.getMsgIdAndNames().entrySet().stream()
                        .filter(entry -> !msgIdSet.add(entry.getKey()))
                        .forEach(entry -> errorMsgList.add(ErrorMsg.builder()
                                .className(info.getClassName())
                                .msgId(entry.getKey())
                                .msgName(entry.getValue().getName())
                                .build())));
        return errorMsgList;
    }

    private void buildAllProtoMsgInfos(Set<Class<?>> protoClasses, List<ProtoMsgInfo> allProtoMsgInfos) throws InvocationTargetException, IllegalAccessException {
        for (Class<?> protoClass : protoClasses) {
            ProtoMsgInfo.ProtoMsgInfoBuilder builder = ProtoMsgInfo.builder();
            AtomicInteger maxMsgId = new AtomicInteger();
            new MsgInfoLoad(msgIdField).forEachMsg(protoClass, (msgId, msgDesc) -> {
                builder.msgIdAndName(msgId, msgDesc);
                if (msgId > maxMsgId.get()) {
                    maxMsgId.set(msgId);
                }
            });
            if (maxMsgId.get() == 0) {
                continue;
            }
            allProtoMsgInfos.add(builder.className(protoClass.getSimpleName()).maxMsgId(maxMsgId.get()).build());
        }
    }


}
