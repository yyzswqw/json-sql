package json.shell.annotation;



import java.lang.annotation.*;

/**
 * udf函数普通参数定义
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParam {

    /**
     * 自定义参数名称
     * @return 参数名称
     */
    String name() default "";

    /**
     * 参数描述信息
     * @return 参数描述信息
     */
    String desc() default "";

}
