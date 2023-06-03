package com.example.monitoringlib.classes;

import com.example.monitoringlib.annotations.Meter;
import com.example.monitoringlib.annotations.Tag;
import com.example.monitoringlib.annotations.Value;
import com.example.monitoringlib.enums.MeterType;
import com.example.monitoringlib.enums.RecordingTime;
import com.example.monitoringlib.enums.Source;
import org.springframework.stereotype.Service;

@Service
public class TestClassService {
    @Meter(name = "test",recordingTime = RecordingTime.BEFORE,
            tags = {@Tag(name = "testTag",value = @Value(name = "name",source = Source.ARGUMENT))})
    public Integer testGaugeMeterType(String name,String val){
        return 1;
    }
    @Meter(name="timer",recordingTime = RecordingTime.AFTER,meterType = MeterType.TIMER,tags = {
            @Tag(name="input",value = @Value(name = "number",source = Source.ARGUMENT)),
            @Tag(name = "output",value = @Value(name = "output",source = Source.OUTCOME))})
    public String testTimerMeterType(int number){
        for(int i=0;i<number;i++);
        return "method executed";
    }
    @Meter(name="counter",meterType = MeterType.COUNTER,tags = {
            @Tag(name = "status", value = @Value(constant = "counting...", source = Source.CONSTANT))
    })
    public void testCounterMeterType(){}

}
