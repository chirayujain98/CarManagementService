package com.example.carServiceServ.controller;

import com.example.carServiceServ.dto.AppointmentRequest;
import com.example.carServiceServ.dto.TimeSlot;
import com.example.carServiceServ.entities.Appointment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.carServiceServ.service.AppointmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // 1. Book an appointment
    @PostMapping("/book")
    public ResponseEntity<Appointment> bookAppointment(@RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentService.bookAppointment(
                request.getCustomerId(), request.getOperatorId(), request.getStartTime());
        return new ResponseEntity<>(appointment, HttpStatus.CREATED);
    }

    // 2. Reschedule an appointment
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(@PathVariable Long id, @RequestBody String request) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(request);
        String newTime = jsonNode.get("newTime").asText();
        Appointment appointment = appointmentService.rescheduleAppointment(id, LocalDateTime.parse(newTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return ResponseEntity.ok(appointment);
    }

    // 3. Cancel an appointment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    // 4. View booked appointments for a specific operator
    @GetMapping("/operator/{operatorId}/booked")
    public ResponseEntity<List<Appointment>> getBookedAppointments(@PathVariable Long operatorId) {
        List<Appointment> appointments = appointmentService.getBookedAppointments(operatorId);
        return ResponseEntity.ok(appointments);
    }

    // 5. View open slots for a specific operator
    @GetMapping("/operator/{operatorId}/open-slots")
    public ResponseEntity<List<TimeSlot>> getOpenSlots(@PathVariable Long operatorId, @RequestParam LocalDate date) {
        List<TimeSlot> openSlots = appointmentService.getAvailableSlots(operatorId, date);
        return ResponseEntity.ok(openSlots);
    }
}

