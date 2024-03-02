package json.sql.annotation;

import java.lang.annotation.*;

/**
 * 自定义运算符函数
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompareSymbolClass {

    /**
     * 描述信息
     * @return 描述信息
     */
    String desc() default "";

}
