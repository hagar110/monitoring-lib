package com.example.monitoringlib.annotations;

import com.example.monitoringlib.enums.MeterType;
import com.example.monitoringlib.enums.RecordingTime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Meter {
    RecordingTime recordingTime() default RecordingTime.BEFORE;

    Value value() default @Value();

    String name() default "";
    MeterType meterType() default MeterType.GAUGE;
    Tag[] tags();
}
