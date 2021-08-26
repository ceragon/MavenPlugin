package com.ceragon.mavenplugin.table.load;

import com.ceragon.mavenplugin.table.bean.Environment;
import com.ceragon.mavenplugin.table.bean.TableData;
import com.ceragon.mavenplugin.table.load.logic.GroovyLoadLogic;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.net.MalformedURLException;
import java.util.List;

public interface ILoadLogic {
    List<TableData> execute(File loadScriptFile, Environment environment) throws Exception;

    enum LogicType {
        groovy(".groovy") {
            @Override
            ILoadLogic instance() {
                return new GroovyLoadLogic();
            }
        },
        python(".py") {
            @Override
            ILoadLogic instance() throws OperationNotSupportedException {
                throw new OperationNotSupportedException("not support python script");
            }
        },
        ;
        private final String suffix;

        LogicType(String suffix) {
            this.suffix = suffix;
        }

        private final static LogicType[] allEnums = LogicType.values();

        abstract ILoadLogic instance() throws OperationNotSupportedException;

        public static ILoadLogic getLogic(File file) throws OperationNotSupportedException {
            for (LogicType logicType : allEnums) {
                if (file.toPath().endsWith(logicType.suffix)) {
                    return logicType.instance();
                }
            }
            throw new OperationNotSupportedException("not support file type");
        }
    }

}