package com.ceragon.mavenplugin.table.load;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.table.load.ILoadLogic.LogicType;
import com.ceragon.mavenplugin.util.StringUtils;
import lombok.AllArgsConstructor;
import org.apache.maven.plugin.logging.Log;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class LoadTableData {
    ConfigContext context;

    public List<TableData> loadTableData() throws Exception {
        ILoadLogic loadLogic = getLoadLogic();
        if (loadLogic == null) {
            return Collections.emptyList();
        }
        Environment environment = Environment.builder()
                .sourceDir(context.getTableSourceDir())
                .build();
        return loadLogic.execute(context, environment);
    }

    private ILoadLogic getLoadLogic() throws OperationNotSupportedException {
        String loadScriptClass = context.getLoadScriptClass();
        File loadScriptFile = context.getLoadScriptFile();
        ILoadLogic loadLogic = null;
        if (!StringUtils.isEmpty(loadScriptClass)) {
            loadLogic = LogicType.java.instance();
        } else if (loadScriptFile.exists()) {
            loadLogic = ILoadLogic.LogicType.getLogic(loadScriptFile);
        }
        return loadLogic;
    }

}
