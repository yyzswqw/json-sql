package json.shell;

import cn.hutool.core.util.ObjectUtil;
import json.shell.entity.CommandDescInfo;
import json.sql.JsonSqlContext;
import lombok.Data;
import org.jline.terminal.Terminal;

import java.io.Serializable;
import java.util.Map;

@Data
public class ShellContext implements Serializable {

    private static ShellContext shellContext = new ShellContext();

    private Terminal terminal;

    private JsonSqlContext jsonSqlContext;

    private Map<String, CommandDescInfo> commandDescInfoMap;

    private String defaultSavePath;

    private String defaultTempDataPath;

    private ShellContext(){}

    public static ShellContext cur(){
        return shellContext;
    }

    public CommandDescInfo getCommandDescInfo(String commandName){
        Map<String, CommandDescInfo> commandDescInfoMap1 = getCommandDescInfoMap();
        if(ObjectUtil.isEmpty(commandDescInfoMap1) || ObjectUtil.isEmpty(commandName)){
            return null;
        }
        return commandDescInfoMap1.get(commandName);
    }


}
