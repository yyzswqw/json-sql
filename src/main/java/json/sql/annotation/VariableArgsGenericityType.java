package json.sql.annotation;

import java.lang.annotation.*;

/**
 * 可变参数泛型类型定义
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface VariableArgsGenericityType {

    /**
     * 获取参数类型
     * @return class
     */
    Class<?> argType() default Object.class;

    /**
     * 获取参数第一个泛型类型，Collection、Map中key的泛型
     * @return class
     */
    Class<?> argGenericityType1();

    /**
     * 获取参数第二个泛型类型，Map中value的泛型
     * @return class
     */
    Class<?> argGenericityType2();

}
