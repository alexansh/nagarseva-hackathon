package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_proformas")
public class PatientProforma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tokenNumber; // e.g. OPD-PSY-102

    @Column(nullable = false)
    private String patientName;

    @Column
    private String patientEmail;

    @Column
    private Integer patientAge;

    @Column
    private String gender;

    @Column
    private String bloodGroup;

    @Column
    private String contactNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String chiefComplaint; // Patient's self-told symptoms

    @Column(columnDefinition = "TEXT")
    private String symptomsTimeline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MedicalDepartment aiTriageDepartment;

    @Column(nullable = false)
    private String aiTriageUrgency = "ROUTINE"; // CRITICAL, URGENT, ROUTINE

    @Column(columnDefinition = "TEXT")
    private String aiClinicalRationale;

    @Column
    private Integer queueEstimatedWaitMinutes = 15;

    @Column
    private String vitalsBp; // e.g. 120/80 mmHg

    @Column
    private String vitalsPulse; // e.g. 74 bpm

    @Column
    private String vitalsTemp; // e.g. 98.6 F

    @Column
    private String vitalsSpo2; // e.g. 99%

    @Column
    private String assignedDoctorName;

    @Column
    private String assignedDoctorRegNo;

    @Column
    private String doctorRoleType; // RESIDING_DOCTOR, CONSULTING_DOCTOR

    @Column(columnDefinition = "TEXT")
    private String clinicalDiagnosis;

    @Column(columnDefinition = "TEXT")
    private String clinicalCaseNotes;

    @Column(nullable = false)
    private String status = "OPD_TRIAGED"; // OPD_TRIAGED, UNDER_CONSULTATION, LAB_REQUESTED, TRANSFERRED, RESOLVED

    @Column
    private Boolean cmoSignOffRequired = false;

    @Column
    private Boolean cmoApproved = false;

    @Column
    private Boolean isTeleConsult = false;

    @Column
    private Boolean isVerifiableRecord = true;

    @Column
    private String verificationHash;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PatientProforma() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public Integer getPatientAge() { return patientAge; }
    public void setPatientAge(Integer patientAge) { this.patientAge = patientAge; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }

    public String getSymptomsTimeline() { return symptomsTimeline; }
    public void setSymptomsTimeline(String symptomsTimeline) { this.symptomsTimeline = symptomsTimeline; }

    public MedicalDepartment getAiTriageDepartment() { return aiTriageDepartment; }
    public void setAiTriageDepartment(MedicalDepartment aiTriageDepartment) { this.aiTriageDepartment = aiTriageDepartment; }

    public String getAiTriageUrgency() { return aiTriageUrgency; }
    public void setAiTriageUrgency(String aiTriageUrgency) { this.aiTriageUrgency = aiTriageUrgency; }

    public String getAiClinicalRationale() { return aiClinicalRationale; }
    public void setAiClinicalRationale(String aiClinicalRationale) { this.aiClinicalRationale = aiClinicalRationale; }

    public Integer getQueueEstimatedWaitMinutes() { return queueEstimatedWaitMinutes; }
    public void setQueueEstimatedWaitMinutes(Integer queueEstimatedWaitMinutes) { this.queueEstimatedWaitMinutes = queueEstimatedWaitMinutes; }

    public String getVitalsBp() { return vitalsBp; }
    public void setVitalsBp(String vitalsBp) { this.vitalsBp = vitalsBp; }

    public String getVitalsPulse() { return vitalsPulse; }
    public void setVitalsPulse(String vitalsPulse) { this.vitalsPulse = vitalsPulse; }

    public String getVitalsTemp() { return vitalsTemp; }
    public void setVitalsTemp(String vitalsTemp) { this.vitalsTemp = vitalsTemp; }

    public String getVitalsSpo2() { return vitalsSpo2; }
    public void setVitalsSpo2(String vitalsSpo2) { this.vitalsSpo2 = vitalsSpo2; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public String getAssignedDoctorRegNo() { return assignedDoctorRegNo; }
    public void setAssignedDoctorRegNo(String assignedDoctorRegNo) { this.assignedDoctorRegNo = assignedDoctorRegNo; }

    public String getDoctorRoleType() { return doctorRoleType; }
    public void setDoctorRoleType(String doctorRoleType) { this.doctorRoleType = doctorRoleType; }

    public String getClinicalDiagnosis() { return clinicalDiagnosis; }
    public void setClinicalDiagnosis(String clinicalDiagnosis) { this.clinicalDiagnosis = clinicalDiagnosis; }

    public String getClinicalCaseNotes() { return clinicalCaseNotes; }
    public void setClinicalCaseNotes(String clinicalCaseNotes) { this.clinicalCaseNotes = clinicalCaseNotes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getCmoSignOffRequired() { return cmoSignOffRequired; }
    public void setCmoSignOffRequired(Boolean cmoSignOffRequired) { this.cmoSignOffRequired = cmoSignOffRequired; }

    public Boolean getCmoApproved() { return cmoApproved; }
    public void setCmoApproved(Boolean cmoApproved) { this.cmoApproved = cmoApproved; }

    public Boolean getIsTeleConsult() { return isTeleConsult; }
    public void setIsTeleConsult(Boolean isTeleConsult) { this.isTeleConsult = isTeleConsult; }

    public Boolean getIsVerifiableRecord() { return isVerifiableRecord; }
    public void setIsVerifiableRecord(Boolean isVerifiableRecord) { this.isVerifiableRecord = isVerifiableRecord; }

    public String getVerificationHash() { return verificationHash; }
    public void setVerificationHash(String verificationHash) { this.verificationHash = verificationHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
