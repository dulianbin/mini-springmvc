package com.dulianbin.mini.springmvc.mvcframework.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DulianbinRequestMapping {

    String value() default "";
}
