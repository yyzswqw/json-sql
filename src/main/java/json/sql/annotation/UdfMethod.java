package json.sql.annotation;



import java.lang.annotation.*;

/**
 * udf函数定义
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UdfMethod {

    /**
     * 注册的函数名
     * @return 函数名称
     */
    String functionName() default "";

    /**
     * 描述信息
     * @return 描述信息
     */
    String desc() default "";

    /**
     * 是否忽略解析udf 来自哪个class
     * @return 是否忽略
     */
    boolean ignoreSourceClass() default false;

}
