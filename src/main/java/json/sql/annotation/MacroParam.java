package json.sql.annotation;


import json.sql.enums.MacroEnum;

import java.lang.annotation.*;

/**
 * udf宏参数定义
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MacroParam {

    /**
     * 宏参数类型
     * @return 宏参数类型
     */
    MacroEnum type();

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
