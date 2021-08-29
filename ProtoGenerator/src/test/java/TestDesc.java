import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestDesc {
    public static void main(String[] args) throws IOException, DescriptorValidationException {
        InputStream fin = TestDesc.class.getResourceAsStream("/login.desc");
        FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(fin);
        for (FileDescriptorProto fdp : descriptorSet.getFileList()) {
            FileDescriptor fd = FileDescriptor.buildFrom(fdp, new FileDescriptor[]{});

            for (Descriptor descriptor : fd.getMessageTypes()) {
                String className = fdp.getOptions().getJavaPackage() + "."
                        + fdp.getOptions().getJavaOuterClassname() + "$"
                        + descriptor.getName();
                List<FieldDescriptor> types = descriptor.getFields();
                for (FieldDescriptor type : types) {
                    System.out.println(type.getFullName());
                }
                System.out.println(descriptor.getFullName() + " -> " + className);
            }
        }

    }
}
