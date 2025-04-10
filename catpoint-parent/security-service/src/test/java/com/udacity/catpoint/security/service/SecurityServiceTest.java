package com.udacity.catpoint.security.service;

import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService securityService;

    @Mock private SecurityRepository repository;
    @Mock private ImageService imageProcessor;
    @Mock private StatusListener listenerOne;
    @Mock private StatusListener listenerTwo;
    @Mock private StatusListener extraListener;

    @BeforeEach
    void init() {
        securityService = new SecurityService(repository, imageProcessor);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void whenSystemArmedAndSensorTriggered_thenSetPendingAlarm(ArmingStatus status) {
        when(repository.getArmingStatus()).thenReturn(status);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor sensor = new Sensor("Entry Door", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(repository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void whenPendingAlarmAndSensorTriggered_thenEscalateToAlarm(ArmingStatus status) {
        when(repository.getArmingStatus()).thenReturn(status);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("Window Sensor", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(repository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifAlarmIsActive_thenSensorChangeHasNoImpact() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        Sensor sensor = new Sensor("Hall Sensor", SensorType.DOOR);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(repository, never()).setAlarmStatus(any());
    }

    @Test
    void inactiveSensorDeactivated_noAlarmUpdate() {
        Sensor sensor = new Sensor("Window", SensorType.DOOR);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(repository, never()).setAlarmStatus(any());
    }

    @Test
    void whenPendingAlarmAndNoSensorsActive_thenResetToNoAlarm() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(repository.getSensors()).thenReturn(new HashSet<>());
        securityService.checkSensorsAndUpdateStatus();
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void reactivatingSensorDuringPendingAlarm_triggersAlarm() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        Sensor sensor = new Sensor("Back Door", SensorType.DOOR);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(repository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifCatDetectedAndSystemIsArmedHome_thenAlarmTriggered() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        verify(repository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void disarmingSystem_shouldResetToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifNoCatAndNoSensorsActive_thenKeepSystemUnarmed() {
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(repository.getSensors()).thenReturn(new HashSet<>());
        securityService.processImage(new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB));
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void settingSystemToArmed_shouldDeactivateAllSensors() {
        Sensor s1 = new Sensor("Main Door", SensorType.DOOR);
        Sensor s2 = new Sensor("Window", SensorType.WINDOW);
        s1.setActive(true);
        s2.setActive(true);
        when(repository.getSensors()).thenReturn(Set.of(s1, s2));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertFalse(s1.getActive());
        assertFalse(s2.getActive());
        verify(repository).updateSensor(s1);
        verify(repository).updateSensor(s2);
    }

    @Test
    void deactivatingSensorWhilePendingAlarm_shouldCheckOtherSensors() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(repository.getSensors()).thenReturn(new HashSet<>());
        Sensor sensor = new Sensor("Motion Sensor", SensorType.DOOR);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(repository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void removingSensor_shouldDelegateToRepository() {
        Sensor sensor = new Sensor("Old Sensor", SensorType.DOOR);
        securityService.removeSensor(sensor);
        verify(repository).removeSensor(sensor);
    }

    @Test
    void catDetectionWhileArmedAway_shouldNotTriggerAlarm() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(repository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void listenersShouldBeInformedOnCatDetection() {
        securityService.addStatusListener(listenerOne);
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(listenerOne).catDetected(true);
    }

    @Test
    void armedHomeAfterCatDetection_shouldTriggerAlarmImmediately() {
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(repository).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void removingNonExistentListener_shouldNotBreakNotifications() {
        securityService.addStatusListener(listenerOne);
        securityService.removeStatusListener(extraListener);
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(listenerOne).catDetected(true);
    }

    @Test
    void removedListeners_shouldNotReceiveUpdates() {
        securityService.addStatusListener(listenerTwo);
        securityService.removeStatusListener(listenerTwo);
        when(imageProcessor.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(listenerTwo, never()).catDetected(anyBoolean());
    }

    @Test
    void armingSystem_shouldDeactivateAllTypesOfSensors() {
        Sensor s1 = new Sensor("D1", SensorType.DOOR);
        Sensor s2 = new Sensor("W1", SensorType.WINDOW);
        Sensor s3 = new Sensor("M1", SensorType.MOTION);
        s1.setActive(true);
        s2.setActive(true);
        s3.setActive(false);
        when(repository.getSensors()).thenReturn(Set.of(s1, s2, s3));
        doAnswer(invocation -> {
            Sensor sensor = invocation.getArgument(0);
            sensor.setActive(false);
            return null;
        }).when(repository).updateSensor(any(Sensor.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertFalse(s1.getActive());
        assertFalse(s2.getActive());
        assertFalse(s3.getActive());
        verify(repository).updateSensor(s1);
        verify(repository).updateSensor(s2);
        verify(repository).updateSensor(s3);
    }
}
