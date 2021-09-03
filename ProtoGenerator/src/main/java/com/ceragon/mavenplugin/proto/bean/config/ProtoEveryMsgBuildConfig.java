package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoEveryMsgBuildConfig {
    private String vmFile;
    private String targetFile;
    private List<String> msgNameMatch;
    private List<String> protoNameMatch = new ArrayList<>();
}
