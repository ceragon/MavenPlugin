package com.ceragon.mavenplugin.table.bean;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
public class TableData {
    String tableName;
    @Singular
    List<Map<String, Object>> cellDatas;
}
