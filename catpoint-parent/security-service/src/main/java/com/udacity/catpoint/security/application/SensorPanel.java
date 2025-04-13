package com.udacity.catpoint.security.application;

import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.udacity.catpoint.security.data.AlarmStatus;
import com.udacity.catpoint.security.data.Sensor;
import com.udacity.catpoint.security.data.SensorType;
import com.udacity.catpoint.security.service.SecurityService;
import com.udacity.catpoint.security.service.StyleService;

import net.miginfocom.swing.MigLayout;

public final class SensorPanel extends JPanel implements StatusListener {

    private final SecurityService securityService;
    private final JLabel panelLabel = new JLabel("Sensor Management");
    private final JLabel newSensorName = new JLabel("Name:");
    private final JLabel newSensorType = new JLabel("Sensor Type:");
    private final JTextField newSensorNameField = new JTextField();
    private final JComboBox<SensorType> newSensorTypeDropdown = new JComboBox<>(SensorType.values());
    private final JButton addNewSensorButton = new JButton("Add New Sensor");
    private final JPanel sensorListPanel;
    private final JPanel newSensorPanel;

    public SensorPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());
        this.securityService = Objects.requireNonNull(securityService, "SecurityService cannot be null");
        securityService.addStatusListener(this);
        panelLabel.setFont(StyleService.HEADING_FONT);
        addNewSensorButton.addActionListener(e -> addSensor(
                new Sensor(
                        newSensorNameField.getText(),
                        (SensorType) newSensorTypeDropdown.getSelectedItem()
                )
        ));

        this.newSensorPanel = buildAddSensorPanel();
        this.sensorListPanel = createSensorListPanel();
        add(panelLabel, "wrap");
        add(newSensorPanel, "span");
        add(sensorListPanel, "span");
        updateSensorList();
    }

    private JPanel createSensorListPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout());
        updateSensorList(panel);
        return panel;
    }
    private JPanel buildAddSensorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout());
        panel.add(newSensorName);
        panel.add(newSensorType);
        panel.add(newSensorTypeDropdown, "wrap");
        panel.add(addNewSensorButton, "span 3");
        panel.add(newSensorNameField, "width 50:100:200");
       
        return panel;
    }
    private void updateSensorList() {
        updateSensorList(sensorListPanel);
    }
    private void updateSensorList(JPanel panel) {
        panel.removeAll();
        securityService.getSensors().stream()
                .sorted()
                .forEach(sensor -> {
                    JLabel sensorLabel = new JLabel(String.format("%s(%s): %s",
                            sensor.getName(),
                            sensor.getSensorType(),
                            sensor.getActive() ? "Active" : "Inactive"));

                    JButton toggleButton = new JButton(sensor.getActive() ? "Deactivate" : "Activate");
                    JButton removeButton = new JButton("Remove Sensor");
                    toggleButton.addActionListener(e -> setSensorActivity(sensor, !sensor.getActive()));
                    removeButton.addActionListener(e -> removeSensor(sensor));
                    panel.add(sensorLabel, "width 300:300:300");
                    panel.add(toggleButton, "width 100:100:100");
                    panel.add(removeButton, "wrap");
                });

        panel.repaint();
        panel.revalidate();
    }
    private void setSensorActivity(Sensor sensor, boolean isActive) {
        securityService.changeSensorActivationStatus(sensor, isActive);
        updateSensorList();
    }
    private void addSensor(Sensor sensor) {
        if (securityService.getSensors().size() < 4) {
            securityService.addSensor(sensor);
            updateSensorList();
        } else {
            JOptionPane.showMessageDialog(null,
                    "please subscribe to our Premium Membership!, for adding more than 4 sensores......");
        }
    }
    private void removeSensor(Sensor sensor) {
        securityService.removeSensor(sensor);
        updateSensorList();
    }
    
    @Override
    public void notify(AlarmStatus status) {
        // no behavior necessary
    }

    @Override
    public void catDetected(boolean catDetected) {
        // no behavior necessary
    }

    @Override
    public void sensorStatusChanged() {
        updateSensorList();
    }
}