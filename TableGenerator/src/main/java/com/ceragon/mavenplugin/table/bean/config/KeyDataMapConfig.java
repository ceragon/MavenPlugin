package com.ceragon.mavenplugin.table.bean.config;

import com.ceragon.mavenplugin.constant.BaseTypeEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 将表格数据提取成Key，map格式，且key不能重复
 * 而map的key是一级列的名称
 */
@Data
public class KeyDataMapConfig {
    /**
     * vm模板文件目录
     */
    String vmFile;
    /**
     * 导出的目标路径
     */
    String targetFile;
    /**
     * key所在的列的名字，只检索第一级列
     */
    String keyColumn;
    /**
     * key所在的列，对应的字段类型
     */
    BaseTypeEnum keyColumnType;
    /**
     * 文件全称匹配，只有以下名字才会处理
     */
    List<String> fileNameMatch = new ArrayList<>();
}
