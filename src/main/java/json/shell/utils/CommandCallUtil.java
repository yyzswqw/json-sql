package json.shell.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import json.shell.ShellContext;
import json.shell.entity.CommandDescInfo;
import json.shell.entity.CommandParamDescInfo;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class CommandCallUtil {

    private CommandCallUtil(){}

    public static <T> T callCommand(ShellContext shellContext,CommandDescInfo commandDescInfo, Object ... args){
        if(ObjectUtil.isEmpty(commandDescInfo)){
            return null;
        }
        List<Object> innerArgs = new ArrayList<>();
        if(ObjectUtil.isNotEmpty(args)){
            innerArgs.addAll(Arrays.asList(args));
        }
        String commandName = commandDescInfo.getName();
        Object instance = commandDescInfo.getInstance();
        Method method = commandDescInfo.getMethod();
        List<? extends Class<?>> argsTypeClasses = commandDescInfo.getCommandParamDescInfoList().stream().map(CommandParamDescInfo::getArgsType).collect(Collectors.toList());
        Object result = null;
        List<Object> innerArgsList = new ArrayList<>();
        innerArgsList.add(shellContext);
        try {
            if(argsTypeClasses.isEmpty()){
                result = method.invoke(instance);
            }else{
                int curArgsIndex = 0;
                for (int i = 0; i < argsTypeClasses.size(); i++) {
                    Class<?> aClass = argsTypeClasses.get(i);
                    Object innerArg = null;
                    if(curArgsIndex < innerArgs.size()){
                        innerArg = innerArgs.get(curArgsIndex);
                    }
    //                        解析可变函数
                    if(aClass.isArray() || Map.class.isAssignableFrom(aClass) || Collection.class.isAssignableFrom(aClass)){
                        int otherArgsNum = argsTypeClasses.size() - (i + 1);
                        int variableArgsNum = innerArgs.size() - otherArgsNum - i;
                        if(variableArgsNum <= 0){
                            // 没有传可变参数
                            innerArgsList.add(null);
                        }else{
                            if (aClass.isArray()) {
                                // 获取数组元素的类型
                                Class<?> componentType = aClass.getComponentType();
                                if (ObjectUtil.isEmpty(innerArgs) || innerArgs.size() - curArgsIndex < 0) {
                                    innerArgsList.add(null);
                                    break ;
                                }
                                Object arguments = Array.newInstance(componentType, variableArgsNum);
                                int j = 0;
                                while (j+i < innerArgs.size() - otherArgsNum) {
                                    Object convert = null;
                                    innerArg = innerArgs.get(j+i);
                                    try {
                                        convert = Convert.convert(componentType,innerArg);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    Array.set(arguments, j++, convert);
                                }
                                innerArgsList.add(arguments);
                                curArgsIndex += j;
                                continue ;
                            }
                            else if (Map.class.isAssignableFrom(aClass)) {
                                Map<Object,Object> temp = new LinkedHashMap<>();
                                Object innerArgKey = null;
                                Object innerArgValue = null;
                                Class<?> keyClazz = Object.class;
                                Class<?> valueClazz = Object.class;
                                List<Class<?>> genericityTypeList = commandDescInfo.getCommandParamDescInfoList().get(i).getGenericityTypeList();
                                if(ObjectUtil.isNotEmpty(genericityTypeList) && genericityTypeList.size() >= 1){
                                    keyClazz = genericityTypeList.get(0);
                                }
                                if(ObjectUtil.isNotEmpty(genericityTypeList) && genericityTypeList.size() >= 2){
                                    valueClazz = genericityTypeList.get(1);
                                }
                                int j = 0;
                                for (;j+i < innerArgs.size() - otherArgsNum;j+=2) {
                                    Object convert = null;
                                    innerArgKey = null;
                                    innerArgKey = innerArgs.get(j+i);
                                    innerArgValue = null;
                                    try {
                                        innerArgKey = Convert.convert(keyClazz,innerArgKey);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    if(j+i+1 < innerArgs.size()){
                                        innerArgValue = innerArgs.get(j+i+1);
                                        try {
                                            convert = Convert.convert(valueClazz,innerArgValue);
                                        }catch (Exception e){
                                            e.printStackTrace();
                                        }
                                    }
                                    if(ObjectUtil.isNotEmpty(innerArgKey)){
                                        temp.put(innerArgKey,convert);
                                    }
                                }
                                Object convert = null;
                                try {
                                    convert = Convert.convert(aClass, temp);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                innerArgsList.add(convert);
                                curArgsIndex += j;
                                continue ;
                            }
                            else if (Collection.class.isAssignableFrom(aClass)) {
                                List<Object> temp = new ArrayList<>();
                                Class<?> valueClazz = Object.class;
                                List<Class<?>> genericityTypeList = commandDescInfo.getCommandParamDescInfoList().get(i).getGenericityTypeList();
                                if(ObjectUtil.isNotEmpty(genericityTypeList) && genericityTypeList.size() >= 1){
                                    valueClazz = genericityTypeList.get(0);
                                }
                                int j = 0;
                                while (j+i < innerArgs.size() - otherArgsNum) {
                                    Object convert = null;
                                    innerArg = innerArgs.get(j+i);
                                    try {
                                        convert = Convert.convert(valueClazz,innerArg);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                    temp.add(convert);
                                    j++;
                                }
                                Object convert = null;
                                try {
                                    convert = Convert.convert(aClass, temp);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                                innerArgsList.add(convert);
                                curArgsIndex += j;
                                continue ;
                            }
                        }
                    }
                    else {
                        Object convert = null;
                        try {
                            convert = Convert.convert(aClass,innerArg);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        innerArgsList.add(convert);
                        curArgsIndex++;
                    }
                }
                result = method.invoke(instance, innerArgsList.toArray());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("执行 command : "+ commandName + "失败",e);
//            e.printStackTrace();
            return null;
        }
        return (T) result;
    }

}
