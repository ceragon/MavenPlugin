package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

@Data
public class ProtoVmInfo {
    private String vmFile;
    private String targetFile;
    private PathType targetPathType = PathType.src;
}
