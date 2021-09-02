package com.ceragon.mavenplugin.proto.bean.proto;

import com.google.protobuf.FieldType;
import lombok.Data;

@Data
public class ProtoFieldPojo {
    FieldType fieldType;
    String fullName;
    int index;
    Object defaultValue;


}
