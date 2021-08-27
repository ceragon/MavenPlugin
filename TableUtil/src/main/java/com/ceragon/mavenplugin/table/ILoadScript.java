package com.ceragon.mavenplugin.table;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;

import java.util.List;

public interface ILoadScript {
    List<TableData> load(Environment env);
}
