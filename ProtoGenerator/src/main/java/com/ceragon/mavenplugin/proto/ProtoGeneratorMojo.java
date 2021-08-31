package com.ceragon.mavenplugin.proto;

import com.ceragon.mavenplugin.proto.bean.ErrorMsg;
import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import com.ceragon.mavenplugin.proto.bean.ProtoMsgInfo;
import com.ceragon.mavenplugin.proto.bean.config.ProtoConfig;
import com.ceragon.mavenplugin.proto.core.MsgCodeBuild;
import com.ceragon.mavenplugin.proto.core.MsgInfoLoad;
import com.ceragon.mavenplugin.proto.core.ProtocBuild;
import com.ceragon.mavenplugin.util.ClassUtil;
import com.ceragon.mavenplugin.util.StringUtils;
import com.github.os72.protocjar.Protoc;
import com.github.os72.protocjar.ProtocVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;
import org.yaml.snakeyaml.Yaml;

import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.GeneratedMessage;

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
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.COMPILE)
public class ProtoGeneratorMojo extends AbstractMojo {
//    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
//    public List<String> compilePath;

    @Parameter(property = "protocVersion", readonly = true)
    private String protocVersion;
    @Parameter(property = "inputDirectories", readonly = true)
    private File[] inputDirectories;
    @Parameter(property = "outputTargets", readonly = true)
    private OutputTarget[] outputTargets;

    @Parameter(property = "protoPackage", required = true, readonly = true)
    public String protoPackage;
    @Parameter(property = "msgIdClass", required = true, readonly = true)
    public String msgIdClass;
    @Parameter(property = "msgIdField", defaultValue = "msgId", readonly = true)
    public String msgIdField;

    @Parameter(property = "protoConfigPath", defaultValue = "protoConfig.yml", readonly = true)
    public String protoConfigPath;
    private BuildContext context;
    private Log log;
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        this.log = getLog();
        this.project = (MavenProject) getPluginContext().get("project");
        this.context = ThreadBuildContext.getContext();
        prepareExecute();
        ProtocBuild protocBuild = ProtocBuild.builder().log(log).project(project).protocVersion(protocVersion)
                .inputDirectories(inputDirectories).includeStdTypes(true).includeImports(true)
                .buildContext(context).build();
        if (outputTargets != null) {
            protocBuild.process(outputTargets);
        }

        List<String> compilePath = null;
        try {
            compilePath = project.getCompileClasspathElements();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoFailureException("the compile path is wrong!" + e.getMessage(), e);
        }
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
            buildAllProtoMsgInfos(classUtil, protoClasses, allProtoMsgInfos);
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
        } catch (MalformedURLException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    private void prepareExecute() {
        if ("Mac OS X".equals(System.getProperty("os.name"))
                && "aarch64".equals(System.getProperty("os.arch"))) {
            // 苹果m1电脑，改为x86_64 架构
            System.setProperty("os.arch", "x86_64");
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

    private void buildAllProtoMsgInfos(ClassUtil classUtil, Set<Class<?>> protoClasses, List<ProtoMsgInfo> allProtoMsgInfos) throws InvocationTargetException, IllegalAccessException, ClassNotFoundException {
        Class<?> msgIdClass = classUtil.loadClass(this.msgIdClass);
        // 类必须包含msgIdField这个字段
        Optional<Field> optional = Arrays.stream(msgIdClass.getDeclaredFields())
                .filter(f -> f.getName().equals(msgIdField)).findFirst();
        if (optional.isEmpty()) {
            return;
        }
        Field field = optional.get();
        GeneratedMessage.GeneratedExtension<DescriptorProtos.MessageOptions, Integer> msgIdExtension =
                (GeneratedMessage.GeneratedExtension<DescriptorProtos.MessageOptions, Integer>) field.get(msgIdClass);

        MsgInfoLoad load = new MsgInfoLoad(msgIdField, msgIdExtension);

        for (Class<?> protoClass : protoClasses) {
            ProtoMsgInfo.ProtoMsgInfoBuilder builder = ProtoMsgInfo.builder();
            AtomicInteger maxMsgId = new AtomicInteger();
            load.forEachMsg(protoClass, (msgId, msgDesc) -> {
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
