package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

@Data
public class ProtoTotalMsgBuildConfig {
    String vmFile;
    String targetFile;
    boolean overwrite = true;
}
