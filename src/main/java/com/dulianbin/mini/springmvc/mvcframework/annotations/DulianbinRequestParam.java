package com.dulianbin.mini.springmvc.mvcframework.annotations;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DulianbinRequestParam {
    String value() default "";
}
