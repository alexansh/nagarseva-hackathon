package com.nagarseva.repository;

import com.nagarseva.entity.PrescriptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<PrescriptionRecord, Long> {
    List<PrescriptionRecord> findByProformaId(Long proformaId);
    List<PrescriptionRecord> findByPatientEmail(String patientEmail);
    List<PrescriptionRecord> findByDoctorRegNo(String doctorRegNo);
}
