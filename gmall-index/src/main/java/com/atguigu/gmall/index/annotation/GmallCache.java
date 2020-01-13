package com.atguigu.gmall.index.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GmallCache {

    String prefix() default "";

    int timeout() default 5;

    int bound() default 100;

    String lockName() default "lock";

}
