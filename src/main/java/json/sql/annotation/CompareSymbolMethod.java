package json.sql.annotation;

import java.lang.annotation.*;

/**
 * 自定义运算符函数
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompareSymbolMethod {

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
