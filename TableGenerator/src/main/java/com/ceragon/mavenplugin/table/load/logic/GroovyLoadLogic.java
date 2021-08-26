package com.ceragon.mavenplugin.table.load.logic;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.load.ILoadLogic;
import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class GroovyLoadLogic implements ILoadLogic {
    @Override
    public List<TableData> execute(File loadScriptFile, Environment environment) throws Exception {
        URL classPath = loadScriptFile.toPath().getParent().toUri().toURL();
        String scriptName = loadScriptFile.toPath().getFileName().toString();
        Binding binding = new Binding(Collections.singletonMap(Environment.ENV, environment));
        GroovyScriptEngine engine = new GroovyScriptEngine(new URL[]{classPath});
        Object result = engine.run(scriptName, binding);
        if (result == null) {
            return Collections.emptyList();
        }
        return (List<TableData>) result;

    }
}
