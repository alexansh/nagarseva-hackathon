package com.nagarseva.repository;

import com.nagarseva.entity.DigitalConsentAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DigitalConsentRepository extends JpaRepository<DigitalConsentAgreement, Long> {
    List<DigitalConsentAgreement> findByProformaId(Long proformaId);
    List<DigitalConsentAgreement> findByPatientEmail(String patientEmail);
}
