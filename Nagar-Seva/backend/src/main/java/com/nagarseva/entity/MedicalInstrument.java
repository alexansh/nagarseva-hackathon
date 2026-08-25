package com.nagarseva.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_instruments")
public class MedicalInstrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false, unique = true)
    private String serialNumber;

    @Column(nullable = false)
    private Integer totalSterilizationCycles = 0;

    @Column(nullable = false)
    private Integer maxSafeCycles = 150;

    @Column(nullable = false)
    private String status = "READY_FOR_USE"; // READY_FOR_USE, STERILIZATION_IN_PROGRESS, REFURBISHMENT_REQUIRED, DECOMMISSIONED

    @Column
    private LocalDateTime lastSterilizedAt;

    @Column
    private LocalDateTime lastRefurbishedAt;

    @Column(columnDefinition = "TEXT")
    private String refurbishingNotes;

    @Column
    private String certifiedSafeByOfficer;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public MedicalInstrument() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public Integer getTotalSterilizationCycles() { return totalSterilizationCycles; }
    public void setTotalSterilizationCycles(Integer totalSterilizationCycles) { this.totalSterilizationCycles = totalSterilizationCycles; }

    public Integer getMaxSafeCycles() { return maxSafeCycles; }
    public void setMaxSafeCycles(Integer maxSafeCycles) { this.maxSafeCycles = maxSafeCycles; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastSterilizedAt() { return lastSterilizedAt; }
    public void setLastSterilizedAt(LocalDateTime lastSterilizedAt) { this.lastSterilizedAt = lastSterilizedAt; }

    public LocalDateTime getLastRefurbishedAt() { return lastRefurbishedAt; }
    public void setLastRefurbishedAt(LocalDateTime lastRefurbishedAt) { this.lastRefurbishedAt = lastRefurbishedAt; }

    public String getRefurbishingNotes() { return refurbishingNotes; }
    public void setRefurbishingNotes(String refurbishingNotes) { this.refurbishingNotes = refurbishingNotes; }

    public String getCertifiedSafeByOfficer() { return certifiedSafeByOfficer; }
    public void setCertifiedSafeByOfficer(String certifiedSafeByOfficer) { this.certifiedSafeByOfficer = certifiedSafeByOfficer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
