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

}
