package com.ceragon.mavenplugin.proto.core.protoc;

import com.ceragon.mavenplugin.util.StringUtils;
import com.github.os72.protocjar.Protoc;
import com.github.os72.protocjar.ProtocVersion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;

public class CommandUtil {
    public static String buildProtocExe(Log log,final String protocVersion) throws MojoExecutionException {
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
}
