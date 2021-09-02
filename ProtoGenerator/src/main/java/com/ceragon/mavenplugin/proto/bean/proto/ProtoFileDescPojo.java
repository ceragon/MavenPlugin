package com.ceragon.mavenplugin.proto.bean.proto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoFileDescPojo {
    String name;
    String javaPackage;
    boolean javaMultipleFiles;
    List<ProtoMessageDescPojo> messageDescList = new ArrayList<>();

    public void addMessageDescPojo(ProtoMessageDescPojo messageDescPojo) {
        messageDescList.add(messageDescPojo);
    }
}
