package json.sql.util;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ObjectUtil;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.entity.UdfParamDescInfo;

import java.util.Collection;
import java.util.List;

public class PrintUtil {

    private PrintUtil(){}

    public void showUdfDesc(Collection<UdfFunctionDescInfo> allFunctionDescInfo){
        StringBuilder sb = new StringBuilder();
        if(ObjectUtil.isEmpty(allFunctionDescInfo)){
            sb.append("not has udf function");
            Console.log(sb);
            return ;
        }
        for (UdfFunctionDescInfo descInfo : allFunctionDescInfo) {
            String functionName = descInfo.getFunctionName();
            String functionDesc = descInfo.getFunctionDesc();
            String returnType = descInfo.getReturnType();
            List<UdfParamDescInfo> udfParamDescInfoList = descInfo.getUdfParamDescInfoList();

            sb.append(functionName).append("\n\tdesc: ").append(functionDesc)
                    .append("\n\tReturns: ").append(returnType)
                    .append("\n\targs:\n");
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
        Console.log(sb);
    }

}
