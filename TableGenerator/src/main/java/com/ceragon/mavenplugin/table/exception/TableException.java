package com.ceragon.mavenplugin.table.exception;

import com.ceragon.mavenplugin.util.StringUtils;

import java.util.Map;

public class TableException extends Exception {
    public TableException(String tableName, Map<String, Object> cellData, String message, Object... params) {
        super(buildMessage(tableName, cellData, message, params));
    }

    private static String buildMessage(String tableName, Map<String, Object> cellData,
                                       String message, Object[] params) {
        message = StringUtils.format(message, params);
        return StringUtils.format("{}.tableName={},cellData={}", message, tableName, cellData);
    }

}
