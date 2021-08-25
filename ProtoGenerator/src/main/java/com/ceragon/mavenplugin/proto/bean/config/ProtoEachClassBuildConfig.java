package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoEachClassBuildConfig {
    private String vmFile;
    private String targetFile;
    private String msgEndWith;
    private List<String> classMatch = new ArrayList<>();
}
