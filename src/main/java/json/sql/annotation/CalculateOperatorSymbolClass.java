package json.sql.annotation;

import java.lang.annotation.*;

/**
 * 自定义计算运算符函数
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CalculateOperatorSymbolClass {

    /**
     * 描述信息
     * @return 描述信息
     */
    String desc() default "";

}
