package com.ceragon.mavenplugin.proto.bean;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.SortedMap;

@Data
@Builder
public class ProtoMsgInfo {
    private String className;
    @Singular
    private SortedMap<Integer, String> msgIdAndNames;
    private int maxMsgId;
}
