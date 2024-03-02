package json.sql.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import json.sql.CurContextProxy;
import json.sql.enums.MacroEnum;
import json.sql.grammar.JsonSqlVisitor;
import json.sql.udf.TypeReference;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

public class MethodUtil {

    private MethodUtil(){

    }

    /**
     * 执行一个静态公有的方法
     * @param method 具体的method
     * @param wrapperMethodArgs 参数列表
     * @return 返回方法执行结果
     */
    public static Object invokeMethod(Method method, Object[] wrapperMethodArgs) {
        Object result = null;
        try {
            if(ObjectUtil.isNull(wrapperMethodArgs)){
                result = method.invoke(null);
            }else{
                result = method.invoke(null, wrapperMethodArgs);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }finally {
            CurContextProxy curContext = CurContext.getCurContext();
            if(ObjectUtil.isNotEmpty(curContext)){
                curContext.setJsonSqlVisitor(null);
            }
            CurContext.remove();
        }
        return result;
    }

    /**
     * 获取一个符合method的参数类型的列表
     * @param context JsonSqlVisitor 上下文
     * @param innerArgs 参数列表
     * @param methodName 函数名称
     * @param argsTypeClasses method的参数类型列表
     * @return 符合method参数类型的列表
     */
    public static Object[] wrapperMethodArgs(JsonSqlVisitor context,List<Object> innerArgs, String methodName, List<Class<?>> argsTypeClasses ) {
        List<Object> innerArgsList = new ArrayList<>();
        List<MacroEnum> macroEnums = context.getMacroMap().get(methodName);
        if(ObjectUtil.isNotEmpty(macroEnums)){
            for (MacroEnum macroEnum : macroEnums) {
                innerArgsList.add(context.getMacro(macroEnum));
            }
        }
        if(argsTypeClasses.isEmpty()){
            if(ObjectUtil.isNotEmpty(innerArgsList)){
                return innerArgsList.toArray();
            }else {
                return null;
            }
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
                            TypeReference functionVariableArgsType = context.getFunctionVariableArgsType(methodName, aClass);
                            if(ObjectUtil.isNotEmpty(functionVariableArgsType)){
                                keyClazz = functionVariableArgsType.getArgGenericityType1();
                                valueClazz = functionVariableArgsType.getArgGenericityType2();
                            }
                            int j = 0;
                            for (; j+i < innerArgs.size() - otherArgsNum; j+=2) {
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
                            TypeReference functionVariableArgsType = context.getFunctionVariableArgsType(methodName, aClass);
                            if(ObjectUtil.isNotEmpty(functionVariableArgsType)){
                                valueClazz = functionVariableArgsType.getArgGenericityType1();
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
            return innerArgsList.toArray();
        }
    }

}
