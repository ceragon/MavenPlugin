package com.ceragon.mavenplugin.table.language;

import com.ceragon.mavenplugin.table.constant.ConfigContext;
import com.ceragon.mavenplugin.util.CodeGenTool;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageService {
    private final static LanguageService instance = new LanguageService();

    private LanguageService() {
    }

    public static LanguageService getInstance() {
        return instance;
    }

    public boolean generate(Object languageYaml, ConfigContext context) {
        Log log = context.getLog();
        Map<Integer, Map<String, Object>> languageMap = (Map<Integer, Map<String, Object>>) languageYaml;
        List<LangPojo> infoList = new ArrayList<>();
        languageMap.forEach((id, obj) -> {
            String desc = (String) obj.get("zh_cn");
            String variable = (String) obj.get("variable");
            infoList.add(new LangPojo(variable, id, desc));
        });
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("className", context.getLangClassName());
        contentMap.put("packageName", context.getLangPackageName());
        contentMap.put("infoList", infoList);
        String path = context.getLangPackageName().replaceAll("\\.", File.separator);
        String destPath = context.getOutputDirectory().getPath() + File.separator + path + File.separator +
                context.getLangClassName() + ".java";
        try {
            CodeGenTool.createCode("NoticeEnum.vm", destPath, contentMap);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
        return true;
    }
}
