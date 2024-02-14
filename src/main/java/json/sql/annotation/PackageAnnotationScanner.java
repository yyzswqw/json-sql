package json.sql.annotation;

import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

@Slf4j
public class PackageAnnotationScanner {

    private PackageAnnotationScanner(){

    }

    public static void main(String[] args) {
        Set<Method> list = scanMethodByAnnotationInClasspath(UdfMethod.class);
        for (Method method : list) {
            log.info(method.getName());
        }
    }

    /**
     * 扫描class Path下指定注解的类
     * @param annotationClass 指定注解
     * @return 指定注解的类
     */
    public static Set<Class<?>> scanClassesByAnnotationInClasspath(Class<? extends Annotation> annotationClass){
        // 设置 Reflections 配置
        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners(Scanners.SubTypes, Scanners.TypesAnnotated);
        // 创建 Reflections 实例
        Reflections reflections = new Reflections(configuration);
        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(annotationClass);
        // 过滤出不是接口、抽象类或枚举的类，这些都是不能实例化的，如果要能实例化的，还需要判断是否有公共的构造方法
        classes.removeIf(clazz -> clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum());
        return classes;
    }

    /**
     * 扫描 classpath下所有带有指定注解的公共静态（public static）方法
     * @param annotationClass 指定注解
     * @return 所有指定注解的公共静态（public static）方法
     */
    public static Set<Method> scanMethodByAnnotationInClasspath(Class<? extends Annotation> annotationClass){
        // 设置 Reflections 配置
        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forJavaClassPath())
                .setScanners( Scanners.MethodsAnnotated);
        // 创建 Reflections 实例
        Reflections reflections = new Reflections(configuration);
        Set<Method> methods = reflections.getMethodsAnnotatedWith(annotationClass);
        // 过滤出公共静态方法
        methods.removeIf(method -> !Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers()));
        return methods;
    }

    /**
     * 扫描指定 package下所有带有指定注解的公共静态（public static）方法
     * @param packageName 指定 package
     * @param annotationClass 指定注解
     * @return 所有指定注解的公共静态（public static）方法
     */
    public static Set<Method> scanMethod(String packageName, Class<? extends Annotation> annotationClass) {
        // 设置 Reflections 配置
        ConfigurationBuilder configuration = new ConfigurationBuilder()
                .forPackages(packageName)
                .setScanners(Scanners.MethodsAnnotated);
        // 创建 Reflections 实例
        Reflections reflections = new Reflections(configuration);
        Set<Method> methods = reflections.getMethodsAnnotatedWith(annotationClass);
        // 过滤出公共静态方法
        methods.removeIf(method -> !Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers()));
        return methods;
    }


}