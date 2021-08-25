package com.ceragon.mavenplugin.table.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 将表格数据提取成Key，Value格式，且key不能重复
 */
@Data
public class TablePairConfig {
    /**
     * vm模板文件目录
     */
    String vmFile;
    /**
     * 导出的目标路径
     */
    String targetFile;
    /**
     * key所在的列的名字，只检索一级列
     */
    String keyColumn;
    /**
     * value所在的列的名字，只检索一级列
     */
    String valueColumn;
    /**
     * 文件全称匹配，只有以下名字才会处理
     */
    List<String> fileNameMatch = new ArrayList<>();
}
