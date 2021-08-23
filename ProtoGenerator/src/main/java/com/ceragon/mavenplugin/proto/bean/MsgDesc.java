package com.ceragon.mavenplugin.proto.bean;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MsgDesc {
    String name;
    String fullName;
}
