package json.sql.annotation;


import json.sql.enums.MacroEnum;

import java.lang.annotation.*;

/**
 * 宏参数定义
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

}
