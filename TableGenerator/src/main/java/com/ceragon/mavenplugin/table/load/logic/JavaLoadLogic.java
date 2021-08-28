package com.ceragon.mavenplugin.table.load.logic;

import com.ceragon.mavenplugin.table.ILoadScript;
import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.table.load.ILoadLogic;
import com.ceragon.mavenplugin.util.ClassUtil;
import com.ceragon.mavenplugin.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;

public class JavaLoadLogic implements ILoadLogic {
    @Override
    public List<TableData> execute(ConfigContext context, Environment environment) throws Exception {
        ClassLoader loader = ClassUtil.getClassLoader(context.getProject().getCompileClasspathElements());
        Class<?> scriptClass = loader.loadClass(context.getLoadScriptClass());
        if (scriptClass == null) {
            return Collections.emptyList();
        }
        if (!ILoadScript.class.isAssignableFrom(scriptClass)) {
            throw new Exception(
                    StringUtils.format("the script {} is not implements ILoadScript", scriptClass));
        }
        Constructor<?> constructor = scriptClass.getDeclaredConstructor();
        ILoadScript loadScript = (ILoadScript) constructor.newInstance();
        return loadScript.load(context.getLog(), environment);
    }
}
