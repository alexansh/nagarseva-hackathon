package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_incentive_ledgers")
public class DoctorIncentiveLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false, unique = true)
    private String doctorRegNo;

    @Column(nullable = false)
    private String doctorType; // RESIDING_DOCTOR, CONSULTING_DOCTOR, CMO_OFFICER

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Integer totalCasesTreated = 0;

    @Column(nullable = false)
    private Integer successfulResolutionsCount = 0;

    @Column(nullable = false)
    private Double averagePatientWellnessScore = 4.8; // 1.0 - 5.0

    @Column(nullable = false)
    private Double commissionPerCase = 350.0; // In INR

    @Column(nullable = false)
    private Double totalCommissionEarned = 0.0;

    @Column(nullable = false)
    private Double wellnessBonusEarned = 0.0;

    @Column(nullable = false)
    private String payoutPeriod = "Current Cycle";

    @Column(nullable = false)
    private String status = "ACTIVE_ACCRUING";

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DoctorIncentiveLedger() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDoctorRegNo() { return doctorRegNo; }
    public void setDoctorRegNo(String doctorRegNo) { this.doctorRegNo = doctorRegNo; }

    public String getDoctorType() { return doctorType; }
    public void setDoctorType(String doctorType) { this.doctorType = doctorType; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getTotalCasesTreated() { return totalCasesTreated; }
    public void setTotalCasesTreated(Integer totalCasesTreated) { this.totalCasesTreated = totalCasesTreated; }

    public Integer getSuccessfulResolutionsCount() { return successfulResolutionsCount; }
    public void setSuccessfulResolutionsCount(Integer successfulResolutionsCount) { this.successfulResolutionsCount = successfulResolutionsCount; }

    public Double getAveragePatientWellnessScore() { return averagePatientWellnessScore; }
    public void setAveragePatientWellnessScore(Double averagePatientWellnessScore) { this.averagePatientWellnessScore = averagePatientWellnessScore; }

    public Double getCommissionPerCase() { return commissionPerCase; }
    public void setCommissionPerCase(Double commissionPerCase) { this.commissionPerCase = commissionPerCase; }

    public Double getTotalCommissionEarned() { return totalCommissionEarned; }
    public void setTotalCommissionEarned(Double totalCommissionEarned) { this.totalCommissionEarned = totalCommissionEarned; }

    public Double getWellnessBonusEarned() { return wellnessBonusEarned; }
    public void setWellnessBonusEarned(Double wellnessBonusEarned) { this.wellnessBonusEarned = wellnessBonusEarned; }

    public String getPayoutPeriod() { return payoutPeriod; }
    public void setPayoutPeriod(String payoutPeriod) { this.payoutPeriod = payoutPeriod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
