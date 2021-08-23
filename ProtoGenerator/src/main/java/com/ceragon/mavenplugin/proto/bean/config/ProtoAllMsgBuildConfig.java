package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

@Data
public class ProtoAllMsgBuildConfig {
    private String vmFile;
    private String targetFile;
    private PathType targetPathType = PathType.absolutePath;
}
