package com.example.carServiceServ.dao;

import com.example.carServiceServ.entities.ServiceOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ServiceOperatorRepository extends JpaRepository<ServiceOperator, Long> {

    @Query("SELECT so FROM ServiceOperator so WHERE so.id NOT IN " +
            "(SELECT a.serviceOperator.id FROM Appointment a WHERE a.startTime = :startTime AND a.status = 'BOOKED')")
    Optional<ServiceOperator> findAvailableOperator(@Param("startTime") LocalDateTime startTime);
}

