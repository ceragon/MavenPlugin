package com.ceragon.mavenplugin.table.bean;

import com.ceragon.mavenplugin.table.constant.ColumnType;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

@Builder
@Value
public class TableColumnInfo {
    String name;
    ColumnType type;
    @Singular
    Set<String> labels;
}
