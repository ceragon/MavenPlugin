package com.ceragon.mavenplugin.proto.core.protoc;

import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
    public static File createTempDir(String name) throws MojoExecutionException {
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

    public static void streamCopy(InputStream in, OutputStream out) throws IOException {
        int read = 0;
        byte[] buf = new byte[4096];
        while ((read = in.read(buf)) > 0) out.write(buf, 0, read);
    }

    public static void deleteOnExitRecursive(File dir) {
        dir.deleteOnExit();
        for (File f : dir.listFiles()) {
            f.deleteOnExit();
            if (f.isDirectory()) deleteOnExitRecursive(f);
        }
    }
}
