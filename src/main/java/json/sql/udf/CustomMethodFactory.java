package json.sql.udf;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.sql.annotation.*;
import json.sql.enums.CalculateOperatorSymbolLevel;
import json.sql.lister.LifecycleListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class CustomMethodFactory {

    private static Set<Method> udfMethodsByAnnotationCache;
    private static Set<Class<?>> udfMethodsByClassCache;
    private static Set<Method> compareSymbolMethodByAnnotationCache;
    private static Set<Class<?>> compareSymbolMethodByClassCache;
    private static Set<Method> calculateOperatorSymbolMethodByAnnotationCache;
    private static Set<Class<?>> calculateOperatorSymbolMethodByClassCache;

    private static final ReentrantLock UDF_METHODS_LOCK = new ReentrantLock();
    private static final ReentrantLock COMPARE_SYMBOL_METHOD_LOCK = new ReentrantLock();
    private static final ReentrantLock CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK = new ReentrantLock();

    /**
     * 注册udf函数
     * @param jsonSqlContext 上下文
     */
    public static void registerCustomMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> udfMethods = udfMethodsByAnnotationCache;
        if(ObjectUtil.isNull(udfMethods)){
            try {
                UDF_METHODS_LOCK.lock();
                if(ObjectUtil.isNull(udfMethodsByAnnotationCache)){
                    udfMethodsByAnnotationCache = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(UdfMethod.class);
                }
            }finally {
                UDF_METHODS_LOCK.unlock();
            }
        }
        udfMethods = udfMethodsByAnnotationCache;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    UdfParser.registerUdfMethod(jsonSqlContext,udfMethod,false);
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
        Set<Class<?>> classes = udfMethodsByClassCache;
        if(ObjectUtil.isNull(classes)){
            try {
                UDF_METHODS_LOCK.lock();
                if(ObjectUtil.isNull(udfMethodsByClassCache)){
                    udfMethodsByClassCache = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(UdfClass.class);
                }
            }finally {
                UDF_METHODS_LOCK.unlock();
            }
        }
        classes = udfMethodsByClassCache;
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                UdfParser.classParser(jsonSqlContext,aClass, false,false, ignoreMethods);
            }
        }
        UdfParser.classParser(jsonSqlContext,ObjectUtil.class, false,false, (Method[])null);
        UdfParser.classParser(jsonSqlContext, DateUtil.class, false,false, (Method[])null);
        List<LifecycleListener> lifecycleListener = jsonSqlContext.getLifecycleListener();
        lifecycleListener.forEach(listener -> listener.innerRegisterUdfFinish(jsonSqlContext));
    }

    /**
     * 注册比较运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerCompareSymbolMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> udfMethods = compareSymbolMethodByAnnotationCache;
        if(ObjectUtil.isNull(udfMethods)){
            try {
                COMPARE_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(compareSymbolMethodByAnnotationCache)){
                    compareSymbolMethodByAnnotationCache = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(CompareSymbolMethod.class);
                }
            }finally {
                COMPARE_SYMBOL_METHOD_LOCK.unlock();
            }
        }
        udfMethods = compareSymbolMethodByAnnotationCache;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    CompareSymbolParser.registerCompareSymbolMethod(jsonSqlContext,udfMethod,false);
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
        Set<Class<?>> classes = compareSymbolMethodByClassCache;
        if(ObjectUtil.isNull(classes)){
            try {
                COMPARE_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(compareSymbolMethodByClassCache)){
                    compareSymbolMethodByClassCache = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CompareSymbolClass.class);
                }
            }finally {
                COMPARE_SYMBOL_METHOD_LOCK.unlock();
            }
        }
        classes = compareSymbolMethodByClassCache;
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                CompareSymbolParser.classParser(jsonSqlContext,aClass, false,false, ignoreMethods);
            }
        }
    }

    /**
     * 注册计算运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerCalculateOperatorSymbolMethod(JsonSqlContext jsonSqlContext) {
        Set<Method> udfMethods = new LinkedHashSet<>();
        if(ObjectUtil.isNotNull(calculateOperatorSymbolMethodByAnnotationCache)){
            udfMethods = calculateOperatorSymbolMethodByAnnotationCache;
        }else {
            try {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(calculateOperatorSymbolMethodByAnnotationCache)){
                    Set<Method> highUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(HighOperatorSymbolMethod.class);
                    Set<Method> lowUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInClasspath(LowOperatorSymbolMethod.class);
                    if(ObjectUtil.isNotEmpty(highUdfMethods)){
                        udfMethods.addAll(highUdfMethods);
                    }
                    if(ObjectUtil.isNotEmpty(lowUdfMethods)){
                        udfMethods.addAll(lowUdfMethods);
                    }
                    calculateOperatorSymbolMethodByAnnotationCache = new HashSet<>();
                    if(ObjectUtil.isNotEmpty(udfMethods)){
                        calculateOperatorSymbolMethodByAnnotationCache.addAll(udfMethods);
                    }
                }
            }finally {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.unlock();
            }
        }
        if(ObjectUtil.isNotEmpty(udfMethods)){
            for (Method udfMethod : udfMethods) {
                try {
                    OperatorSymbolParser.registerOperatorSymbolMethod(jsonSqlContext,udfMethod, CalculateOperatorSymbolLevel.NONE,false);
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
        Set<Class<?>> classes = calculateOperatorSymbolMethodByClassCache;
        if(ObjectUtil.isNull(classes)){
            try {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(calculateOperatorSymbolMethodByClassCache)){
                    calculateOperatorSymbolMethodByClassCache = PackageAnnotationScanner.scanClassesByAnnotationInClasspath(CalculateOperatorSymbolClass.class);
                }
            }finally {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.unlock();
            }
        }
        classes = calculateOperatorSymbolMethodByClassCache;
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                OperatorSymbolParser.classParser(jsonSqlContext,aClass,CalculateOperatorSymbolLevel.BOTH ,false,ignoreMethods);
            }
        }
    }
}
