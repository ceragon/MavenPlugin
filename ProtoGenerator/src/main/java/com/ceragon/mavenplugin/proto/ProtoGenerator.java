package com.ceragon.mavenplugin.proto;

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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mojo(name = "generator", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class ProtoGenerator extends AbstractMojo {
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> compilePath;
    private Log log;
    @Parameter(property = "protoPackage", required = true, readonly = true)
    private String protoPackage;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log = getLog();
        try {
            ClassUtil classUtil = new ClassUtil(log, compilePath);
            Set<Class<?>> protoClasses = classUtil.scan(protoPackage, null);
            for (Class<?> protoClass : protoClasses) {
                if (protoClass.getSuperclass() != Object.class) {
                    continue;
                }
                Optional<Field> optional = Arrays.stream(protoClass.getDeclaredFields())
                        .filter(f -> f.getName().equals("msgId")).findFirst();
                if (optional.isEmpty()) {
                    continue;
                }
                Field field = optional.get();
                GeneratedExtension<MessageOptions, Integer> msgIdExtension = (GeneratedExtension<MessageOptions, Integer>) field.get(protoClass);
                Method method = protoClass.getDeclaredMethod("getDescriptor");
                FileDescriptor fileDescriptor = (FileDescriptor) method.invoke(protoClass);

                for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
                    int msgId = descriptor.getOptions().getExtension(msgIdExtension);
                    String name = descriptor.getName();
                    log.info("msg:" + name + ",msgId:" + msgId);
                }
            }
        } catch (MalformedURLException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error(e.getMessage(), e);
        }
    }
}
