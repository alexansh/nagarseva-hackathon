package com.nagarseva.controller;

import com.nagarseva.entity.*;
import com.nagarseva.service.HealthcareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/health")
public class HealthcareController {

    @Autowired
    private HealthcareService healthcareService;

    /**
     * POST /api/health/triage - AI Layman Symptom Triage & Digital Token Issuance
     */
    @PostMapping("/triage")
    public ResponseEntity<PatientProforma> triageAndRegister(@RequestBody Map<String, Object> body) {
        String patientName = (String) body.getOrDefault("patientName", "Anonymous Citizen");
        String patientEmail = (String) body.getOrDefault("patientEmail", "patient@nagarseva.com");
        Integer age = body.get("patientAge") != null ? Integer.parseInt(body.get("patientAge").toString()) : 30;
        String gender = (String) body.getOrDefault("gender", "Female");
        String bloodGroup = (String) body.getOrDefault("bloodGroup", "B+");
        String contact = (String) body.getOrDefault("contactNumber", "+91 98765 43210");
        String chiefComplaint = (String) body.getOrDefault("chiefComplaint", "Fever and fatigue for 2 days.");
        Boolean isTeleConsult = body.get("isTeleConsult") != null ? Boolean.parseBoolean(body.get("isTeleConsult").toString()) : false;

        PatientProforma proforma = healthcareService.triageAndRegisterPatient(
                patientName, patientEmail, age, gender, bloodGroup, contact, chiefComplaint, isTeleConsult
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(proforma);
    }

    /**
     * GET /api/health/proformas - Get all patient clinical proformas
     */
    @GetMapping("/proformas")
    public ResponseEntity<List<PatientProforma>> getAllProformas() {
        return ResponseEntity.ok(healthcareService.getAllProformas());
    }

    /**
     * GET /api/health/proformas/{id} - Get proforma by ID
     */
    @GetMapping("/proformas/{id}")
    public ResponseEntity<PatientProforma> getProformaById(@PathVariable Long id) {
        Optional<PatientProforma> opt = healthcareService.getProformaById(id);
        return opt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/health/proformas/patient - Get patient's proformas
     */
    @GetMapping("/proformas/patient")
    public ResponseEntity<List<PatientProforma>> getPatientProformas(@RequestParam(required = false) String email) {
        return ResponseEntity.ok(healthcareService.getProformasByPatient(email));
    }

    /**
     * POST /api/health/proformas/{id}/handover - Zero-Resistance Doctor Case Handover
     */
    @PostMapping("/proformas/{id}/handover")
    public ResponseEntity<?> handoverCase(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String fromDoctor = body.getOrDefault("fromDoctorName", "Dr. Rajeshwar Sharma");
        String fromDept = body.getOrDefault("fromDepartment", "GENERAL_MEDICINE");
        String toDoctor = body.getOrDefault("toDoctorName", "Dr. Ananya Roy");
        String toDept = body.getOrDefault("toDepartment", "PSYCHIATRY");
        String reason = body.getOrDefault("reason", "Specialized evaluation and management required.");

        try {
            CaseHandoverLog log = healthcareService.handoverPatientCase(id, fromDoctor, fromDept, toDoctor, toDept, reason);
            return ResponseEntity.ok(log);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/health/prescriptions - Create and Stamp Prescription
     */
    @PostMapping("/prescriptions")
    public ResponseEntity<PrescriptionRecord> createPrescription(@RequestBody Map<String, Object> body) {
        Long proformaId = Long.parseLong(body.get("proformaId").toString());
        String patientName = (String) body.getOrDefault("patientName", "Patient");
        String patientEmail = (String) body.getOrDefault("patientEmail", "patient@nagarseva.com");
        String doctorName = (String) body.getOrDefault("doctorName", "Dr. Ananya Roy (MD Psychiatry)");
        String doctorRegNo = (String) body.getOrDefault("doctorRegNo", "MCI-PSY-44912");
        String doctorDept = (String) body.getOrDefault("doctorDepartment", "Psychiatry & Behavioral Health");
        String doctorType = (String) body.getOrDefault("doctorType", "CONSULTING_DOCTOR");
        String diagnosis = (String) body.getOrDefault("diagnosisSummary", "Clinical diagnosis confirmed.");
        String medsJson = (String) body.getOrDefault("medicationsJson", "[]");
        String advice = (String) body.getOrDefault("lifestyleAdvice", "Adequate rest and hydration.");
        boolean approveStampNow = body.get("approveStampNow") != null ? Boolean.parseBoolean(body.get("approveStampNow").toString()) : true;

        PrescriptionRecord record = healthcareService.createAndStampPrescription(
                proformaId, patientName, patientEmail, doctorName, doctorRegNo, doctorDept, doctorType, diagnosis, medsJson, advice, approveStampNow
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    /**
     * GET /api/health/prescriptions - Get all prescriptions
     */
    @GetMapping("/prescriptions")
    public ResponseEntity<List<PrescriptionRecord>> getAllPrescriptions() {
        return ResponseEntity.ok(healthcareService.getAllPrescriptions());
    }

    /**
     * GET /api/health/prescriptions/proforma/{proformaId}
     */
    @GetMapping("/prescriptions/proforma/{proformaId}")
    public ResponseEntity<List<PrescriptionRecord>> getPrescriptionsByProforma(@PathVariable Long proformaId) {
        return ResponseEntity.ok(healthcareService.getPrescriptionsByProforma(proformaId));
    }

    /**
     * POST /api/health/lab-tests - Request Lab Test
     */
    @PostMapping("/lab-tests")
    public ResponseEntity<LabTestRequest> createLabTest(@RequestBody Map<String, Object> body) {
        Long proformaId = Long.parseLong(body.get("proformaId").toString());
        String patientName = (String) body.getOrDefault("patientName", "Patient");
        String patientEmail = (String) body.getOrDefault("patientEmail", "patient@nagarseva.com");
        String testCategory = (String) body.getOrDefault("testCategory", "RADIOLOGY");
        String testName = (String) body.getOrDefault("testName", "MRI Brain / CT Contrast");
        String clinicalIndication = (String) body.getOrDefault("clinicalIndication", "Rule out structural intracranial anomaly.");
        String doctorName = (String) body.getOrDefault("orderingDoctorName", "Dr. Vikram Malhotra");
        String doctorRegNo = (String) body.getOrDefault("orderingDoctorRegNo", "MCI-RAD-88120");

        LabTestRequest request = healthcareService.createLabTestRequest(
                proformaId, patientName, patientEmail, testCategory, testName, clinicalIndication, doctorName, doctorRegNo
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /**
     * PATCH /api/health/lab-tests/{id}/approve - Consultant / Pathologist Approval Stamp
     */
    @PatchMapping("/lab-tests/{id}/approve")
    public ResponseEntity<?> approveLabTest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String tech = body.getOrDefault("labTechnicianName", "Senior Diagnostic Technologist");
        String findings = body.getOrDefault("findingsReport", "Normal scan parameters observed.");
        String observed = body.getOrDefault("observedValue", "Within physiological limit");
        String normal = body.getOrDefault("normalRange", "Standard Baseline");
        String interp = body.getOrDefault("interpretation", "NORMAL");
        String consultant = body.getOrDefault("approvingConsultantName", "Dr. Vikram Malhotra (MD Radiology)");
        String regNo = body.getOrDefault("approvingConsultantRegNo", "MCI-RAD-88120");

        try {
            LabTestRequest approved = healthcareService.approveLabTestResult(
                    id, tech, findings, observed, normal, interp, consultant, regNo
            );
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/health/lab-tests - Get all lab tests
     */
    @GetMapping("/lab-tests")
    public ResponseEntity<List<LabTestRequest>> getAllLabTests() {
        return ResponseEntity.ok(healthcareService.getAllLabTests());
    }

    /**
     * GET /api/health/consents - Get all NOC / consent agreements
     */
    @GetMapping("/consents")
    public ResponseEntity<List<DigitalConsentAgreement>> getAllConsents() {
        return ResponseEntity.ok(healthcareService.getAllConsents());
    }

    /**
     * PATCH /api/health/consents/{id}/sign - Patient signs digital NOC consent
     */
    @PatchMapping("/consents/{id}/sign")
    public ResponseEntity<?> signConsent(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String signature = body.getOrDefault("patientSignatureText", "Digitally Signed by Patient");
        try {
            DigitalConsentAgreement signed = healthcareService.signConsentAgreement(id, signature);
            return ResponseEntity.ok(signed);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/health/instruments - Get all medical instruments
     */
    @GetMapping("/instruments")
    public ResponseEntity<List<MedicalInstrument>> getAllInstruments() {
        return ResponseEntity.ok(healthcareService.getAllInstruments());
    }

    /**
     * PATCH /api/health/instruments/{id}/maintenance - Sterilize or Refurbish Instrument
     */
    @PatchMapping("/instruments/{id}/maintenance")
    public ResponseEntity<?> maintainInstrument(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String officer = (String) body.getOrDefault("officerName", "OT In-Charge");
        String notes = (String) body.getOrDefault("notes", "Routine autoclave sterilization cycle performed.");
        boolean isRefurbish = body.get("isFullRefurbish") != null ? Boolean.parseBoolean(body.get("isFullRefurbish").toString()) : false;

        try {
            MedicalInstrument updated = healthcareService.recordInstrumentSterilizationOrRefurbish(id, officer, notes, isRefurbish);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/health/counter-measures - Emergency Preparedness Points
     */
    @GetMapping("/counter-measures")
    public ResponseEntity<List<CounterMeasurePoint>> getAllCounterMeasures() {
        return ResponseEntity.ok(healthcareService.getAllCounterMeasurePoints());
    }

    /**
     * PATCH /api/health/counter-measures/{id}/audit - Audit Point Readiness
     */
    @PatchMapping("/counter-measures/{id}/audit")
    public ResponseEntity<?> auditCounterMeasure(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        int score = body.get("readinessScore") != null ? Integer.parseInt(body.get("readinessScore").toString()) : 100;
        String status = (String) body.getOrDefault("status", "READY_OPERATIONAL");
        String officer = (String) body.getOrDefault("officerName", "Emergency Response Lead");

        try {
            CounterMeasurePoint audited = healthcareService.auditCounterMeasurePoint(id, score, status, officer);
            return ResponseEntity.ok(audited);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/health/doctor-incentives - Doctor Commission & Wellness Ledger
     */
    @GetMapping("/doctor-incentives")
    public ResponseEntity<List<DoctorIncentiveLedger>> getDoctorIncentives() {
        return ResponseEntity.ok(healthcareService.getDoctorIncentives());
    }
}
