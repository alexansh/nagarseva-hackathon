package com.nagarseva.repository;

import com.nagarseva.entity.PatientProforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PatientProformaRepository extends JpaRepository<PatientProforma, Long> {
    List<PatientProforma> findByPatientEmail(String patientEmail);
    List<PatientProforma> findByAssignedDoctorRegNo(String assignedDoctorRegNo);
    List<PatientProforma> findByStatus(String status);
}
