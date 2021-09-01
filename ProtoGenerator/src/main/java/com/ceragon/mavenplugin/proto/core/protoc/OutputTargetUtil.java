package com.ceragon.mavenplugin.proto.core.protoc;

import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import com.ceragon.mavenplugin.proto.constant.ContextKey;
import com.ceragon.mavenplugin.util.FileFilter;
import com.ceragon.mavenplugin.util.StringUtils;
import com.github.os72.protocjar.Protoc;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class OutputTargetUtil {
    public static void initTarget(final MavenProject project, final OutputTarget target) {
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

    public static void preprocessTarget(OutputTarget target) throws MojoExecutionException {
        BuildContext context = ThreadBuildContext.getContext();
        Log log = (Log) context.getValue(ContextKey.LOG);
        File f = target.getOutputDirectory();
        if (!f.exists()) {
            log.info(f + " does not exist. Creating...");
            f.mkdirs();
        }

        if (target.isCleanOutputFolder()) {
            try {
                log.info("Cleaning " + f);
                FileUtils.cleanDirectory(f);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public static void processTarget(String protocCommand, final List<File> inputDirectoryList,
                                     final List<File> includeDirectoryList, final OutputTarget target) throws MojoExecutionException {
        BuildContext context = ThreadBuildContext.getContext();
        final Log log = (Log) context.getValue(ContextKey.LOG);
        final String extension = (String) context.getValue(ContextKey.PROTO_EXTENSION);
        final String protocVersion = (String) context.getValue(ContextKey.PROTOC_VERSION);
        boolean shaded = false;
        String targetType = target.getType();
        if (targetType.equals("java-shaded") || targetType.equals("java_shaded")) {
            targetType = "java";
            shaded = true;
        }
        FileFilter fileFilter = new FileFilter(extension);
        for (File input : inputDirectoryList) {
            if (input == null) continue;

            if (input.exists() && input.isDirectory()) {
                Collection<File> protoFiles = FileUtils.listFiles(input, fileFilter, TrueFileFilter.INSTANCE);
                for (File protoFile : protoFiles) {
                    if (target.isCleanOutputFolder() || context.hasDelta(protoFile.getPath())) {
                        processFile(context, includeDirectoryList, protocCommand, protoFile, targetType, null, target.getOutputDirectory(), target.getOutputOptions());
                    } else {
                        log.info("Not changed " + protoFile);
                    }
                }
            } else {
                if (input.exists()) log.warn(input + " is not a directory");
                else log.warn(input + " does not exist");
            }
        }

        if (shaded) {
            try {
                log.info("    Shading (version " + protocVersion + "): " + target.getOutputDirectory());
                Protoc.doShading(target.getOutputDirectory(), protocVersion);
            } catch (IOException e) {
                throw new MojoExecutionException("Error occurred during shading", e);
            }
        }
    }

    private static void processFile(final BuildContext context, final List<File> includeDirectories, final String protocCommand, final File file,
                                    final String type, final String pluginPath, final File outputDir,
                                    final String outputOptions) throws MojoExecutionException {
        Log log = (Log) context.getValue(ContextKey.LOG);
        log.info("    Processing (" + type + "): " + file.getName());

        try {
            context.removeMessages(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            TeeOutputStream outTee = new TeeOutputStream(System.out, out);
            TeeOutputStream errTee = new TeeOutputStream(System.err, err);

            int ret = 0;
            Collection<String> cmd = buildCommand(context, includeDirectories, file, type, pluginPath, outputDir, outputOptions);
            if (protocCommand == null) ret = Protoc.runProtoc(cmd.toArray(new String[0]), outTee, errTee);
            else ret = Protoc.runProtoc(protocCommand, Arrays.asList(cmd.toArray(new String[0])), outTee, errTee);

            // add eclipse m2e warnings/errors
            String errStr = err.toString();
            if (!StringUtils.isEmpty(errStr)) {
                int severity = (ret != 0) ? BuildContext.SEVERITY_ERROR : BuildContext.SEVERITY_WARNING;
                String[] lines = errStr.split("\\n", -1);
                for (String line : lines) {
                    int lineNum = 0;
                    int colNum = 0;
                    String msg = line;
                    if (line.contains(file.getName())) {
                        String[] parts = line.split(":", 4);
                        if (parts.length == 4) {
                            try {
                                lineNum = Integer.parseInt(parts[1]);
                                colNum = Integer.parseInt(parts[2]);
                                msg = parts[3];
                            } catch (Exception e) {
                                log.warn("Failed to parse protoc warning/error for " + file);
                            }
                        }
                    }
                    context.addMessage(file, lineNum, colNum, msg, severity, null);
                }
            }

            if (ret != 0) throw new MojoExecutionException("protoc-jar failed for " + file + ". Exit code " + ret);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Interrupted", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute protoc-jar for " + file, e);
        }
    }

    private static Collection<String> buildCommand(BuildContext context, List<File> includeDirectories, File file, String type, String pluginPath, File outputDir, String outputOptions) throws MojoExecutionException {
        Log log = (Log) context.getValue(ContextKey.LOG);
        final String version = (String) context.getValue(ContextKey.PROTOC_VERSION);
        final boolean includeImports = (boolean) context.getValue(ContextKey.INCLUDE_IMPORTS);
        Collection<String> cmd = new ArrayList<String>();
        populateIncludes(includeDirectories, cmd);
        cmd.add("-I" + file.getParentFile().getAbsolutePath());
        if ("descriptor".equals(type)) {
            File outFile = new File(outputDir, file.getName());
            cmd.add("--descriptor_set_out=" + FilenameUtils.removeExtension(outFile.toString()) + ".desc");
            if (includeImports) {
                cmd.add("--include_imports");
            }
            if (outputOptions != null) {
                for (String arg : outputOptions.split("\\s+")) cmd.add(arg);
            }
        } else {
            if (outputOptions != null) {
                cmd.add("--" + type + "_out=" + outputOptions + ":" + outputDir);
            } else {
                cmd.add("--" + type + "_out=" + outputDir);
            }

            if (pluginPath != null) {
                log.info("    Plugin path: " + pluginPath);
                cmd.add("--plugin=protoc-gen-" + type + "=" + pluginPath);
            }
        }
        cmd.add(file.toString());
        if (version != null) cmd.add("-v" + version);
        return cmd;
    }

    private static void populateIncludes(List<File> includeDirectories, Collection<String> args) throws MojoExecutionException {
        for (File include : includeDirectories) {
            if (!include.exists())
                throw new MojoExecutionException("Include path '" + include.getPath() + "' does not exist");
            if (!include.isDirectory())
                throw new MojoExecutionException("Include path '" + include.getPath() + "' is not a directory");
            args.add("-I" + include.getPath());
        }
    }
}
