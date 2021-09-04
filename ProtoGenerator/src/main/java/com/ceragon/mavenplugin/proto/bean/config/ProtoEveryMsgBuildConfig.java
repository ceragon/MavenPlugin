package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoEveryMsgBuildConfig {
    String vmFile;
    String targetFile;
    List<String> msgNameMatch;
    List<String> protoNameMatch = new ArrayList<>();
    boolean overwrite = true;
}
