package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoConfig {
    private List<ProtoTotalMsgBuildConfig> allMsg = new ArrayList<>();
    private List<ProtoEveryMsgBuildConfig> eachMsg = new ArrayList<>();
    private List<ProtoEveryProtoBuildConfig> eachClass = new ArrayList<>();
}
