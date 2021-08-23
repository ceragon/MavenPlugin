package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.MsgDesc;
import com.google.protobuf.DescriptorProtos.MessageOptions;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FileDescriptor;
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
            consumer.accept(msgId, MsgDesc.builder()
                    .name(name)
                    .fullName(protoClass.getName() + "." + name)
                    .build());
        }
    }
}
