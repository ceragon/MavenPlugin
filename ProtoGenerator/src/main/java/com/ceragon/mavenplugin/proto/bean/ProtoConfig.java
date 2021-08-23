package com.ceragon.mavenplugin.proto.bean;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ProtoConfig {
    private Map<String, String> vmToFileMap = new HashMap<>();
}
