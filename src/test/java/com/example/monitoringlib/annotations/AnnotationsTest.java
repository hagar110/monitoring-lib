package com.example.monitoringlib.annotations;

import com.example.monitoringlib.enums.RecordingTime;

public class AnnotationsTest {
  //  @Meter(getName = "test",getRecordingTime = RecordingTime.ERROR)
  @Meter(name="test", recordingTime = RecordingTime.BEFORE)
  void test(){};
}
