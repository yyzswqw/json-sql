package json.shell;

import cn.hutool.core.util.ObjectUtil;
import json.shell.entity.CommandDescInfo;
import json.shell.utils.ConsoleLog;
import json.sql.JsonSqlContext;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class ShellContext implements Serializable {

    private ConsoleLog consoleLog;

    private JsonSqlContext jsonSqlContext;

    private Map<String, CommandDescInfo> commandDescInfoMap;

    private String defaultSavePath;

    private String defaultTempDataPath;

    public ShellContext(){}

    public CommandDescInfo getCommandDescInfo(String commandName){
        Map<String, CommandDescInfo> commandDescInfoMap1 = getCommandDescInfoMap();
        if(ObjectUtil.isEmpty(commandDescInfoMap1) || ObjectUtil.isEmpty(commandName)){
            return null;
        }
        return commandDescInfoMap1.get(commandName);
    }


}
