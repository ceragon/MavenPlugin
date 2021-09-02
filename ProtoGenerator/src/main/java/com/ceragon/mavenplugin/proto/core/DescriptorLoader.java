package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.proto.ProtoFileDescPojo;
import com.ceragon.mavenplugin.proto.bean.proto.ProtoMessageDescPojo;
import com.ceragon.mavenplugin.util.FileFilter;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DescriptorLoader {
    public static List<ProtoFileDescPojo> loadDesc(File descPath) {
        FileFilter fileFilter = new FileFilter(".desc");
        List<ProtoFileDescPojo> fileDescPojos = new ArrayList<>();
        if (!FileUtils.listFiles(descPath, fileFilter, TrueFileFilter.INSTANCE).stream()
                .allMatch(file -> buildFileDesc(fileDescPojos, file))) {
            return Collections.emptyList();
        }
        return fileDescPojos;
    }

    private static boolean buildFileDesc(List<ProtoFileDescPojo> fileDescPojos, File descFile) {
        BuildContext context = ThreadBuildContext.getContext();

        try (FileInputStream fin = new FileInputStream(descFile)) {
            FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(fin);
            for (FileDescriptorProto fdp : descriptorSet.getFileList()) {
                ProtoFileDescPojo fileDescPojo = new ProtoFileDescPojo();
                fileDescPojos.add(fileDescPojo);
                fileDescPojo.setProto(fdp);
                if (fdp.getMessageTypeCount() < 1) {
                    continue;
                }

                fileDescPojo.setJavaPackage(fdp.getOptions().getJavaPackage());
                fileDescPojo.setJavaMultipleFiles(fdp.getOptions().getJavaMultipleFiles());

                FileDescriptor fd = FileDescriptor.buildFrom(fdp, new FileDescriptor[]{});
                for (Descriptor descriptor : fd.getMessageTypes()) {
                    ProtoMessageDescPojo messageDescPojo = new ProtoMessageDescPojo(descriptor.getName(), descriptor, descriptor.getFields());
                    fileDescPojo.addMessageDescPojo(messageDescPojo);
                }
            }
            return true;
        } catch (IOException | DescriptorValidationException e) {
            throw new RuntimeException(e);
        }
    }
}
