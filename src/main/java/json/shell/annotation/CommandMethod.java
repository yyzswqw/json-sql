package json.shell.annotation;

import java.lang.annotation.*;


@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMethod {

    String[] name() default {""};

    String desc() default "";

}
