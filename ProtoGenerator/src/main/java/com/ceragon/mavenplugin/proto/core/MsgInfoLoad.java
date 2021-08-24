package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.MsgDesc;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.GeneratedExtension;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;

@AllArgsConstructor
public class MsgInfoLoad {
    String msgIdField;
    GeneratedMessage.GeneratedExtension<DescriptorProtos.MessageOptions, Integer> msgIdExtension;

    /**
     * 遍历类中的每一个消息
     *
     * @param protoClass
     * @param consumer   处理遍历到的消息Id和内容
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void forEachMsg(Class<?> protoClass, BiConsumer<Integer, MsgDesc> consumer) throws IllegalAccessException, InvocationTargetException {
        // 过滤一部分类
        if (protoClass.getSuperclass() != Object.class) {
            return;
        }
        // 必须包含 getDescriptor 这个方法
        Optional<Method> methodOptional = Arrays.stream(protoClass.getDeclaredMethods())
                .filter(f -> f.getName().equals("getDescriptor")).findFirst();
        if (methodOptional.isEmpty()) {
            return;
        }

        Method method = methodOptional.get();
        FileDescriptor fileDescriptor = (FileDescriptor) method.invoke(protoClass);

        for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
            int msgId = descriptor.getOptions().getExtension(msgIdExtension);
            if (msgId == 0){
                continue;
            }
            String name = descriptor.getName();
            consumer.accept(msgId, MsgDesc.builder()
                    .name(name)
                    .fullName(protoClass.getName() + "." + name)
                    .build());
        }
    }
}