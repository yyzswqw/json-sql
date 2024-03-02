package json.sql.annotation;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.TypeUtil;
import json.sql.JsonSqlContext;
import json.sql.enums.CalculateOperatorSymbolLevel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
public class OperatorSymbolParser {

    private OperatorSymbolParser(){

    }

    /**
     * 注册 class 计算运算符 函数
     * @param jsonSqlContext jsonSqlContext
     * @param clazz class
     * @param nonAnnotationRegisterLevel 没有注解的方法如何注册优先级的策略
     * @param ignoreMethodName 需要忽略的方法名
     */
    public static void classParser(JsonSqlContext jsonSqlContext,Class<?> clazz,CalculateOperatorSymbolLevel nonAnnotationRegisterLevel,String ... ignoreMethodName){
        List<Method> allPublicStaticMethodList = getAllPublicStaticMethod(clazz);
        Set<String> ignoreMethodNameSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreMethodName)){
            ignoreMethodNameSet.addAll(Arrays.asList(ignoreMethodName));
        }
        for (Method method : allPublicStaticMethodList) {
            OperatorSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(OperatorSymbolMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String symbol = method.getName();
            if(ignoreMethodNameSet.contains(symbol)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerOperatorSymbolMethod(jsonSqlContext, method,nonAnnotationRegisterLevel);
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    log.debug("注册计算运算符 函数失败! symbol : {} ,class : {} ,method : {} ,parameterTypes : {}",symbol,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册计算运算符 函数失败!",e);
                }
            }

        }
    }

    /**
     * 注册 class 计算运算符 函数
     * @param jsonSqlContext jsonSqlContext
     * @param clazz class
     * @param nonAnnotationRegisterLevel 没有注解的方法如何注册优先级的策略
     * @param ignoreMethod 需要忽略的方法
     */
    public static void classParser(JsonSqlContext jsonSqlContext,Class<?> clazz,CalculateOperatorSymbolLevel nonAnnotationRegisterLevel,Method ... ignoreMethod){
        List<Method> allPublicStaticMethodList = getAllPublicStaticMethod(clazz);
        Set<Method> ignoreMethodSet = new HashSet<>();
        if(ObjectUtil.isNotEmpty(ignoreMethod)){
            ignoreMethodSet.addAll(Arrays.asList(ignoreMethod));
        }
        for (Method method : allPublicStaticMethodList) {
            OperatorSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(OperatorSymbolMethodIgnore.class);
            if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
                continue;
            }
            String symbol = method.getName();
            if(ignoreMethodSet.contains(method)){
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            try {
                registerOperatorSymbolMethod(jsonSqlContext, method,nonAnnotationRegisterLevel);
            }catch (Exception e){
                if (log.isDebugEnabled()) {
                    log.debug("注册计算运算符 函数失败! methodName : {} ,class : {} ,method : {} ,parameterTypes : {}",symbol,clazz.getName(),method.getName(),parameterTypes);
                    log.debug("注册计算运算符 函数失败!",e);
                }
            }
        }
    }

    /**
     * 注册一个自定义比较符函数
     * @param jsonSqlContext jsonSqlContext
     * @param method method
     * @param nonAnnotationRegisterLevel 没有注解的方法如何注册优先级的策略
     */
    public static void registerOperatorSymbolMethod(JsonSqlContext jsonSqlContext, Method method, CalculateOperatorSymbolLevel nonAnnotationRegisterLevel) {
        if(ObjectUtil.isEmpty(method)){
            return ;
        }
        OperatorSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(OperatorSymbolMethodIgnore.class);
        if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
            return ;
        }
        // 获取参数列表
        String symbolTemp = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();

        if(!checkOperatorMethod(method)){
            if (log.isDebugEnabled()) {
                log.debug("计算运算符 函数不符合规范! methodName : {} ,method : {} ,parameterTypes : {}", symbolTemp, method.getName(),parameterTypes);
            }
            return;
        }
        HighOperatorSymbolMethod highOperatorSymbolMethod = method.getAnnotation(HighOperatorSymbolMethod.class);
        LowOperatorSymbolMethod lowOperatorSymbolMethod = method.getAnnotation(LowOperatorSymbolMethod.class);
        if(ObjectUtil.isNotEmpty(highOperatorSymbolMethod) || ( ObjectUtil.isNotEmpty(nonAnnotationRegisterLevel) && (CalculateOperatorSymbolLevel.BOTH == nonAnnotationRegisterLevel || CalculateOperatorSymbolLevel.MUL_DIV == nonAnnotationRegisterLevel))){
            if(ObjectUtil.isNotEmpty(highOperatorSymbolMethod) && ObjectUtil.isNotEmpty(highOperatorSymbolMethod.symbol())){
                symbolTemp = highOperatorSymbolMethod.symbol();
            }
            jsonSqlContext.registerHighOperatorSymbolFunction(symbolTemp,method);
        }
        if( ObjectUtil.isNotEmpty(lowOperatorSymbolMethod) || ( ObjectUtil.isNotEmpty(nonAnnotationRegisterLevel) && (CalculateOperatorSymbolLevel.BOTH == nonAnnotationRegisterLevel || CalculateOperatorSymbolLevel.ADD_SUB == nonAnnotationRegisterLevel))){
            if(ObjectUtil.isNotEmpty(lowOperatorSymbolMethod) && ObjectUtil.isNotEmpty(lowOperatorSymbolMethod.symbol())){
                symbolTemp = lowOperatorSymbolMethod.symbol();
            }
            jsonSqlContext.registerLowOperatorSymbolFunction(symbolTemp,method);
        }
    }

    /**
     * 注册一个自定义运算符函数
     * @param jsonSqlContext jsonSqlContext
     * @param symbol 运算符标识
     * @param method method
     */
    public static void registerOperatorSymbolMethod(JsonSqlContext jsonSqlContext, String symbol, Method method, CalculateOperatorSymbolLevel nonAnnotationRegisterLevel) {
        if(ObjectUtil.isEmpty(method)){
            return ;
        }
        OperatorSymbolMethodIgnore udfMethodIgnore = method.getAnnotation(OperatorSymbolMethodIgnore.class);
        if(ObjectUtil.isNotEmpty(udfMethodIgnore)){
            return ;
        }
        // 获取参数列表
        if(ObjectUtil.isEmpty(symbol)){
            symbol = method.getName();
        }
        Class<?>[] parameterTypes = method.getParameterTypes();

        if(!checkOperatorMethod(method)){
            if (log.isDebugEnabled()) {
                log.debug("计算运算符 函数不符合规范! symbol : {} ,method : {} ,parameterTypes : {}", symbol, method.getName(),parameterTypes);
            }
            return;
        }
        HighOperatorSymbolMethod highOperatorSymbolMethod = method.getAnnotation(HighOperatorSymbolMethod.class);
        LowOperatorSymbolMethod lowOperatorSymbolMethod = method.getAnnotation(LowOperatorSymbolMethod.class);
        if(ObjectUtil.isNotEmpty(highOperatorSymbolMethod) || ( ObjectUtil.isNotEmpty(nonAnnotationRegisterLevel) && (CalculateOperatorSymbolLevel.BOTH == nonAnnotationRegisterLevel || CalculateOperatorSymbolLevel.MUL_DIV == nonAnnotationRegisterLevel))){
            jsonSqlContext.registerHighOperatorSymbolFunction(symbol,method);
        }
        if( ObjectUtil.isNotEmpty(lowOperatorSymbolMethod) || ( ObjectUtil.isNotEmpty(nonAnnotationRegisterLevel) && (CalculateOperatorSymbolLevel.BOTH == nonAnnotationRegisterLevel || CalculateOperatorSymbolLevel.ADD_SUB == nonAnnotationRegisterLevel))){
            jsonSqlContext.registerLowOperatorSymbolFunction(symbol,method);
        }
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
    public static boolean checkOperatorMethod(Method method) {
        Parameter[] parameters = method.getParameters();
        if(ObjectUtil.isEmpty(parameters)){
            return false;
        }
        return parameters.length == 2;
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
