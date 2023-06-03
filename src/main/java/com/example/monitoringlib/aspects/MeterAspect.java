package com.example.monitoringlib.aspects;

import com.example.monitoringlib.annotations.Meter;
import com.example.monitoringlib.annotations.Tag;
import com.example.monitoringlib.annotations.Value;
import com.example.monitoringlib.enums.RecordingTime;
import com.example.monitoringlib.enums.Source;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Aspect
@Component
public class MeterAspect {
    Logger logger = LoggerFactory.getLogger(MeterAspect.class);
    @Autowired
    private MeterRegistry meterRegistry;

    @Before(value = "@annotation(com.example.monitoringlib.annotations.Meter)")
    public void logMethodExecutionInfo(JoinPoint joinPoint)  {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        logger.info("full method description:{} ", signature.getMethod());
        logger.info("method name: {}", signature.getMethod().getName());
        logger.info("declaring type: {}", signature.getDeclaringType());
    }

    @Pointcut("execution(@com.example.monitoringlib.annotations.Meter * *.*(..))")
    public void anyMethodExecutionAnnotatedWithMeter() {
    }

    @Around("anyMethodExecutionAnnotatedWithMeter()")
    public void aroundMethodExec(ProceedingJoinPoint joinPoint) throws Throwable {
        Signature signature = joinPoint.getSignature();
        logMethodExecutionInfo(joinPoint);
        if (!(signature instanceof MethodSignature)) {
            joinPoint.proceed();
            // throw new SignatureNotAllowedException("invalid signature");
        }
        MethodSignature methodSignature = (MethodSignature) signature;
        List<Meter> annotations = getAnnotations(Meter.class, methodSignature.getMethod());
        processAnnotations(annotations,joinPoint,methodSignature.getMethod());

    }

    private Map<String, Object> getMethodArguments(Parameter[] parameters, Object[] args) {
        Map<String, Object> argsMap = new HashMap<>(parameters.length);
        for (int i = 0; i < parameters.length; i++) {
            argsMap.put(parameters[i].getName(), args[i]);
        }
        return argsMap;
    }

    private <A extends Annotation> List<A> getAnnotations(Class<A> annotationClass, Method method) {
        return Arrays.stream(method.getAnnotationsByType(annotationClass)).filter(Objects::nonNull).
                collect(Collectors.toUnmodifiableList());
    }

    private void processAnnotations(List<Meter> annotations, ProceedingJoinPoint joinPoint, Method method) throws Throwable {
        // 1-tags , 2-metername , 3- description
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();
        Map<String, Object> argsMap = getMethodArguments(parameters, args);
        annotations.stream().filter(annotation -> annotation.recordingTime() == RecordingTime.BEFORE).forEach(e -> recordMeter((Meter) e,argsMap,null,-1L));//duration =--1 as the method execution not yet started
        long startInNanos=System.nanoTime();
        Object outcome= joinPoint.proceed();
        long durationInNanos=System.nanoTime()-startInNanos;
        annotations.stream().filter(annotation -> annotation.recordingTime() == RecordingTime.AFTER).forEach(e -> recordMeter((Meter) e,argsMap,outcome,durationInNanos));
        }

    public void recordMeter(Meter meter,Map<String,Object> argsMap,Object outcome,long duration) {

        Optional<Object> gaugeValue = getValue(meter.value(),argsMap,outcome);
        Tag[] tags = meter.tags();
        String meterName = meter.name();

        switch (meter.meterType()) {
            case GAUGE:
                recordGauge(meterName,tags,toDoubleValue(gaugeValue.isEmpty()?null:gaugeValue.get()),argsMap,outcome);
                break;
            case TIMER:
                recordTimer(meterName,tags,argsMap,outcome,duration);
                break;
            case COUNTER:
                recordCounter(meterName,tags,argsMap,outcome);
        }


        // meterRegistry.
    }
    private double toDoubleValue(Object gaugeValue){
        if(gaugeValue==null)
            return 0d;
        if(gaugeValue instanceof Number){
            return (double) gaugeValue;
        }
        if(gaugeValue instanceof String){
            try {
                double val = Double.parseDouble((String) gaugeValue);
                return val;
            }
            catch(Exception e){
                logger.error("exception {} is thrown while parsing string to double",e);
            }
        }
        return 0d;
    }

    private Optional<Object> getValue(Value value, Map<String, Object> argsMap,Object outcome) {

        Source source = value.source();
        String valueName= value.name();

        switch (source) {
            case ARGUMENT:
                return Optional.ofNullable(argsMap.get(valueName));
            case CONSTANT:
                return Optional.ofNullable(value.constant());
            case NONE:
                return Optional.empty();
            case CLOCK:
                return Optional.of(LocalTime.now().toString());
            case OUTCOME:
              return Optional.ofNullable(outcome);
        }
        return Optional.empty();
    }
    void recordTimer(String meterName, Tag[] tags, Map<String, Object> argsMap, Object outcome, long duration) {
        Timer timer = null;
        timer=Timer.builder(meterName).tags(toMeterTag(tags,argsMap,outcome)).register(meterRegistry);
        timer.record(()->duration);
    }
    void recordCounter(String meterName , Tag[] tags,Map<String, Object> argsMap, Object outcome) {
        Counter counter = null;
        counter=Counter.builder(meterName).tags(toMeterTag(tags,argsMap,outcome)).register(meterRegistry);
        counter.increment();
    }
    void recordGauge(String meterName, Tag[] tags, double doubleValue, Map<String, Object> argsMap, Object outcome) {
        Gauge gauge = null;
        gauge=Gauge.builder(meterName,()->doubleValue).tags(toMeterTag(tags,argsMap,outcome)).register(meterRegistry);
    }
    private List<io.micrometer.core.instrument.Tag> toMeterTag(Tag[] tags,Map<String, Object> argsMap,Object outcome){
        return Arrays.stream(tags).map(e->io.micrometer.core.instrument.Tag.of(e.name(),getValue(e.value(),argsMap,outcome).map(
                String::valueOf).orElse(""))).collect(Collectors.toList());
    }
}
