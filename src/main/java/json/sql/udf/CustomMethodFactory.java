package json.sql.udf;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.sql.annotation.*;
import json.sql.enums.CalculateOperatorSymbolLevel;
import json.sql.lister.LifecycleListener;
import lombok.extern.slf4j.Slf4j;
import org.reflections.util.ClasspathHelper;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
public class CustomMethodFactory {

    private static final Map<String,Set<Method>> udfMethodsByAnnotationCache = new LinkedHashMap<>();
    private static final Map<String,Set<Class<?>>> udfMethodsByClassCache = new LinkedHashMap<>();
    private static final Map<String,Set<Method>> compareSymbolMethodByAnnotationCache = new LinkedHashMap<>();
    private static final Map<String,Set<Class<?>>> compareSymbolMethodByClassCache = new LinkedHashMap<>();
    private static final Map<String,Set<Method>> calculateOperatorSymbolMethodByAnnotationCache = new LinkedHashMap<>();
    private static final Map<String,Set<Class<?>>> calculateOperatorSymbolMethodByClassCache = new LinkedHashMap<>();

    private static final ReentrantLock UDF_METHODS_LOCK = new ReentrantLock();
    private static final ReentrantLock COMPARE_SYMBOL_METHOD_LOCK = new ReentrantLock();
    private static final ReentrantLock CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK = new ReentrantLock();

    /**
     * 注册udf函数
     * @param jsonSqlContext 上下文
     */
    public static void registerClassPathCustomMethod(JsonSqlContext jsonSqlContext) {
        registerCustomMethod(jsonSqlContext,"classPath",ClasspathHelper.forJavaClassPath());
        UdfParser.classParser(jsonSqlContext,ObjectUtil.class, false,false, (Method[])null);
        UdfParser.classParser(jsonSqlContext, DateUtil.class, false,false, (Method[])null);
        List<LifecycleListener> lifecycleListener = jsonSqlContext.getLifecycleListener();
        lifecycleListener.forEach(listener -> listener.innerRegisterUdfFinish(jsonSqlContext));
    }

