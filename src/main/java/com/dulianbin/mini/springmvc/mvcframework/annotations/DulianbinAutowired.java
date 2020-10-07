package com.dulianbin.mini.springmvc.mvcframework.annotations;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DulianbinAutowired {
    String value() default "";
}
