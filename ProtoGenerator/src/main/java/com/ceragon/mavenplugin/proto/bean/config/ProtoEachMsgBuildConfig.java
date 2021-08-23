package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ProtoEachMsgBuildConfig {
    private String vmFile;
    private String targetFile;
    private PathType targetPathType = PathType.absolutePath;
    private String msgEndWith;
    private List<String> classMatch = new ArrayList<>();
}
