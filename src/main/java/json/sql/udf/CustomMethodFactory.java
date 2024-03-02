package json.sql.udf;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.sql.annotation.*;
import json.sql.enums.CalculateOperatorSymbolLevel;
import json.sql.lister.LifecycleListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@Slf4j
public class CustomMethodFactory {

    /**
     * 注册udf函数
     * @param jsonSqlContext 上下文
     */
    public static void registerCustomMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> udfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(UdfMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    UdfParser.registerUdfMethod(jsonSqlContext,udfMethod);
                }catch (Exception e){
                    Class<?>[] parameterTypes = udfMethod.getParameterTypes();
                    // 获取所在类的 Class 对象
                    Class<?> clazz = udfMethod.getDeclaringClass();
                    log.info("注册udf 函数失败!class : {} ,method : {} ,parameterTypes : {}",clazz.getName(),udfMethod.getName(),parameterTypes);
                    log.error("注册udf 函数失败!",e);
                }
            }
        }
        Method[] ignoreMethods = null;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            ignoreMethods = udfMethods.toArray(new Method[0]);
        }
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(UdfClass.class);
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                UdfParser.classParser(jsonSqlContext,aClass, false, ignoreMethods);
            }
        }
        UdfParser.classParser(jsonSqlContext,ObjectUtil.class, false, (Method[])null);
        UdfParser.classParser(jsonSqlContext, DateUtil.class, false, (Method[])null);
        List<LifecycleListener> lifecycleListener = jsonSqlContext.getLifecycleListener();
        lifecycleListener.forEach(listener -> listener.innerRegisterUdfFinish(jsonSqlContext));
    }

    /**
     * 注册比较运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerCompareSymbolMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> udfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(CompareSymbolMethod.class);
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    CompareSymbolParser.registerCompareSymbolMethod(jsonSqlContext,udfMethod);
                }catch (Exception e){
                    Class<?>[] parameterTypes = udfMethod.getParameterTypes();
                    // 获取所在类的 Class 对象
                    Class<?> clazz = udfMethod.getDeclaringClass();
                    log.info("注册运算符 函数失败!class : {} ,method : {} ,parameterTypes : {}",clazz.getName(),udfMethod.getName(),parameterTypes);
                    log.error("注册运算符 函数失败!",e);
                }
            }
        }
        Method[] ignoreMethods = null;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            ignoreMethods = udfMethods.toArray(new Method[0]);
        }
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CompareSymbolClass.class);
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                CompareSymbolParser.classParser(jsonSqlContext,aClass, false, ignoreMethods);
            }
        }
    }

    /**
     * 注册计算运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerCalculateOperatorSymbolMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> highUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(HighOperatorSymbolMethod.class);
        Set<Method> lowUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(LowOperatorSymbolMethod.class);
        Set<Method> udfMethods = new LinkedHashSet<>();
        if(ObjectUtil.isNotEmpty(highUdfMethods)){
            udfMethods.addAll(highUdfMethods);
        }
        if(ObjectUtil.isNotEmpty(lowUdfMethods)){
            udfMethods.addAll(lowUdfMethods);
        }
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    OperatorSymbolParser.registerOperatorSymbolMethod(jsonSqlContext,udfMethod, CalculateOperatorSymbolLevel.NONE);
                }catch (Exception e){
                    Class<?>[] parameterTypes = udfMethod.getParameterTypes();
                    // 获取所在类的 Class 对象
                    Class<?> clazz = udfMethod.getDeclaringClass();
                    log.info("注册计算运算符 函数失败!class : {} ,method : {} ,parameterTypes : {}",clazz.getName(),udfMethod.getName(),parameterTypes);
                    log.error("注册计算运算符 函数失败!",e);
                }
            }
        }
        Method[] ignoreMethods = null;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            ignoreMethods = udfMethods.toArray(new Method[0]);
        }
        Set<Class<?>> classes = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CalculateOperatorSymbolClass.class);
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                OperatorSymbolParser.classParser(jsonSqlContext,aClass,CalculateOperatorSymbolLevel.BOTH ,ignoreMethods);
            }
        }
    }
}
