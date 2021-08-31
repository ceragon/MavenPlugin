package com.ceragon.mavenplugin.proto.core.protoc;

import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import org.apache.maven.project.MavenProject;

import java.io.File;

public class OutputTargetUtil {
    public static void initTarget(final MavenProject project,final OutputTarget target) {
        target.setAddSources(target.getAddSources().toLowerCase().trim());
        if ("true".equals(target.getAddSources())) target.setAddSources("main");

        if (target.getOutputDirectory() == null) {
            String subdir = "generated-" + ("test".equals(target.getAddSources()) ? "test-" : "") + "sources";
            target.setOutputDirectory(new File(project.getBuild().getDirectory() + File.separator + subdir + File.separator));
        }

        if (target.getOutputDirectorySuffix() != null) {
            target.setOutputDirectory(new File(target.getOutputDirectory(), target.getOutputDirectorySuffix()));
        }
    }
}
