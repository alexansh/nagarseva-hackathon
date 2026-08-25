package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_test_requests")
public class LabTestRequest {

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
    private String testCategory; // RADIOLOGY, PATHOLOGY, PSYCHIATRY_INVENTORY, CARDIOLOGY_ECG

    @Column(nullable = false)
    private String testName;

    @Column(columnDefinition = "TEXT")
    private String clinicalIndication;

    @Column(nullable = false)
    private String orderingDoctorName;

    @Column(nullable = false)
    private String orderingDoctorRegNo;

    @Column(nullable = false)
    private Boolean isDoctorRequisitionStamped = true;

    @Column
    private String doctorRequisitionStampSeal;

    @Column
    private String labTechnicianName;

    @Column(columnDefinition = "TEXT")
    private String findingsReport;

    @Column
    private String normalRange;

    @Column
    private String observedValue;

    @Column
    private String interpretation; // NORMAL, BORDERLINE, ABNORMAL_CRITICAL

    @Column(nullable = false)
    private Boolean isLabApproved = false;

    @Column
    private String approvingConsultantName;

    @Column
    private String approvingConsultantRegNo;

    @Column
    private String labApprovalStampSeal;

    @Column
    private Boolean cmoSignOff = false;

    @Column(nullable = false)
    private String status = "REQUESTED"; // REQUESTED, SAMPLE_COLLECTED, ANALYZED, APPROVED_STAMPED

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime completedAt;

    public LabTestRequest() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProformaId() { return proformaId; }
    public void setProformaId(Long proformaId) { this.proformaId = proformaId; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getTestCategory() { return testCategory; }
    public void setTestCategory(String testCategory) { this.testCategory = testCategory; }

    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }

    public String getClinicalIndication() { return clinicalIndication; }
    public void setClinicalIndication(String clinicalIndication) { this.clinicalIndication = clinicalIndication; }

    public String getOrderingDoctorName() { return orderingDoctorName; }
    public void setOrderingDoctorName(String orderingDoctorName) { this.orderingDoctorName = orderingDoctorName; }

    public String getOrderingDoctorRegNo() { return orderingDoctorRegNo; }
    public void setOrderingDoctorRegNo(String orderingDoctorRegNo) { this.orderingDoctorRegNo = orderingDoctorRegNo; }

    public Boolean getIsDoctorRequisitionStamped() { return isDoctorRequisitionStamped; }
    public void setIsDoctorRequisitionStamped(Boolean isDoctorRequisitionStamped) { this.isDoctorRequisitionStamped = isDoctorRequisitionStamped; }

    public String getDoctorRequisitionStampSeal() { return doctorRequisitionStampSeal; }
    public void setDoctorRequisitionStampSeal(String doctorRequisitionStampSeal) { this.doctorRequisitionStampSeal = doctorRequisitionStampSeal; }

    public String getLabTechnicianName() { return labTechnicianName; }
    public void setLabTechnicianName(String labTechnicianName) { this.labTechnicianName = labTechnicianName; }

    public String getFindingsReport() { return findingsReport; }
    public void setFindingsReport(String findingsReport) { this.findingsReport = findingsReport; }

    public String getNormalRange() { return normalRange; }
    public void setNormalRange(String normalRange) { this.normalRange = normalRange; }

    public String getObservedValue() { return observedValue; }
    public void setObservedValue(String observedValue) { this.observedValue = observedValue; }

    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String interpretation) { this.interpretation = interpretation; }

    public Boolean getIsLabApproved() { return isLabApproved; }
    public void setIsLabApproved(Boolean isLabApproved) { this.isLabApproved = isLabApproved; }

    public String getApprovingConsultantName() { return approvingConsultantName; }
    public void setApprovingConsultantName(String approvingConsultantName) { this.approvingConsultantName = approvingConsultantName; }

    public String getApprovingConsultantRegNo() { return approvingConsultantRegNo; }
    public void setApprovingConsultantRegNo(String approvingConsultantRegNo) { this.approvingConsultantRegNo = approvingConsultantRegNo; }

    public String getLabApprovalStampSeal() { return labApprovalStampSeal; }
    public void setLabApprovalStampSeal(String labApprovalStampSeal) { this.labApprovalStampSeal = labApprovalStampSeal; }

    public Boolean getCmoSignOff() { return cmoSignOff; }
    public void setCmoSignOff(Boolean cmoSignOff) { this.cmoSignOff = cmoSignOff; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
