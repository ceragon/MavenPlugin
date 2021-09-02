package com.ceragon.mavenplugin.proto.bean.proto;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoFileDescPojo {
    FileDescriptorProto proto;
    String javaPackage;
    boolean javaMultipleFiles;
    List<ProtoMessageDescPojo> messageList = new ArrayList<>();

    public void addMessageDescPojo(ProtoMessageDescPojo messageDescPojo) {
        messageList.add(messageDescPojo);
    }

    public String getName(){
        return proto.getName();
    }
    public String getJavaOuterClassname(){
        return proto.getOptions().getJavaOuterClassname();
    }
}
