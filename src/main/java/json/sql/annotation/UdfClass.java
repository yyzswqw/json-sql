package json.sql.annotation;



import java.lang.annotation.*;

/**
 * udf函数定义
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface UdfClass {

    /**
     * 参数描述信息
     * @return 参数描述信息
     */
    String desc() default "";

    /**
     * 是否忽略解析udf 来自哪个class
     * @return 是否忽略
     */
    boolean ignoreSourceClass() default false;

}