    /**
     * 注册udf函数
     * @param jsonSqlContext 上下文
     * @param pathName 扫描的路径全局唯一名称
     * @param urls urls
     */
    public static void registerCustomMethod(JsonSqlContext jsonSqlContext,String pathName,Collection<URL> urls) {
        Set<Method> methods = udfMethodsByAnnotationCache.get(pathName);
        Set<Method> udfMethods = methods;
        if(ObjectUtil.isEmpty(methods)){
            if(ObjectUtil.isNull(udfMethods)){
                try {
                    UDF_METHODS_LOCK.lock();
                    methods = PackageAnnotationScanner.scanMethodByAnnotationInUrls(UdfMethod.class,urls);
                    udfMethodsByAnnotationCache.put(pathName,methods);
                }finally {
                    UDF_METHODS_LOCK.unlock();
                }
            }
            udfMethods = methods;
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
        }
        Method[] ignoreMethods = null;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            ignoreMethods = udfMethods.toArray(new Method[0]);
        }
        Set<Class<?>> classes1 = udfMethodsByClassCache.get(pathName);
        if(ObjectUtil.isNotEmpty(classes1)){
            Set<Class<?>> classes = classes1;
            if(ObjectUtil.isNull(classes)){
                try {
                    UDF_METHODS_LOCK.lock();
                    classes1 = PackageAnnotationScanner.scanClassesByAnnotationInUrls(UdfClass.class,urls);
                    udfMethodsByClassCache.put(pathName,classes1);
                }finally {
                    UDF_METHODS_LOCK.unlock();
                }
            }
            classes = classes1;
            if(ObjectUtil.isNotEmpty(classes)){
                for (Class<?> aClass : classes) {
                    UdfParser.classParser(jsonSqlContext,aClass, false,false, ignoreMethods);
                }
            }
        }
    }


    /**
     * 注册比较运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerClassPathCompareSymbolMethod(JsonSqlContext jsonSqlContext) {
        registerCompareSymbolMethod(jsonSqlContext,"classPath", ClasspathHelper.forJavaClassPath());
    }
    /**
     * 注册比较运算符
     * @param jsonSqlContext 上下文
     * @param pathName 扫描的路径全局唯一名称
     * @param urls urls
     */
    public static void registerCompareSymbolMethod(JsonSqlContext jsonSqlContext,String pathName,Collection<URL> urls) {
        Set<Method> methods = compareSymbolMethodByAnnotationCache.get(pathName);
        Set<Method> udfMethods = methods;
        if(ObjectUtil.isNotEmpty(methods)){
            try {
                COMPARE_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(udfMethods)){
                    udfMethods = PackageAnnotationScanner.scanMethodByAnnotationInUrls(CompareSymbolMethod.class,urls);
                    compareSymbolMethodByAnnotationCache.put(pathName, udfMethods);
                }
            }finally {
                COMPARE_SYMBOL_METHOD_LOCK.unlock();
            }
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
        }

        Method[] ignoreMethods = null;
        if(ObjectUtil.isNotEmpty(udfMethods)){
            ignoreMethods = udfMethods.toArray(new Method[0]);
        }
        Set<Class<?>> classes1 = compareSymbolMethodByClassCache.get(pathName);
        if(ObjectUtil.isNotEmpty(classes1)){
            try {
                COMPARE_SYMBOL_METHOD_LOCK.lock();
                if(ObjectUtil.isNull(classes1)){
                    classes1 = PackageAnnotationScanner.scanClassesByAnnotationInUrls(CompareSymbolClass.class,urls);
                    compareSymbolMethodByClassCache.put(pathName,classes1);
                }
            }finally {
                COMPARE_SYMBOL_METHOD_LOCK.unlock();
            }
            if(ObjectUtil.isNotEmpty(classes1)){
                for (Class<?> aClass : classes1) {
                    CompareSymbolParser.classParser(jsonSqlContext,aClass, false,false, ignoreMethods);
                }
            }
        }

    }

    /**
     * 注册计算运算符
     * @param jsonSqlContext 上下文
     */
    public static void registerClassPathCalculateOperatorSymbolMethod(JsonSqlContext jsonSqlContext) {
        registerCalculateOperatorSymbolMethod(jsonSqlContext,"classPath",ClasspathHelper.forJavaClassPath());
    }

    /**
     * 注册计算运算符
     * @param jsonSqlContext 上下文
     * @param pathName 扫描的路径全局唯一名称
     * @param urls urls
     */
    public static void registerCalculateOperatorSymbolMethod(JsonSqlContext jsonSqlContext,String pathName,Collection<URL> urls) {
        Set<Method> udfMethods = new LinkedHashSet<>();
        Set<Method> methods = calculateOperatorSymbolMethodByAnnotationCache.get(pathName);

        if(ObjectUtil.isNotNull(methods)){
            udfMethods = methods;
        }else {
            try {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.lock();
                Set<Method> highUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInUrls(HighOperatorSymbolMethod.class,urls);
                Set<Method> lowUdfMethods = PackageAnnotationScanner.scanMethodByAnnotationInUrls(LowOperatorSymbolMethod.class,urls);
                if(ObjectUtil.isNotEmpty(highUdfMethods)){
                    udfMethods.addAll(highUdfMethods);
                }
                if(ObjectUtil.isNotEmpty(lowUdfMethods)){
                    udfMethods.addAll(lowUdfMethods);
                }
                calculateOperatorSymbolMethodByAnnotationCache.put(pathName,new HashSet<>());
                if(ObjectUtil.isNotEmpty(udfMethods)){
                    calculateOperatorSymbolMethodByAnnotationCache.get(pathName).addAll(udfMethods);
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

        Set<Class<?>> classes = calculateOperatorSymbolMethodByClassCache.get(pathName);
        if(ObjectUtil.isNull(classes)){
            try {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.lock();
                classes = PackageAnnotationScanner.scanClassesByAnnotationInUrls(CalculateOperatorSymbolClass.class,urls);
                calculateOperatorSymbolMethodByClassCache.put(pathName,classes);
            }finally {
                CALCULATE_OPERATOR_SYMBOL_METHOD_LOCK.unlock();
            }
        }
        if(ObjectUtil.isNotEmpty(classes)){
            for (Class<?> aClass : classes) {
                OperatorSymbolParser.classParser(jsonSqlContext,aClass,CalculateOperatorSymbolLevel.BOTH ,false,ignoreMethods);
            }
        }
    }
}
