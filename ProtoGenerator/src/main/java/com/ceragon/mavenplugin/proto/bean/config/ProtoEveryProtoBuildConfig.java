package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoEveryProtoBuildConfig {
    private String vmFile;
    private String targetFile;
    private List<String> protoNameMatch = new ArrayList<>();
}
