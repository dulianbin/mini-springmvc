package com.dulianbin.mini.springmvc.mvcframework.annotations;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DulianbinController {

    String value() default "";
}
