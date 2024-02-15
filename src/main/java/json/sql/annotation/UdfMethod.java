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
     * 参数描述信息
     * @return 参数描述信息
     */
    String desc() default "";

}
