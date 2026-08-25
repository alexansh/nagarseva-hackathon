package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "digital_consent_agreements")
public class DigitalConsentAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long proformaId;

    @Column(nullable = false)
    private String consentType; // PROCEDURE_NOC, TELE_CONSULT_CONSENT, PSYCHIATRIC_EVALUATION_CONSENT, RADIOLOGY_CONTRAST_NOC, SURGERY_PRE_NUP

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String termsAndConditions;

    @Column(nullable = false)
    private String patientName;

    @Column
    private String patientEmail;

    @Column(nullable = false)
    private Boolean isPatientSigned = false;

    @Column
    private String patientSignatureText;

    @Column
    private LocalDateTime patientSignedAt;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false)
    private String doctorRegNo;

    @Column(nullable = false)
    private Boolean isDoctorWitnessed = false;

    @Column
    private String doctorWitnessStampSeal;

    @Column(nullable = false)
    private String status = "PENDING_SIGNATURE"; // PENDING_SIGNATURE, SIGNED_AND_VERIFIED, REVOKED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DigitalConsentAgreement() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProformaId() { return proformaId; }
    public void setProformaId(Long proformaId) { this.proformaId = proformaId; }

    public String getConsentType() { return consentType; }
    public void setConsentType(String consentType) { this.consentType = consentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTermsAndConditions() { return termsAndConditions; }
    public void setTermsAndConditions(String termsAndConditions) { this.termsAndConditions = termsAndConditions; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public Boolean getIsPatientSigned() { return isPatientSigned; }
    public void setIsPatientSigned(Boolean isPatientSigned) { this.isPatientSigned = isPatientSigned; }

    public String getPatientSignatureText() { return patientSignatureText; }
    public void setPatientSignatureText(String patientSignatureText) { this.patientSignatureText = patientSignatureText; }

    public LocalDateTime getPatientSignedAt() { return patientSignedAt; }
    public void setPatientSignedAt(LocalDateTime patientSignedAt) { this.patientSignedAt = patientSignedAt; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorRegNo() { return doctorRegNo; }
    public void setDoctorRegNo(String doctorRegNo) { this.doctorRegNo = doctorRegNo; }

    public Boolean getIsDoctorWitnessed() { return isDoctorWitnessed; }
    public void setIsDoctorWitnessed(Boolean isDoctorWitnessed) { this.isDoctorWitnessed = isDoctorWitnessed; }

    public String getDoctorWitnessStampSeal() { return doctorWitnessStampSeal; }
    public void setDoctorWitnessStampSeal(String doctorWitnessStampSeal) { this.doctorWitnessStampSeal = doctorWitnessStampSeal; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
