package json.sql.annotation;

import java.lang.annotation.*;

/**
 * 自定义低优先级运算符函数，同加减优先级
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LowOperatorSymbolMethod {

    /**
     * 注册的运算符号
     * @return 运算符号
     */
    String symbol();

    /**
     * 描述信息
     * @return 描述信息
     */
    String desc() default "";

}
