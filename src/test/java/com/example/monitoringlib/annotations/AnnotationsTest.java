package com.example.monitoringlib.annotations;

import com.example.monitoringlib.classes.TestClassService;
import com.example.monitoringlib.enums.MeterType;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class AnnotationsTest {
    @Autowired
    private TestClassService testClassService;
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testGauge() {
        testClassService.testGaugeMeterType("test", "val");
        List<io.micrometer.core.instrument.Tag> expectedTags = new ArrayList<>(
                Arrays.asList(io.micrometer.core.instrument.Tag.of("testTag", "test")));
        Assertions.assertTrue(isMeterRecorded("test", 0d, expectedTags,MeterType.GAUGE));
    }

    @Test
    void testTimer() {
        testClassService.testTimerMeterType(100);
        List<io.micrometer.core.instrument.Tag> expectedTags = new ArrayList<>(
                Arrays.asList(io.micrometer.core.instrument.Tag.of("input", "100"),
                        io.micrometer.core.instrument.Tag.of("output", "method executed")));
        Assertions.assertTrue(isMeterRecorded("timer", 0D, expectedTags,MeterType.TIMER));
    }

    @Test
    void testCounter() {
        testClassService.testCounterMeterType();
        testClassService.testCounterMeterType();
        List<io.micrometer.core.instrument.Tag> expectedTags = new ArrayList<>(
                Arrays.asList(io.micrometer.core.instrument.Tag.of("status", "counting...")));
        Assertions.assertTrue(isMeterRecorded("counter", 2D, expectedTags,MeterType.COUNTER));
    }

    private boolean isMeterRecorded(String meterName, double meterValue, List<Tag> expectedTags, MeterType meterType) {
        Optional<Meter> actualMeter = meterRegistry.getMeters().stream().filter(meter -> meter.getId().getName().equals(meterName)).findFirst();
        boolean equalValues = false;
        if (!meterType.equals(MeterType.TIMER))
            for (Measurement v : actualMeter.get().measure()) {
                if (v.getValue() == meterValue) {
                    equalValues = true;
                    break;
                }
            }
        if ((!meterType.equals(MeterType.TIMER) &&!equalValues) || actualMeter.isEmpty() || !checkTags(expectedTags, actualMeter.get()))
            return false;
        return true;
    }//io.micrometer.core.instrument.Tag.of("testTag","test")

    private boolean checkTags(List<Tag> expectedTags, Meter meter) {
        List<Tag> meterActualTags = meter.getId().getTags();
        if (meterActualTags.size() != expectedTags.size())
            return false;
        for (int i = 0; i < expectedTags.size(); i++) {
            boolean tagFound = false;
            for (int j = 0; j < meterActualTags.size(); j++) {
                if ((expectedTags.get(i).getKey().equals(meterActualTags.get(i).getKey()) && expectedTags.get(i).getValue().equals(meterActualTags.get(i).getValue()))) {
                    tagFound = true;
                    break;
                }
            }
            if (!tagFound)
                return false;
        }
        return true;
    }
}
