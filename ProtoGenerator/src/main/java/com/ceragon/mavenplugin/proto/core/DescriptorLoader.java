package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.util.FileFilter;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class DescriptorLoader {
    public static void loadDesc(File descPath) {
        FileFilter fileFilter = new FileFilter(".desc");
        FileUtils.listFiles(descPath, fileFilter, TrueFileFilter.INSTANCE)
                .forEach(DescriptorLoader::buildDesc);
    }

    private static void buildDesc(File descFile) {
        BuildContext context = ThreadBuildContext.getContext();
        try (FileInputStream fin = new FileInputStream(descFile)) {
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
        } catch (IOException | DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
