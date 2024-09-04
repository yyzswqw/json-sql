package json.shell.command;

import cn.hutool.core.util.ObjectUtil;
import json.shell.ShellContext;
import json.shell.annotation.CommandClass;
import json.shell.annotation.CommandMethod;
import json.shell.annotation.CommandMethodIgnore;
import json.shell.annotation.CommandParam;
import json.shell.entity.CommandDescInfo;
import json.shell.entity.CommandParamDescInfo;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.entity.UdfParamDescInfo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@CommandClass
public class CommonCommand implements Serializable {

    @CommandMethod(name = {"exit","quit","q"},desc = "退出当前终端")
    public void sql(ShellContext shellContext){
        System.exit(0);
    }

    @CommandMethod(name = {"sqlUdf"},desc = "展示所有注册的udf函数")
    public String sqlHelp(ShellContext shellContext,
                          @CommandParam(desc = "sql中udf函数名称，为空时查询全部")String udfName){
        if(ObjectUtil.isNotEmpty(udfName)){
            UdfFunctionDescInfo descInfo = shellContext.getJsonSqlContext().getFunctionDescInfo(udfName);
            if(ObjectUtil.isEmpty(descInfo)){
                return "not has udf name: " + udfName;
            }else{
                return showUdfDesc(shellContext,Arrays.asList(descInfo));
            }
        }else{
            Collection<UdfFunctionDescInfo> allFunctionDescInfo = shellContext.getJsonSqlContext().getAllFunctionDescInfo();
            if(ObjectUtil.isEmpty(allFunctionDescInfo)){
                return "not has udf";
            }else{
                return showUdfDesc(shellContext,allFunctionDescInfo);
            }
        }
    }

    @CommandMethod(name = {"help","h"},desc = "展示可使用的命令")
    public String help(ShellContext shellContext,
                       @CommandParam(desc = "命令名称，为空时查询全部")String commandName){
        if(ObjectUtil.isNotEmpty(commandName)){
            CommandDescInfo commandDescInfo = shellContext.getCommandDescInfo(commandName);
            if(ObjectUtil.isEmpty(commandDescInfo)){
                return "not has command name: " + commandName;
            }else{
                return showDesc(shellContext,Arrays.asList(commandDescInfo));
            }
        }else{
            Map<String, CommandDescInfo> commandDescInfoMap = shellContext.getCommandDescInfoMap();
            if(ObjectUtil.isEmpty(commandDescInfoMap)){
                return "not has commands";
            }else{
                return showDesc(shellContext,commandDescInfoMap.values());
            }
        }
    }

    @CommandMethodIgnore
    public String showUdfDesc(ShellContext shellContext,
                              Collection<UdfFunctionDescInfo> allDescInfo){
        StringBuilder sb = new StringBuilder();
        if(ObjectUtil.isEmpty(allDescInfo)){
            sb.append("not has commands");
            return sb.toString();
        }
        for (UdfFunctionDescInfo descInfo : allDescInfo) {
            String functionName = descInfo.getFunctionName();
            String functionDesc = descInfo.getFunctionDesc();
            String returnType = descInfo.getReturnType();
            String sourceByClass = descInfo.getSourceByClass();
            List<UdfParamDescInfo> udfParamDescInfoList = descInfo.getUdfParamDescInfoList();

            sb.append(functionName).append("\n\tdesc: ").append(functionDesc)
                    .append("\n\tSource By Class : ").append(ObjectUtil.isEmpty(sourceByClass)? "unknown": sourceByClass)
                    .append("\n\tReturns: ").append(returnType)
                    .append("\n\tArgs:\n");
            if(ObjectUtil.isEmpty(udfParamDescInfoList)){
                sb.append("\t\tNone\n");
            }
            for (UdfParamDescInfo paramDescInfo : udfParamDescInfoList) {
                String paramName = paramDescInfo.getParamName();
                String paramType = paramDescInfo.getParamType();
                String paramDesc = paramDescInfo.getParamDesc();
                sb.append("\t\t").append(paramName)
                        .append("\n\t\t\t").append(String.format("%-15s", paramType))
                        .append("\t").append(paramDesc).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @CommandMethodIgnore
    public String showDesc(ShellContext shellContext,
                           Collection<CommandDescInfo> allDescInfo){
        StringBuilder sb = new StringBuilder();
        if(ObjectUtil.isEmpty(allDescInfo)){
            sb.append("not has commands");
            return sb.toString();
        }
        for (CommandDescInfo descInfo : allDescInfo) {
            String functionName = descInfo.getName();
            String functionDesc = descInfo.getFunctionDesc();
            String returnType = descInfo.getReturnType();
            List<CommandParamDescInfo> udfParamDescInfoList = descInfo.getCommandParamDescInfoList();

            sb.append(functionName).append("\n\tdesc: ").append(functionDesc)
                    .append("\n\tReturns: ").append(returnType)
                    .append("\n\tArgs:\n");
            if(ObjectUtil.isEmpty(udfParamDescInfoList)){
                sb.append("\t\tNone\n");
            }
            for (CommandParamDescInfo paramDescInfo : udfParamDescInfoList) {
                String paramName = paramDescInfo.getParamName();
                String paramType = paramDescInfo.getParamType();
                String paramDesc = paramDescInfo.getParamDesc();
                sb.append("\t\t").append(paramName)
                        .append("\n\t\t\t").append(String.format("%-15s", paramType))
                        .append("\t").append(paramDesc).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

}
