package json.shell.command;

import cn.hutool.core.lang.Console;
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
    public void sql(){
        System.exit(0);
    }

    @CommandMethod(name = {"sqlUdf"},desc = "展示所有注册的udf函数")
    public void sqlHelp(@CommandParam(desc = "sql中udf函数名称，为空时查询全部")String udfName){
        if(ObjectUtil.isNotEmpty(udfName)){
            UdfFunctionDescInfo descInfo = ShellContext.cur().getJsonSqlContext().getFunctionDescInfo(udfName);
            if(ObjectUtil.isEmpty(descInfo)){
                Console.log("not has udf name: {}",udfName);
            }else{
                String s = showUdfDesc(Arrays.asList(descInfo));
                Console.log(s);
            }
        }else{
            Collection<UdfFunctionDescInfo> allFunctionDescInfo = ShellContext.cur().getJsonSqlContext().getAllFunctionDescInfo();
            if(ObjectUtil.isEmpty(allFunctionDescInfo)){
                Console.log("not has udf");
            }else{
                String s = showUdfDesc(allFunctionDescInfo);
                Console.log(s);
            }
        }
    }

    @CommandMethod(name = {"help","h"},desc = "展示可使用的命令")
    public void help(@CommandParam(desc = "命令名称，为空时查询全部")String commandName){
        if(ObjectUtil.isNotEmpty(commandName)){
            CommandDescInfo commandDescInfo = ShellContext.cur().getCommandDescInfo(commandName);
            if(ObjectUtil.isEmpty(commandDescInfo)){
                Console.log("not has command name: {}",commandName);
            }else{
                String s = showDesc(Arrays.asList(commandDescInfo));
                Console.log(s);
            }
        }else{
            Map<String, CommandDescInfo> commandDescInfoMap = ShellContext.cur().getCommandDescInfoMap();
            if(ObjectUtil.isEmpty(commandDescInfoMap)){
                Console.log("not has commands");
            }else{
                String s = showDesc(commandDescInfoMap.values());
                Console.log(s);
            }
        }
    }

    @CommandMethodIgnore
    public String showUdfDesc(Collection<UdfFunctionDescInfo> allDescInfo){
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
    public String showDesc(Collection<CommandDescInfo> allDescInfo){
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
