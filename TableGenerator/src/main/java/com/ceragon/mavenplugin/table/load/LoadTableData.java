package com.ceragon.mavenplugin.table.load;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import lombok.AllArgsConstructor;

import java.io.File;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class LoadTableData {
    File loadScriptFile;

    public List<TableData> loadTableData() throws Exception {
        if (!loadScriptFile.exists()) {
            return Collections.emptyList();
        }
        ILoadLogic loadLogic = ILoadLogic.LogicType.getLogic(loadScriptFile);
        Environment environment = new Environment();
        return loadLogic.execute(loadScriptFile, environment);
    }


}
