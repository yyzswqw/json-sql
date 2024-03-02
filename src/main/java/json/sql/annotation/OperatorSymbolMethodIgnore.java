package json.sql.annotation;



import java.lang.annotation.*;

/**
 * 忽略注册运算符函数
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperatorSymbolMethodIgnore {

}
