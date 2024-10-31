package com.example.carServiceServ.service;

import com.example.carServiceServ.Excptions.NoAvailableOperatorException;
import com.example.carServiceServ.Excptions.ResourceNotFoundException;
import com.example.carServiceServ.dao.AppointmentRepository;
import com.example.carServiceServ.dao.ServiceOperatorRepository;
import com.example.carServiceServ.dto.TimeSlot;
import com.example.carServiceServ.entities.Appointment;
import com.example.carServiceServ.entities.AppointmentStatus;
import com.example.carServiceServ.entities.Customer;
import com.example.carServiceServ.entities.ServiceOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private ServiceOperatorRepository serviceOperatorRepository;

    // 1. Book an appointment with a specific operator or any available operator
    public Appointment bookAppointment(Long customerId, Long operatorId, LocalDateTime startTime) {
        ServiceOperator operator;

        if (operatorId != null) {
            operator = serviceOperatorRepository.findById(operatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Operator not found"));
        } else {
            // Find any available operator for the given time
            operator = serviceOperatorRepository.findAvailableOperator(startTime)
                    .orElseThrow(() -> new NoAvailableOperatorException("No operators available"));
        }

        Appointment appointment = new Appointment();
        appointment.setCustomer(new Customer(customerId)); // Assuming customer exists
        appointment.setServiceOperator(operator);
        appointment.setStartTime(startTime);
        appointment.setEndTime(startTime.plusHours(1)); // 1-hour appointment
        appointment.setStatus(AppointmentStatus.BOOKED);

        return appointmentRepository.save(appointment);
    }

    // 2. Reschedule an existing appointment
    public Appointment rescheduleAppointment(Long appointmentId, LocalDateTime newStartTime) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newStartTime.plusHours(1));
        appointment.setStatus(AppointmentStatus.RESCHEDULED);

        return appointmentRepository.save(appointment);
    }

    // 3. Cancel an appointment
    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    // 4. Get all booked appointments for a specific operator
    public List<Appointment> getBookedAppointments(Long operatorId) {
        return appointmentRepository.findAllByServiceOperatorIdAndStatusIn(operatorId, Arrays.asList(AppointmentStatus.BOOKED, AppointmentStatus.RESCHEDULED));
    }

    // 5. Get available (open) slots for a specific operator, merging consecutive slots
    public List<TimeSlot> getAvailableSlots(Long operatorId, LocalDate date) {
        List<Appointment> bookedAppointments = appointmentRepository.findAllByServiceOperatorIdAndStartTimeBetween(
                operatorId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        return mergeAvailableSlots(bookedAppointments, date);
    }

    // Logic to merge consecutive open time slots
    private List<TimeSlot> mergeAvailableSlots(List<Appointment> bookedAppointments, LocalDate date) {
        List<TimeSlot> fullDaySlots = createFullDaySlots(date);

        // Step 2: Extract booked time slots from appointments
        List<TimeSlot> bookedSlots = new ArrayList<>();
        for (Appointment appointment : bookedAppointments) {
            bookedSlots.add(new TimeSlot(appointment.getStartTime(), appointment.getEndTime()));
        }

        // Step 3: Sort the booked slots based on start time
        bookedSlots.sort(Comparator.comparing(TimeSlot::getStart));

        // Step 4: Merge booked slots (to handle overlapping slots)
        List<TimeSlot> mergedBookedSlots = mergeTimeSlots(bookedSlots);

        // Step 5: Calculate available time slots
        List<TimeSlot> availableSlots = calculateAvailableSlots(fullDaySlots, mergedBookedSlots);

        return availableSlots;
    }
    private List<TimeSlot> createFullDaySlots(LocalDate date) {
        List<TimeSlot> fullDaySlots = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN); // 00:00
        LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX); // 23:59:59

        for (LocalDateTime time = start; time.isBefore(end.plusSeconds(1)); time = time.plusHours(1)) {
            if(time.isAfter(end)) break;
            fullDaySlots.add(new TimeSlot(time, time.plusHours(1)));
        }

        return fullDaySlots;
    }

    // Helper method to merge booked time slots
    private List<TimeSlot> mergeTimeSlots(List<TimeSlot> bookedSlots) {
        List<TimeSlot> mergedSlots = new ArrayList<>();

        for (TimeSlot currentSlot : bookedSlots) {
            if (mergedSlots.isEmpty() || !isOverlapping(mergedSlots.get(mergedSlots.size() - 1), currentSlot)) {
                mergedSlots.add(currentSlot);
            } else {
                TimeSlot lastMergedSlot = mergedSlots.get(mergedSlots.size() - 1);
                lastMergedSlot.setEnd(currentSlot.getEnd()); // Update the end time
            }
        }

        return mergedSlots;
    }

    // Step 5 Helper: Calculate available time slots
    private List<TimeSlot> calculateAvailableSlots(List<TimeSlot> fullDaySlots, List<TimeSlot> bookedSlots) {
        List<TimeSlot> availableSlots = new ArrayList<>();
        LocalDateTime lastEnd = fullDaySlots.get(0).getStart(); // Start from 00:00

        for (TimeSlot bookedSlot : bookedSlots) {
            // If there is time available before the booked slot
            if (lastEnd.isBefore(bookedSlot.getStart())) {
                availableSlots.add(new TimeSlot(lastEnd, bookedSlot.getStart()));
            }
            // Update the lastEnd to the end of the booked slot
            lastEnd = bookedSlot.getEnd();
        }

        // Check for remaining time after the last booked slot
        if (lastEnd.isBefore(fullDaySlots.get(fullDaySlots.size() - 1).getEnd())) {
            availableSlots.add(new TimeSlot(lastEnd, fullDaySlots.get(fullDaySlots.size() - 1).getEnd()));
        }

        return availableSlots;
    }


    private boolean isOverlapping(TimeSlot slot1, TimeSlot slot2) {
        return !slot1.getEnd().isBefore(slot2.getStart()) && !slot2.getEnd().isBefore(slot1.getStart());
    }
}

