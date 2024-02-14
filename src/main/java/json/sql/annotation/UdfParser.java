package json.sql.annotation;

import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.sql.entity.UdfFunctionDescInfo;
import json.sql.entity.UdfParamDescInfo;
import json.sql.enums.MacroEnum;
import json.sql.udf.BaseTypeReference;
import json.sql.udf.ListTypeReference;
import json.sql.udf.MapTypeReference;
import json.sql.udf.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;

@Slf4j
public class UdfParser {

    private UdfParser(){

    }

    /**
     * 注册 class udf 函数
     * @param jsonSqlContext jsonSqlContext
     * @param clazz class
     * @param onlyParseAnnotation 是否只解析带有注解的函数
     * @param ignoreMethodName 需要忽略的方法名
     */
    public static void classParser(JsonSqlContext jsonSqlContext,Class<?> clazz,boolean onlyParseAnnotation,String ... ignoreMethodName){
        List<Method> allPublicStaticMethodList = getAllPublicStaticMethod(clazz);
        Set<String> ignoreMethodNameSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreMethodName)){
            ignoreMethodNameSet.addAll(Arrays.asList(ignoreMethodName));
        }
        for (Method method : allPublicStaticMethodList) {
            UdfMethodIgnore udfMethodIgnore = method.getAnnotation(UdfMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String functionName = method.getName();
            UdfMethod udfMethod = method.getAnnotation(UdfMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(udfMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.functionName())){
                functionName = udfMethod.functionName();
            }
            if(ignoreMethodNameSet.contains(functionName)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerUdfMethod(jsonSqlContext, method);
            }catch (Exception e){
                log.info("注册udf 函数失败! functionName : {} ,class : {} ,method : {} ,parameterTypes : {}",functionName,clazz.getName(),method.getName(),parameterTypes);
                log.error("注册udf 函数失败!",e);
            }

        }
    }

    /**
     * 注册 class udf 函数
     * @param jsonSqlContext jsonSqlContext
     * @param clazz class
     * @param onlyParseAnnotation 是否只解析带有注解的函数
     * @param ignoreMethod 需要忽略的方法
     */
    public static void classParser(JsonSqlContext jsonSqlContext,Class<?> clazz,boolean onlyParseAnnotation,Method ... ignoreMethod){
        List<Method> allPublicStaticMethodList = getAllPublicStaticMethod(clazz);
        Set<Method> ignoreMethodSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreMethod)){
            ignoreMethodSet.addAll(Arrays.asList(ignoreMethod));
        }
        for (Method method : allPublicStaticMethodList) {
            UdfMethodIgnore udfMethodIgnore = method.getAnnotation(UdfMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String functionName = method.getName();
            UdfMethod udfMethod = method.getAnnotation(UdfMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(udfMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.functionName())){
                functionName = udfMethod.functionName();
            }
            if(ignoreMethodSet.contains(method)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerUdfMethod(jsonSqlContext, method);
            }catch (Exception e){
                log.info("注册udf 函数失败! functionName : {} ,class : {} ,method : {} ,parameterTypes : {}",functionName,clazz.getName(),method.getName(),parameterTypes);
                log.error("注册udf 函数失败!",e);
            }
        }
    }

    /**
     * 注册一个udf函数
     * @param jsonSqlContext jsonSqlContext
     * @param method method
     */
    public static void registerUdfMethod(JsonSqlContext jsonSqlContext, Method method) {
        // 获取参数列表
        String functionName = method.getName();
        UdfMethod udfMethod = method.getAnnotation(UdfMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.functionName())){
            functionName = udfMethod.functionName();
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        Type[] genericParameterTypes = method.getGenericParameterTypes();

        if(!checkUdfMethod(method)){
            log.info("udf 函数不符合规范! functionName : {} ,method : {} ,parameterTypes : {}", functionName, method.getName(),parameterTypes);
            return;
        }

        if(ObjectUtil.isEmpty(parameterTypes)){
            jsonSqlContext.registerFunction(functionName, method);
            return;
        }
        List<MacroEnum> macroList = new ArrayList<>();
        List<Class<?>> argsTypeList = new ArrayList<>();
        Class<?> variableArgsType = null;
        TypeReference genericityArgsType = new BaseTypeReference();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameterTypes[i];
            MacroParam annotation = parameter.getAnnotation(MacroParam.class);
            if(ObjectUtil.isNotEmpty(annotation)){
                macroList.add(annotation.type());
                continue;
            }
            argsTypeList.add(parameterType);
            if(parameterType.isArray() || Map.class.isAssignableFrom(parameterType) || Collection.class.isAssignableFrom(parameterType)){
                variableArgsType = parameterType;
                VariableArgsGenericityType variableArgsGenericityType = parameter.getAnnotation(VariableArgsGenericityType.class);
                if(ObjectUtil.isNotEmpty(variableArgsGenericityType)){
                    Class<?> argType = variableArgsGenericityType.argType();
                    Class<?> argGenericityType1 = variableArgsGenericityType.argGenericityType1();
                    Class<?> argGenericityType2 = variableArgsGenericityType.argGenericityType2();
                    if(Map.class.isAssignableFrom(argType) || Map.class.isAssignableFrom(parameterType)){
                        genericityArgsType = new MapTypeReference(argGenericityType1, argGenericityType2);
                    }else if(Collection.class.isAssignableFrom(argType) || Collection.class.isAssignableFrom(parameterType)){
                        genericityArgsType = new ListTypeReference(argGenericityType1);
                    }
                }else {
                    List<String> simpleNameList = new ArrayList<>();
                    List<Class<?>> clazzList = new ArrayList<>();
                    Type genericParameterType = genericParameterTypes[i];
                    if (genericParameterType instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericParameterType;
                        Type[] actualTypeArguments = paramType.getActualTypeArguments();
                        try {
                            for (Type actualTypeArgument : actualTypeArguments) {
                                Class<?> actualTypeArgumentClass = (Class<?>) actualTypeArgument;
                                clazzList.add(actualTypeArgumentClass);
                                String simpleName = actualTypeArgumentClass.getSimpleName();
                                simpleNameList.add(simpleName);
                            }
                        }catch (Exception e){
                            simpleNameList.clear();
                            clazzList.clear();
                        }
                    }
                    if(ObjectUtil.isNotEmpty(simpleNameList)){
                        try {
                            if(Map.class.isAssignableFrom(parameterType)){
                                genericityArgsType = new MapTypeReference(clazzList.get(0), clazzList.get(1));
                            }else if(Collection.class.isAssignableFrom(parameterType)){
                                genericityArgsType = new ListTypeReference(clazzList.get(0));
                            }
                        }catch (Exception e){}
                    }
                }
            }
        }
        MacroEnum[] macroEnums = new MacroEnum[macroList.size()];
        for (int i = 0; i < macroList.size(); i++) {
            macroEnums[i] = macroList.get(i);
        }
        Class<?>[] argsTypes = new Class<?>[argsTypeList.size()];
        for (int i = 0; i < argsTypeList.size(); i++) {
            argsTypes[i] = argsTypeList.get(i);
        }
        jsonSqlContext.registerFunction(functionName, method,macroEnums,argsTypes,variableArgsType,genericityArgsType);
        UdfFunctionDescInfo udfDescInfo = getUdfDescInfo(method);
        jsonSqlContext.registerFunctionDescInfo(functionName, udfDescInfo);
    }

    /**
     * 获取一个udf 函数的描述信息
     * @param method udf函数
     * @return 描述信息
     */
    public static UdfFunctionDescInfo getUdfDescInfo(Method method){
        if(ObjectUtil.isEmpty(method)){
            return null;
        }
        UdfFunctionDescInfo descInfo = new UdfFunctionDescInfo();
        String functionName = method.getName();
        UdfMethod udfMethod = method.getAnnotation(UdfMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.functionName())){
            functionName = udfMethod.functionName();
        }
        descInfo.setFunctionName(functionName);
        if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.desc())){
            descInfo.setFunctionDesc(udfMethod.desc());
        }else{
            descInfo.setFunctionDesc(functionName);
        }
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        Type[] genericParameterTypes = method.getGenericParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> parameterType = parameterTypes[i];
            MacroParam annotation = parameter.getAnnotation(MacroParam.class);
            if(ObjectUtil.isNotEmpty(annotation)){
                continue;
            }
            String paramName = parameter.getName();
            UdfParamDescInfo paramDescInfo = new UdfParamDescInfo();
            paramDescInfo.setParamType(parameterType.getSimpleName());
            paramDescInfo.setParamName(paramName);

            if(parameterType.isArray() || Map.class.isAssignableFrom(parameterType) || Collection.class.isAssignableFrom(parameterType)){
                VariableArgsGenericityType variableArgsGenericityType = parameter.getAnnotation(VariableArgsGenericityType.class);
                if(ObjectUtil.isNotEmpty(variableArgsGenericityType)){
                    Class<?> argType = variableArgsGenericityType.argType();
                    Class<?> argGenericityType1 = variableArgsGenericityType.argGenericityType1();
                    Class<?> argGenericityType2 = variableArgsGenericityType.argGenericityType2();
                    if(Map.class.isAssignableFrom(argType) || Map.class.isAssignableFrom(parameterType)){
                        String simpleName = argGenericityType1.getSimpleName();
                        String simpleName1 = argGenericityType2.getSimpleName();
                        paramDescInfo.setParamType(String.format("%s<%s,%s>", paramDescInfo.getParamType(),simpleName,simpleName1));
                    }else if(Collection.class.isAssignableFrom(argType) || Collection.class.isAssignableFrom(parameterType)){
                        String simpleName = argGenericityType1.getSimpleName();
                        paramDescInfo.setParamType(String.format("%s<%s>", paramDescInfo.getParamType(),simpleName));
                    }
                }else {
                    List<String> simpleNameList = new ArrayList<>();
                    Type genericParameterType = genericParameterTypes[i];
                    if (genericParameterType instanceof ParameterizedType) {
                        ParameterizedType paramType = (ParameterizedType) genericParameterType;
                        Type[] actualTypeArguments = paramType.getActualTypeArguments();
                        try {
                            for (Type actualTypeArgument : actualTypeArguments) {
                                Class<?> actualTypeArgumentClass = (Class<?>) actualTypeArgument;
                                String simpleName = actualTypeArgumentClass.getSimpleName();
                                simpleNameList.add(simpleName);
                            }
                        }catch (Exception e){
                            simpleNameList.clear();
                        }
                    }
                    if(ObjectUtil.isNotEmpty(simpleNameList)){
                        paramDescInfo.setParamType(String.format("%s<%s>", paramDescInfo.getParamType(),String.join(",", simpleNameList)));
                    }else if(Map.class.isAssignableFrom(parameterType)){
                        paramDescInfo.setParamType(String.format("%s<%s,%s>", paramDescInfo.getParamType(),Object.class.getSimpleName(),Object.class.getSimpleName()));
                    }else if(Collection.class.isAssignableFrom(parameterType)){
                        paramDescInfo.setParamType(String.format("%s<%s>", paramDescInfo.getParamType(),Object.class.getSimpleName()));
                    }
                }
            }
            String paramDesc = "";
            UdfParam udfParam = parameter.getAnnotation(UdfParam.class);
            if(ObjectUtil.isNotEmpty(udfParam) && ObjectUtil.isNotEmpty(udfParam.desc())){
                paramDesc = udfParam.desc();
            }
            if(ObjectUtil.isNotEmpty(udfParam) && ObjectUtil.isNotEmpty(udfParam.name())){
                paramDescInfo.setParamName(udfParam.name());
            }
            paramDescInfo.setParamDesc(paramDesc);
            descInfo.getUdfParamDescInfoList().add(paramDescInfo);
        }
        String returnTypeSimpleName = method.getReturnType().getSimpleName();
        // 获取方法的返回类型
        Type returnType = method.getGenericReturnType();
        descInfo.setReturnType(returnTypeSimpleName);
        List<String> simpleNameList = new ArrayList<>();
        if (returnType instanceof ParameterizedType) {
            // 强制转换为参数化类型
            ParameterizedType parameterizedType = (ParameterizedType) returnType;
            // 获取泛型类型的数组
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            // 输出泛型类型
            for (Type typeArgument : typeArguments) {
                String simpleName = typeArgument.getTypeName();
                try {
                    simpleName = ((Class<?>) typeArgument).getSimpleName();
                }catch (Exception e){}
                simpleNameList.add(simpleName);
            }
        }
        if(ObjectUtil.isNotEmpty(simpleNameList)){
            descInfo.setReturnType(String.format("%s<%s>", descInfo.getReturnType(),String.join(",", simpleNameList)));
        }
        return descInfo;
    }

    /**
     * 校验一个方法是否可以被注册为UDF
     * @param method method
     * @return true:是，false:否
     */
    public static boolean checkUdfMethod(Method method) {
        // 获取参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        Parameter[] parameters = method.getParameters();
        if(ObjectUtil.isEmpty(parameterTypes)){
            return true;
        }
        // 检查参数列表中宏参数是否都在参数列表的最前面
        int macroIndex = 0;
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            MacroParam annotation = parameter.getAnnotation(MacroParam.class);
            if(ObjectUtil.isEmpty(annotation)){
                continue;
            }
            if(macroIndex != i){
                return false;
            }
            macroIndex ++;
        }
        // 校验可变参数是否只有一个
        int variableArgsNum = 0;
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            Parameter parameter = parameters[i];
            MacroParam annotation = parameter.getAnnotation(MacroParam.class);
            if(ObjectUtil.isNotEmpty(annotation)){
                continue;
            }
            if(parameterType.isArray() || Map.class.isAssignableFrom(parameterType) || Collection.class.isAssignableFrom(parameterType)){
                variableArgsNum +=1 ;
                if(variableArgsNum > 1){
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 获取所有公共静态方法
     * @param clazz clazz
     * @return 所有公共静态方法
     */
    public static List<Method> getAllPublicStaticMethod(Class<?> clazz){
        List<Method> list = new ArrayList<>();
        // 获取所有公共方法
        Method[] methods = clazz.getMethods();
        // 遍历方法并筛选出静态方法
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                list.add(method);
            }
        }
        return list;
    }

}
