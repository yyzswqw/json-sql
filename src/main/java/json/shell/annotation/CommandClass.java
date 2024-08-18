package json.shell.annotation;



import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandClass {

    /**
     * 参数描述信息
     * @return 参数描述信息
     */
    String desc() default "";

}
