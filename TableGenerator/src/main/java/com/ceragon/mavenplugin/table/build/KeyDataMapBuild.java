package com.ceragon.mavenplugin.table.build;

import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.bean.config.KeyDataMapConfig;
import com.ceragon.mavenplugin.table.exception.TableException;
import com.ceragon.mavenplugin.util.CodeGenTool;
import com.ceragon.mavenplugin.util.PathFormat;
import com.ceragon.mavenplugin.util.VelocityUtil;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyDataMapBuild {
    MavenProject project;
    List<TableData> allTableDatas;
    List<Exception> exceptions;
    PathFormat pathFormat;

    public KeyDataMapBuild(MavenProject project, List<TableData> allTableDatas) {
        this.project = project;
        this.allTableDatas = allTableDatas;
        this.pathFormat = new PathFormat(project);
    }

    public void processAllTable(KeyDataMapConfig config) {
        Map<String, TableData> tableDataMap = allTableDatas.stream()
                .filter(tableData -> config.getFileNameMatch().contains(tableData.getTableName()))
                .collect(Collectors.toMap(TableData::getTableName, tableData -> tableData));

        tableDataMap.forEach((tableName, tableData) -> processOneTable(tableName, tableData, config));
    }

    private void processOneTable(String tableName, TableData tableData, KeyDataMapConfig config) {
        String vmFilePath = config.getVmFile();
        vmFilePath = pathFormat.format(vmFilePath);
        File vmFile = new File(vmFilePath);
        String vmPath = vmFile.toPath().getParent().toFile().getPath();
        String vmFileName = vmFile.toPath().getFileName().toString();
        Map<String, Object> content = new HashMap<>();
        if (tableData.getCellDatas().stream()
                .anyMatch(data -> checkErrorAndBuildContent(data, tableName, config, content))) {
            // 有错误，停止
            return;
        }
        try {
            String destPath = pathFormat.format(config.getTargetFile(), "tableName", tableName);
            CodeGenTool.createCodeByPath(vmPath, vmFileName, destPath, true,
                    Map.of("infoList", content, "util", VelocityUtil.getInstance()));
        } catch (IOException e) {
            exceptions.add(e);
        }
    }

    private boolean checkErrorAndBuildContent(Map<String, Object> cellData, String tableName,
                                              KeyDataMapConfig config, Map<String, Object> content) {
        Object keyValue = cellData.get(config.getKeyColumn());
        if (keyValue == null) {
            exceptions.add(new TableException(tableName, cellData, "the {} is null", config.getKeyColumn()));
            return true;
        }
        if (!config.getKeyColumnType().checkMatch(keyValue)) {
            exceptions.add(new TableException(tableName, cellData, "the {} is wrong type", config.getKeyColumn()));
            return true;
        }
        String keyValueStr = keyValue.toString();
        if (content.containsKey(keyValueStr)) {
            exceptions.add(new TableException(tableName, cellData, "the {} is repeat,value={}",
                    config.getKeyColumn(), keyValue));
            return true;
        }
        content.put(keyValueStr, cellData);
        return false;
    }
}
