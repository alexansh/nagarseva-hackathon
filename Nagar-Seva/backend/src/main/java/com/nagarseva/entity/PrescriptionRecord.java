package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescription_records")
public class PrescriptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long proformaId;

    @Column(nullable = false)
    private String patientName;

    @Column
    private String patientEmail;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false)
    private String doctorRegNo;

    @Column(nullable = false)
    private String doctorDepartment;

    @Column(nullable = false)
    private String doctorType; // RESIDING_DOCTOR, CONSULTING_DOCTOR

    @Column(columnDefinition = "TEXT")
    private String diagnosisSummary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String medicationsJson; // JSON array of { name, dosage, frequency, duration, instructions }

    @Column(columnDefinition = "TEXT")
    private String lifestyleAdvice;

    @Column(nullable = false)
    private Boolean isStamped = false;

    @Column
    private String digitalStampSeal; // e.g. STAMP-DEL-MCI-88921-VERIFIED

    @Column
    private LocalDateTime stampApprovedAt;

    @Column(nullable = false)
    private String status = "DRAFT"; // DRAFT, STAMPED_APPROVED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PrescriptionRecord() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProformaId() { return proformaId; }
    public void setProformaId(Long proformaId) { this.proformaId = proformaId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorRegNo() { return doctorRegNo; }
    public void setDoctorRegNo(String doctorRegNo) { this.doctorRegNo = doctorRegNo; }

    public String getDoctorDepartment() { return doctorDepartment; }
    public void setDoctorDepartment(String doctorDepartment) { this.doctorDepartment = doctorDepartment; }

    public String getDoctorType() { return doctorType; }
    public void setDoctorType(String doctorType) { this.doctorType = doctorType; }

    public String getDiagnosisSummary() { return diagnosisSummary; }
    public void setDiagnosisSummary(String diagnosisSummary) { this.diagnosisSummary = diagnosisSummary; }

    public String getMedicationsJson() { return medicationsJson; }
    public void setMedicationsJson(String medicationsJson) { this.medicationsJson = medicationsJson; }

    public String getLifestyleAdvice() { return lifestyleAdvice; }
    public void setLifestyleAdvice(String lifestyleAdvice) { this.lifestyleAdvice = lifestyleAdvice; }

    public Boolean getIsStamped() { return isStamped; }
    public void setIsStamped(Boolean isStamped) { this.isStamped = isStamped; }

    public String getDigitalStampSeal() { return digitalStampSeal; }
    public void setDigitalStampSeal(String digitalStampSeal) { this.digitalStampSeal = digitalStampSeal; }

    public LocalDateTime getStampApprovedAt() { return stampApprovedAt; }
    public void setStampApprovedAt(LocalDateTime stampApprovedAt) { this.stampApprovedAt = stampApprovedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
