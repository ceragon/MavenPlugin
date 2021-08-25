package com.ceragon.mavenplugin.table.bean.config;

import lombok.Data;

import java.util.List;

@Data
public class TableConfig {
    List<KeyDataMapConfig> eachTableKeyDataMap;
}
