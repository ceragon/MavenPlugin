package com.ceragon.mavenplugin.proto.bean;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class ErrorMsg {
    private String className;
    private Integer msgId;
    private String msgName;
}
