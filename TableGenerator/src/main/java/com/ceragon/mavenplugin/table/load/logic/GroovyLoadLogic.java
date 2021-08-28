package com.ceragon.mavenplugin.table.load.logic;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.table.load.ILoadLogic;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.net.URL;
import java.util.Collections;
import java.util.List;

public class GroovyLoadLogic implements ILoadLogic {
    @Override
    public List<TableData> execute(ConfigContext context, Environment environment) throws Exception {
        URL classPath = context.getLoadScriptFile().toPath().getParent().toUri().toURL();
        String scriptName = context.getLoadScriptFile().toPath().getFileName().toString();
        Binding binding = new Binding(Collections.singletonMap(Environment.ENV, environment));
        GroovyScriptEngine engine = new GroovyScriptEngine(new URL[]{classPath});
        Object result = engine.run(scriptName, binding);
        if (result == null) {
            return Collections.emptyList();
        }
        return (List<TableData>) result;

    }
}
