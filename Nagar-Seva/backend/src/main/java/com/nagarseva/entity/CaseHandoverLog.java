package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_handover_logs")
public class CaseHandoverLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long proformaId;

    @Column(nullable = false)
    private String fromDoctorName;

    @Column(nullable = false)
    private String fromDepartment;

    @Column(nullable = false)
    private String toDoctorName;

    @Column(nullable = false)
    private String toDepartment;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reasonForHandover;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String aiHandoverClinicalBriefing;

    @Column(nullable = false)
    private String status = "TRANSFERRED_ACKNOWLEDGED";

    @Column(nullable = false, updatable = false)
    private LocalDateTime handoverTimestamp = LocalDateTime.now();

    public CaseHandoverLog() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProformaId() { return proformaId; }
    public void setProformaId(Long proformaId) { this.proformaId = proformaId; }

    public String getFromDoctorName() { return fromDoctorName; }
    public void setFromDoctorName(String fromDoctorName) { this.fromDoctorName = fromDoctorName; }

    public String getFromDepartment() { return fromDepartment; }
    public void setFromDepartment(String fromDepartment) { this.fromDepartment = fromDepartment; }

    public String getToDoctorName() { return toDoctorName; }
    public void setToDoctorName(String toDoctorName) { this.toDoctorName = toDoctorName; }

    public String getToDepartment() { return toDepartment; }
    public void setToDepartment(String toDepartment) { this.toDepartment = toDepartment; }

    public String getReasonForHandover() { return reasonForHandover; }
    public void setReasonForHandover(String reasonForHandover) { this.reasonForHandover = reasonForHandover; }

    public String getAiHandoverClinicalBriefing() { return aiHandoverClinicalBriefing; }
    public void setAiHandoverClinicalBriefing(String aiHandoverClinicalBriefing) { this.aiHandoverClinicalBriefing = aiHandoverClinicalBriefing; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getHandoverTimestamp() { return handoverTimestamp; }
    public void setHandoverTimestamp(LocalDateTime handoverTimestamp) { this.handoverTimestamp = handoverTimestamp; }
}
