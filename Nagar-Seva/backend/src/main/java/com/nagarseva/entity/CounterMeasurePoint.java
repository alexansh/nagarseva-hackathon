package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "counter_measure_points")
public class CounterMeasurePoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String pointCode;

    @Column(nullable = false)
    private String pointName;

    @Column(nullable = false)
    private String locationArea;

    @Column(nullable = false)
    private Integer readinessScore = 100; // 0 - 100%

    @Column(columnDefinition = "TEXT")
    private String suppliesChecklist; // JSON array of items checked

    @Column(nullable = false)
    private String officerInChargeName;

    @Column
    private String officerInChargeContact;

    @Column(nullable = false)
    private String status = "READY_OPERATIONAL"; // READY_OPERATIONAL, MAINTENANCE_DUE, RESTOCK_CRITICAL

    @Column
    private LocalDateTime lastAuditedAt = LocalDateTime.now();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public CounterMeasurePoint() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPointCode() { return pointCode; }
    public void setPointCode(String pointCode) { this.pointCode = pointCode; }

    public String getPointName() { return pointName; }
    public void setPointName(String pointName) { this.pointName = pointName; }

    public String getLocationArea() { return locationArea; }
    public void setLocationArea(String locationArea) { this.locationArea = locationArea; }

    public Integer getReadinessScore() { return readinessScore; }
    public void setReadinessScore(Integer readinessScore) { this.readinessScore = readinessScore; }

    public String getSuppliesChecklist() { return suppliesChecklist; }
    public void setSuppliesChecklist(String suppliesChecklist) { this.suppliesChecklist = suppliesChecklist; }

    public String getOfficerInChargeName() { return officerInChargeName; }
    public void setOfficerInChargeName(String officerInChargeName) { this.officerInChargeName = officerInChargeName; }

    public String getOfficerInChargeContact() { return officerInChargeContact; }
    public void setOfficerInChargeContact(String officerInChargeContact) { this.officerInChargeContact = officerInChargeContact; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastAuditedAt() { return lastAuditedAt; }
    public void setLastAuditedAt(LocalDateTime lastAuditedAt) { this.lastAuditedAt = lastAuditedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
