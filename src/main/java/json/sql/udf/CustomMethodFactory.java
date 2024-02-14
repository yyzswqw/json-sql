package json.sql.udf;

import cn.hutool.core.util.ObjectUtil;
import json.sql.JsonSqlContext;
import json.sql.annotation.PackageAnnotationScanner;
import json.sql.annotation.UdfClass;
import json.sql.annotation.UdfMethod;
import json.sql.annotation.UdfParser;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Set;


@Slf4j
public class CustomMethodFactory {

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
    }

}
