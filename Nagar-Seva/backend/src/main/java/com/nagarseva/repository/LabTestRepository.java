package com.nagarseva.repository;

import com.nagarseva.entity.LabTestRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabTestRepository extends JpaRepository<LabTestRequest, Long> {
    List<LabTestRequest> findByProformaId(Long proformaId);
    List<LabTestRequest> findByPatientEmail(String patientEmail);
    List<LabTestRequest> findByStatus(String status);
}
