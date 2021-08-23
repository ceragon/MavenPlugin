package com.ceragon.mavenplugin.proto.bean.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProtoConfig {
    private List<ProtoVmInfo> vmInfoList = new ArrayList<>();
}
