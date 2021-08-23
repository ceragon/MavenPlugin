package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoConfig {
    private List<ProtoAllMsgBuildConfig> allMsg = new ArrayList<>();
    private List<ProtoEachMsgBuildConfig> eachMsg = new ArrayList<>();
}
