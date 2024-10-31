package com.example.carServiceServ.dao;

import com.example.carServiceServ.entities.Appointment;
import com.example.carServiceServ.entities.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByServiceOperatorIdAndStatusIn(Long operatorId, List<AppointmentStatus> statuses);

    List<Appointment> findAllByServiceOperatorIdAndStartTimeBetween(Long operatorId, LocalDateTime start, LocalDateTime end);
}

