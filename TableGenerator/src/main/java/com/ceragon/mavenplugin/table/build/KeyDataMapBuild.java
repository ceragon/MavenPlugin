package com.ceragon.mavenplugin.table.build;

import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.bean.config.KeyDataMapConfig;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.PathFormat;

import org.apache.maven.project.MavenProject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KeyDataMapBuild {
    MavenProject project;
    String resourceRoot;
    List<TableData> allTableDatas;
    List<Exception> exceptions;
    PathFormat pathFormat;

    public KeyDataMapBuild(MavenProject project, String resourceRoot, List<TableData> allTableDatas) {
        this.project = project;
        this.resourceRoot = resourceRoot;
        this.allTableDatas = allTableDatas;
        this.pathFormat = new PathFormat(project);
    }

    public void processAllTable(KeyDataMapConfig config) {
        String sourceName = config.getVmFile();
        Map<String, TableData> tableDataMap = allTableDatas.stream()
                .filter(tableData -> config.getFileNameMatch().contains(tableData.getTableName()))
                .collect(Collectors.toMap(TableData::getTableName, tableData -> tableData));

        try {
            String destPath = pathFormat.format(config.getTargetFile(), "tableName", tableData.getTableName());
            CodeGenTool.createCodeByPath(resourceRoot, sourceName, destPath, true, content);
        } catch (IOException e) {
            exceptions.add(e);
        }
    }
}
