package json.sql.annotation;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.TypeUtil;
import json.sql.JsonSqlContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;

@Slf4j
public class CompareSymbolParser {

    private CompareSymbolParser(){

    }

    /**
     * 注册 class 比较运算符 函数
     * @param jsonSqlContext jsonSqlContext
     * @param clazz class
     * @param onlyParseAnnotation 是否只解析带有注解的函数
     * @param ignoreSymbolName 需要忽略的符号名
     */
    public static void classParser(JsonSqlContext jsonSqlContext,Class<?> clazz,boolean onlyParseAnnotation,String ... ignoreSymbolName){
        List<Method> allPublicStaticMethodList = getAllPublicStaticMethod(clazz);
        Set<String> ignoreSymbolNameSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreSymbolName)){
            ignoreSymbolNameSet.addAll(Arrays.asList(ignoreSymbolName));
        }
        for (Method method : allPublicStaticMethodList) {
            CompareSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(CompareSymbolMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String symbol = method.getName();
            CompareSymbolMethod udfMethod = method.getAnnotation(CompareSymbolMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(udfMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.symbol())){
                symbol = udfMethod.symbol();
            }
            if(ignoreSymbolNameSet.contains(symbol)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerCompareSymbolMethod(jsonSqlContext, method);
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    log.debug("注册比较运算符 函数失败! symbol : {} ,class : {} ,method : {} ,parameterTypes : {}",symbol,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册比较运算符 函数失败!",e);
                }
            }

        }
    }

    /**
     * 注册 class 比较运算符 函数
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
            CompareSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(CompareSymbolMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String symbol = method.getName();
            CompareSymbolMethod udfMethod = method.getAnnotation(CompareSymbolMethod.class);
            if(onlyParseAnnotation && ObjectUtil.isEmpty(udfMethod)){
                continue;
            }
            if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.symbol())){
                symbol = udfMethod.symbol();
            }
            if(ignoreMethodSet.contains(method)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerCompareSymbolMethod(jsonSqlContext, method);
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    log.debug("注册比较运算符 函数失败! symbol : {} ,class : {} ,method : {} ,parameterTypes : {}",symbol,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册比较运算符 函数失败!",e);
                }
            }
        }
    }

    /**
     * 注册一个自定义比较符函数
     * @param jsonSqlContext jsonSqlContext
     * @param method method
     */
    public static void registerCompareSymbolMethod(JsonSqlContext jsonSqlContext, Method method) {
        if(ObjectUtil.isEmpty(method)){
            return ;
        }
        CompareSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(CompareSymbolMethodIgnore.class);
        if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
            return ;
        }
        // 获取参数列表
        String symbol = method.getName();
        CompareSymbolMethod udfMethod = method.getAnnotation(CompareSymbolMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethod) && ObjectUtil.isNotEmpty(udfMethod.symbol())){
            symbol = udfMethod.symbol();
        }
        Class<?>[] parameterTypes = method.getParameterTypes();

        if(!checkCompareMethod(method)){
            if (log.isDebugEnabled()) {
                log.debug("比较运算符 函数不符合规范! symbol : {} ,method : {} ,parameterTypes : {}", symbol, method.getName(),parameterTypes);
            }
            return;
        }
        jsonSqlContext.registerCompareSymbolFunction(symbol,method);
    }

    /**
     * 注册一个自定义比较符函数
     * @param jsonSqlContext jsonSqlContext
     * @param symbol 比较符标识
     * @param method method
     */
    public static void registerCompareSymbolMethod(JsonSqlContext jsonSqlContext, String symbol,Method method) {
        if(ObjectUtil.isEmpty(method)){
            return ;
        }
        CompareSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(CompareSymbolMethodIgnore.class);
        if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
            return ;
        }
        // 获取参数列表
        if(ObjectUtil.isEmpty(symbol)){
            symbol = method.getName();
        }
        Class<?>[] parameterTypes = method.getParameterTypes();

        if(!checkCompareMethod(method)){
            if (log.isDebugEnabled()) {
                log.debug("比较运算符 函数不符合规范! symbol : {} ,method : {} ,parameterTypes : {}", symbol, method.getName(),parameterTypes);
            }
            return;
        }
        jsonSqlContext.registerCompareSymbolFunction(symbol,method);
    }

    /**
     * 获取方法的参数列表
     * @param method 方法
     * @return 参数列表
     */
    public static cn.hutool.core.lang.TypeReference<?>[] getMethodArgsType(Method method){
        Parameter[] parameters = method.getParameters();
        if(ObjectUtil.isEmpty(parameters)){
            return null;
        }
        List<cn.hutool.core.lang.TypeReference<?>> argsTypeList = new ArrayList<>();
        for (int i = 0; i < parameters.length; i++) {
            int finalI = i;
            cn.hutool.core.lang.TypeReference<?> typeReference = new cn.hutool.core.lang.TypeReference(){
                @Override
                public Type getType() {
                    return TypeUtil.getParamType(method, finalI);
                }
            };
            argsTypeList.add(typeReference);
        }
        cn.hutool.core.lang.TypeReference<?>[] argsTypes = new cn.hutool.core.lang.TypeReference<?>[argsTypeList.size()];
        for (int i = 0; i < argsTypeList.size(); i++) {
            argsTypes[i] = argsTypeList.get(i);
        }
        return argsTypes;
    }

    /**
     * 校验一个方法是否可以被注册
     * @param method method
     * @return true:是，false:否
     */
    public static boolean checkCompareMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        if(ObjectUtil.isEmpty(parameters)){
            return false;
        }
        if(parameters.length != 2){
            return false;
        }
        Class<?> returnType = method.getReturnType();
        if (returnType.equals(boolean.class)) {
            return true;
        }
        return returnType.equals(Boolean.class);
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
