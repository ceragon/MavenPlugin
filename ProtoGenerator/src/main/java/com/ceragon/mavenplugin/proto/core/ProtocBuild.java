package com.ceragon.mavenplugin.proto.core;

import com.ceragon.mavenplugin.proto.bean.OutputTarget;
import com.ceragon.mavenplugin.util.FileFilter;
import com.ceragon.mavenplugin.util.StringUtils;
import com.github.os72.protocjar.Protoc;
import com.github.os72.protocjar.ProtocVersion;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Builder
public class ProtocBuild {
    Log log;
    MavenProject project;
    String protocVersion;
    File[] inputDirectories;
    File[] includeDirectories;
    boolean includeStdTypes;
    OutputTarget[] outputTargets;
    boolean includeImports;
    BuildContext buildContext;
    private final static String extension = ".proto";
    private final static String includeMavenTypes = "transitive";
    private String protocCommand;

    public void process() throws MojoExecutionException {
        for (OutputTarget target : outputTargets) {
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

        protocCommand = prepareProtoc();
        File tmpDir = createTempDir("protocjar");


        if (includeStdTypes || hasIncludeMavenTypes()) {
            try {
                File extraTypeDir = new File(tmpDir, "include");
                extraTypeDir.mkdir();
                log.info("Additional include types: " + extraTypeDir);
                addIncludeDir(extraTypeDir);
                if (includeStdTypes)
                    Protoc.extractStdTypes(ProtocVersion.getVersion("-v" + protocVersion), tmpDir); // yes, tmpDir
                if (hasIncludeMavenTypes())
                    extractProtosFromDependencies(extraTypeDir, includeMavenTypes.equalsIgnoreCase("transitive"));
                deleteOnExitRecursive(extraTypeDir);
            } catch (IOException e) {
                throw new MojoExecutionException("Error extracting additional include types", e);
            }
        }

        for (OutputTarget target : outputTargets) {
            preprocessTarget(target);
        }
        for (OutputTarget target : outputTargets) {
            processTarget(target);
        }
    }

    static void deleteOnExitRecursive(File dir) {
        dir.deleteOnExit();
        for (File f : dir.listFiles()) {
            f.deleteOnExit();
            if (f.isDirectory()) deleteOnExitRecursive(f);
        }
    }

    private String prepareProtoc() throws MojoExecutionException {
        if (StringUtils.isEmpty(protocVersion)) protocVersion = ProtocVersion.PROTOC_VERSION.mVersion;
        log.info("Protoc version: " + protocVersion);
        String protocCommand;
        try {
            File protocFile = Protoc.extractProtoc(ProtocVersion.getVersion("-v" + protocVersion), false);
            protocCommand = protocFile.getAbsolutePath();
            try {
                // some linuxes don't allow exec in /tmp, try one dummy execution, switch to user home if it fails
                Protoc.runProtoc(protocCommand, new String[]{"--version"});
            } catch (Exception e) {
                File tempRoot = new File(System.getProperty("user.home"));
                protocFile = Protoc.extractProtoc(ProtocVersion.getVersion("-v" + protocVersion), false, tempRoot);
                protocCommand = protocFile.getAbsolutePath();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error extracting protoc for version " + protocVersion, e);
        }
        return protocCommand;
    }


    static File createTempDir(String name) throws MojoExecutionException {
        try {
            File tmpDir = File.createTempFile(name, "");
            tmpDir.delete();
            tmpDir.mkdirs();
            tmpDir.deleteOnExit();
            return tmpDir;
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating temporary directory: " + name, e);
        }
    }

    private void preprocessTarget(OutputTarget target) throws MojoExecutionException {
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

    private void processTarget(OutputTarget target) throws MojoExecutionException {
        boolean shaded = false;
        String targetType = target.getType();
        if (targetType.equals("java-shaded") || targetType.equals("java_shaded")) {
            targetType = "java";
            shaded = true;
        }
        FileFilter fileFilter = new FileFilter(extension);
        for (File input : inputDirectories) {
            if (input == null) continue;

            if (input.exists() && input.isDirectory()) {
                Collection<File> protoFiles = FileUtils.listFiles(input, fileFilter, TrueFileFilter.INSTANCE);
                for (File protoFile : protoFiles) {
                    if (target.isCleanOutputFolder() || buildContext.hasDelta(protoFile.getPath())) {
                        processFile(protoFile, protocVersion, targetType, null, target.getOutputDirectory(), target.getOutputOptions());
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

    private void processFile(File file, String version, String type, String pluginPath, File outputDir, String outputOptions) throws MojoExecutionException {
        log.info("    Processing (" + type + "): " + file.getName());

        try {
            buildContext.removeMessages(file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            TeeOutputStream outTee = new TeeOutputStream(System.out, out);
            TeeOutputStream errTee = new TeeOutputStream(System.err, err);

            int ret = 0;
            Collection<String> cmd = buildCommand(file, version, type, pluginPath, outputDir, outputOptions);
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
                    buildContext.addMessage(file, lineNum, colNum, msg, severity, null);
                }
            }

            if (ret != 0) throw new MojoExecutionException("protoc-jar failed for " + file + ". Exit code " + ret);
        } catch (InterruptedException e) {
            throw new MojoExecutionException("Interrupted", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to execute protoc-jar for " + file, e);
        }
    }

    private Collection<String> buildCommand(File file, String version, String type, String pluginPath, File outputDir, String outputOptions) throws MojoExecutionException {
        Collection<String> cmd = new ArrayList<String>();
        populateIncludes(cmd);
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

    private boolean hasIncludeMavenTypes() {
        return includeMavenTypes.equalsIgnoreCase("direct") || includeMavenTypes.equalsIgnoreCase("transitive");
    }

    private void addIncludeDir(File dir) {
        includeDirectories = addDir(includeDirectories, dir);
    }

    static File[] addDir(File[] dirs, File dir) {
        if (dirs == null) {
            dirs = new File[]{dir};
        } else {
            dirs = Arrays.copyOf(dirs, dirs.length + 1);
            dirs[dirs.length - 1] = dir;
        }
        return dirs;
    }

    private Set<Artifact> getArtifactsForProtoExtraction(boolean transitive) {
        if (transitive) return project.getArtifacts();
        return project.getDependencyArtifacts();
    }

    private List<File> listFilesRecursively(File directory, String ext, List<File> list) {
        for (File f : directory.listFiles()) {
            if (f.isFile() && f.canRead() && f.getName().toLowerCase().endsWith(ext)) list.add(f);
            else if (f.isDirectory() && f.canExecute()) listFilesRecursively(f, ext, list);
        }
        return list;
    }

    private void writeProtoFile(File dir, InputStream zis, String name) throws IOException {
        log.info("    " + name);
        File protoOut = new File(dir, name);
        protoOut.getParentFile().mkdirs();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(protoOut);
            streamCopy(zis, fos);
        } finally {
            if (fos != null) fos.close();
        }
    }

    static void streamCopy(InputStream in, OutputStream out) throws IOException {
        int read = 0;
        byte[] buf = new byte[4096];
        while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
    }

    private void extractProtosFromDependencies(File dir, boolean transitive) throws IOException {
        for (Artifact artifact : getArtifactsForProtoExtraction(transitive)) {
            if (artifact.getFile() == null) continue;
            log.debug("  Scanning artifact: " + artifact.getFile());
            InputStream is = null;
            try {
                if (artifact.getFile().isDirectory()) {
                    for (File f : listFilesRecursively(artifact.getFile(), extension, new ArrayList<File>())) {
                        is = new FileInputStream(f);
                        String name = f.getAbsolutePath().replace(artifact.getFile().getAbsolutePath(), "");
                        if (name.startsWith("/")) name = name.substring(1);
                        writeProtoFile(dir, is, name);
                        is.close();
                    }
                } else {
                    ZipInputStream zis = new ZipInputStream(new FileInputStream(artifact.getFile()));
                    is = zis;
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.isDirectory() || !ze.getName().toLowerCase().endsWith(extension)) continue;
                        writeProtoFile(dir, zis, ze.getName());
                        zis.closeEntry();
                    }
                }
            } catch (IOException e) {
                log.info("  Error scanning artifact: " + artifact.getFile() + ": " + e);
            } finally {
                if (is != null) is.close();
            }
        }
    }

    private void populateIncludes(Collection<String> args) throws MojoExecutionException {
        for (File include : includeDirectories) {
            if (!include.exists())
                throw new MojoExecutionException("Include path '" + include.getPath() + "' does not exist");
            if (!include.isDirectory())
                throw new MojoExecutionException("Include path '" + include.getPath() + "' is not a directory");
            args.add("-I" + include.getPath());
        }
    }
}
