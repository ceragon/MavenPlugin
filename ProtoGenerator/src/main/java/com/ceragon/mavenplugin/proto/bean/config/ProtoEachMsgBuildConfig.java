package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoEachMsgBuildConfig {
    private String vmFile;
    private String targetFile;
    private PathType targetPathType = PathType.absolutePath;
    private List<String> classMatch = new ArrayList<>();
}
