package com.example.monitoringlib.annotations;

import com.example.monitoringlib.enums.Source;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Value {
    Source source() default Source.NONE;
    String name() default "";
    String constant() default "";

}
