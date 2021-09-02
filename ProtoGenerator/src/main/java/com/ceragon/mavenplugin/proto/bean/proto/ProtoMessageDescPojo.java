package com.ceragon.mavenplugin.proto.bean.proto;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProtoMessageDescPojo {
    ProtoFileDescPojo filePojo;
    String name;
    Descriptor descriptor;
    List<FieldDescriptor> fieldList;

    public long getOptionInt(int number) {
        UnknownFieldSet unknownFieldSet = descriptor.getOptions().getUnknownFields();
        if (!unknownFieldSet.hasField(number)) {
            return 0;
        }
        Field field = descriptor.getOptions().getUnknownFields().getField(number);
        if (field.getVarintList().isEmpty()) {
            return 0;
        }
        return field.getVarintList().get(0);
    }

    public String getFileName() {
        return filePojo.getName();
    }
}
