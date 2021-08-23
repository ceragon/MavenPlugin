package com.ceragon.mavenplugin.proto;

import com.ceragon.mavenplugin.proto.bean.ErrorMsg;
import com.ceragon.mavenplugin.proto.bean.ProtoMsgInfo;
import com.ceragon.mavenplugin.util.ClassUtil;
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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        try {
            ClassUtil classUtil = new ClassUtil(log, compilePath);
            Set<Class<?>> protoClasses = classUtil.scan(protoPackage, null);
            List<ProtoMsgInfo> allProtoMsgInfos = new ArrayList<>();
            // 封装所有的消息
            buildAllProtoMsgInfos(protoClasses, allProtoMsgInfos);
            // 检查消息列表是否合法
            List<ErrorMsg> errorMsgList = checkProtoMsg(allProtoMsgInfos);
            if (!errorMsgList.isEmpty()) {
                log.error("find repeat msg,please read follow list!");
                errorMsgList.forEach(errorMsg -> log.error(errorMsg.toString()));
                throw new MojoFailureException("find repeat msg!");
            }

        } catch (MalformedURLException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
            throw new MojoFailureException(e.getMessage(), e);
        }
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
                                .msgName(entry.getValue())
                                .build())));
        return errorMsgList;
    }

    private void buildAllProtoMsgInfos(Set<Class<?>> protoClasses, List<ProtoMsgInfo> allProtoMsgInfos) throws InvocationTargetException, IllegalAccessException {
        for (Class<?> protoClass : protoClasses) {
            ProtoMsgInfo.ProtoMsgInfoBuilder builder = ProtoMsgInfo.builder();
            AtomicInteger maxMsgId = new AtomicInteger();
            forEachMsg(protoClass, (msgId, msgName) -> {
                builder.msgIdAndName(msgId, msgName);
                if (msgId > maxMsgId.get()) {
                    maxMsgId.set(msgId);
                }
            });
            allProtoMsgInfos.add(builder.className(protoClass.getSimpleName()).maxMsgId(maxMsgId.get()).build());
        }
    }

    /**
     * 遍历类中的每一个消息
     *
     * @param protoClass
     * @param consumer   处理遍历到的消息Id和内容
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void forEachMsg(Class<?> protoClass, BiConsumer<Integer, String> consumer) throws IllegalAccessException, InvocationTargetException {
        // 过滤一部分类
        if (protoClass.getSuperclass() != Object.class) {
            return;
        }
        // 类必须包含msgIdField这个字段
        Optional<Field> optional = Arrays.stream(protoClass.getDeclaredFields())
                .filter(f -> f.getName().equals(msgIdField)).findFirst();
        if (optional.isEmpty()) {
            return;
        }
        // 必须包含 getDescriptor 这个方法
        Optional<Method> methodOptional = Arrays.stream(protoClass.getDeclaredMethods())
                .filter(f -> f.getName().equals("getDescriptor")).findFirst();
        if (methodOptional.isEmpty()) {
            return;
        }
        Field field = optional.get();
        GeneratedExtension<MessageOptions, Integer> msgIdExtension = (GeneratedExtension<MessageOptions, Integer>) field.get(protoClass);

        Method method = methodOptional.get();
        FileDescriptor fileDescriptor = (FileDescriptor) method.invoke(protoClass);

        for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
            int msgId = descriptor.getOptions().getExtension(msgIdExtension);
            String name = descriptor.getName();
            consumer.accept(msgId, name);
        }
    }
}
