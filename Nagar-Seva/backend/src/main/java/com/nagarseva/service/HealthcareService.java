package com.nagarseva.service;

import com.nagarseva.entity.*;
import com.nagarseva.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class HealthcareService {

    private static final Logger log = LoggerFactory.getLogger(HealthcareService.class);

    @Autowired
    private PatientProformaRepository proformaRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private LabTestRepository labTestRepository;

    @Autowired
    private DigitalConsentRepository consentRepository;

    @Autowired
    private MedicalInstrumentRepository instrumentRepository;

    @Autowired
    private CounterMeasureRepository counterMeasureRepository;

    @Autowired
    private DoctorIncentiveRepository incentiveRepository;

    @Autowired
    private CaseHandoverLogRepository handoverLogRepository;

    @Autowired
    private GeminiService geminiService;

    @PostConstruct
    public void init() {
        if (proformaRepository.count() == 0) {
            seedSampleHealthcareData();
        }
    }

    /**
     * AI Triage and Issue Digital OPD Token
     */
    public PatientProforma triageAndRegisterPatient(
            String patientName,
            String patientEmail,
            Integer age,
            String gender,
            String bloodGroup,
            String contact,
            String chiefComplaint,
            Boolean isTeleConsult) {

        GeminiService.PatientTriageResult triage = geminiService.triagePatientSymptom(chiefComplaint);

        MedicalDepartment dept;
        try {
            dept = MedicalDepartment.valueOf(triage.recommendedDepartment().toUpperCase());
        } catch (Exception e) {
            dept = MedicalDepartment.GENERAL_MEDICINE;
        }

        String deptPrefix = dept.name().substring(0, Math.min(3, dept.name().length()));
        int randomSeq = 100 + (int)(Math.random() * 899);
        String token = "OPD-" + deptPrefix + "-" + randomSeq;

        PatientProforma proforma = new PatientProforma();
        proforma.setTokenNumber(token);
        proforma.setPatientName(patientName);
        proforma.setPatientEmail(patientEmail != null && !patientEmail.isBlank() ? patientEmail : "patient@nagarseva.com");
        proforma.setPatientAge(age != null ? age : 32);
        proforma.setGender(gender != null ? gender : "Female");
        proforma.setBloodGroup(bloodGroup != null ? bloodGroup : "B+");
        proforma.setContactNumber(contact != null ? contact : "+91 98765 43210");
        proforma.setChiefComplaint(chiefComplaint);
        proforma.setSymptomsTimeline("Onset reported 2-4 days ago with progressive severity.");
        proforma.setAiTriageDepartment(dept);
        proforma.setAiTriageUrgency(triage.urgency());
        proforma.setAiClinicalRationale(triage.clinicalRationale());
        proforma.setQueueEstimatedWaitMinutes(triage.estimatedWaitMinutes());
        proforma.setCmoSignOffRequired(triage.requiresImmediateCmo());
        proforma.setIsTeleConsult(isTeleConsult != null ? isTeleConsult : false);
        proforma.setStatus("OPD_TRIAGED");

        // Initial Vitals
        proforma.setVitalsBp("120/80 mmHg");
        proforma.setVitalsPulse("76 bpm");
        proforma.setVitalsTemp("98.6 °F");
        proforma.setVitalsSpo2("99%");

        // Default doctor allocation
        if (dept == MedicalDepartment.PSYCHIATRY) {
            proforma.setAssignedDoctorName("Dr. Ananya Roy (MD Psychiatry)");
            proforma.setAssignedDoctorRegNo("MCI-PSY-44912");
            proforma.setDoctorRoleType("CONSULTING_DOCTOR");
        } else if (dept == MedicalDepartment.RADIOLOGY) {
            proforma.setAssignedDoctorName("Dr. Vikram Malhotra (MD Radiology)");
            proforma.setAssignedDoctorRegNo("MCI-RAD-88120");
            proforma.setDoctorRoleType("RESIDING_DOCTOR");
        } else {
            proforma.setAssignedDoctorName("Dr. Rajeshwar Sharma (MD Medicine)");
            proforma.setAssignedDoctorRegNo("MCI-GEN-10294");
            proforma.setDoctorRoleType("RESIDING_DOCTOR");
        }

        // Generate SHA-256 verification hash
        String rawHash = token + patientName + System.currentTimeMillis();
        proforma.setVerificationHash(generateSha256(rawHash));

        PatientProforma saved = proformaRepository.save(proforma);

        // Auto-generate standard procedural/tele-consult NOC if required
        createDefaultConsentForProforma(saved);

        return saved;
    }

    public List<PatientProforma> getAllProformas() {
        return proformaRepository.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0))
                .toList();
    }

    public Optional<PatientProforma> getProformaById(Long id) {
        return proformaRepository.findById(id);
    }

    public List<PatientProforma> getProformasByPatient(String email) {
        if (email == null || email.isBlank() || "citizen@nagarseva.com".equalsIgnoreCase(email) || "patient@nagarseva.com".equalsIgnoreCase(email)) {
            return getAllProformas();
        }
        List<PatientProforma> list = proformaRepository.findByPatientEmail(email);
        return list.isEmpty() ? getAllProformas() : list;
    }

    /**
     * Zero-Resistance Inter-Doctor Handover
     */
    public CaseHandoverLog handoverPatientCase(
            Long proformaId,
            String fromDoctor,
            String fromDept,
            String toDoctor,
            String toDept,
            String reason) {

        Optional<PatientProforma> opt = proformaRepository.findById(proformaId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Patient proforma not found: " + proformaId);
        }

        PatientProforma proforma = opt.get();

        String aiBriefing = geminiService.generateCaseHandoverSummary(
                proforma.getPatientName(),
                proforma.getPatientAge() != null ? proforma.getPatientAge() : 30,
                proforma.getChiefComplaint(),
                proforma.getClinicalDiagnosis(),
                proforma.getVitalsBp() + ", Pulse: " + proforma.getVitalsPulse(),
                fromDept,
                toDept,
                reason
        );

        CaseHandoverLog logEntry = new CaseHandoverLog();
        logEntry.setProformaId(proformaId);
        logEntry.setFromDoctorName(fromDoctor);
        logEntry.setFromDepartment(fromDept);
        logEntry.setToDoctorName(toDoctor);
        logEntry.setToDepartment(toDept);
        logEntry.setReasonForHandover(reason);
        logEntry.setAiHandoverClinicalBriefing(aiBriefing);
        logEntry.setStatus("TRANSFERRED_ACKNOWLEDGED");

        // Update Proforma status & active doctor
        try {
            proforma.setAiTriageDepartment(MedicalDepartment.valueOf(toDept.toUpperCase()));
        } catch (Exception ignored) {}
        proforma.setAssignedDoctorName(toDoctor);
        proforma.setStatus("TRANSFERRED");
        proforma.setClinicalCaseNotes((proforma.getClinicalCaseNotes() != null ? proforma.getClinicalCaseNotes() + "\n" : "") + "[Handover]: " + reason);
        proformaRepository.save(proforma);

        return handoverLogRepository.save(logEntry);
    }

    /**
     * Digital Stamp Prescription Creation & Approval
     */
    public PrescriptionRecord createAndStampPrescription(
            Long proformaId,
            String patientName,
            String patientEmail,
            String doctorName,
            String doctorRegNo,
            String doctorDept,
            String doctorType,
            String diagnosis,
            String medicationsJson,
            String lifestyleAdvice,
            boolean approveStampNow) {

        PrescriptionRecord record = new PrescriptionRecord();
        record.setProformaId(proformaId);
        record.setPatientName(patientName);
        record.setPatientEmail(patientEmail);
        record.setDoctorName(doctorName);
        record.setDoctorRegNo(doctorRegNo);
        record.setDoctorDepartment(doctorDept);
        record.setDoctorType(doctorType != null ? doctorType : "CONSULTING_DOCTOR");
        record.setDiagnosisSummary(diagnosis);
        record.setMedicationsJson(medicationsJson);
        record.setLifestyleAdvice(lifestyleAdvice);

        if (approveStampNow) {
            record.setIsStamped(true);
            record.setStatus("STAMPED_APPROVED");
            record.setDigitalStampSeal("STAMP-MED-" + doctorRegNo.replace(" ", "-") + "-" + (System.currentTimeMillis() % 100000));
            record.setStampApprovedAt(LocalDateTime.now());
        } else {
            record.setIsStamped(false);
            record.setStatus("DRAFT");
        }

        // Update proforma diagnosis
        proformaRepository.findById(proformaId).ifPresent(p -> {
            p.setClinicalDiagnosis(diagnosis);
            p.setStatus("RESOLVED");
            proformaRepository.save(p);
        });

        // Record Doctor commission incentive
        recordDoctorConsultation(doctorRegNo, doctorName, doctorDept, doctorType, 4.9);

        return prescriptionRepository.save(record);
    }

    public List<PrescriptionRecord> getPrescriptionsByProforma(Long proformaId) {
        return prescriptionRepository.findByProformaId(proformaId);
    }

    public List<PrescriptionRecord> getAllPrescriptions() {
        return prescriptionRepository.findAll();
    }

    /**
     * Lab Test Requisitions and Stamped Approvals
     */
    public LabTestRequest createLabTestRequest(
            Long proformaId,
            String patientName,
            String patientEmail,
            String testCategory,
            String testName,
            String clinicalIndication,
            String doctorName,
            String doctorRegNo) {

        LabTestRequest request = new LabTestRequest();
        request.setProformaId(proformaId);
        request.setPatientName(patientName);
        request.setPatientEmail(patientEmail);
        request.setTestCategory(testCategory);
        request.setTestName(testName);
        request.setClinicalIndication(clinicalIndication);
        request.setOrderingDoctorName(doctorName);
        request.setOrderingDoctorRegNo(doctorRegNo);
        request.setIsDoctorRequisitionStamped(true);
        request.setDoctorRequisitionStampSeal("REQ-STAMP-" + doctorRegNo.replace(" ", "-") + "-" + (System.currentTimeMillis() % 10000));
        request.setStatus("REQUESTED");

        proformaRepository.findById(proformaId).ifPresent(p -> {
            p.setStatus("LAB_REQUESTED");
            proformaRepository.save(p);
        });

        return labTestRepository.save(request);
    }

    public LabTestRequest approveLabTestResult(
            Long testId,
            String technicianName,
            String findings,
            String observedValue,
            String normalRange,
            String interpretation,
            String consultantName,
            String consultantRegNo) {

        Optional<LabTestRequest> opt = labTestRepository.findById(testId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Lab test not found: " + testId);
        }

        LabTestRequest test = opt.get();
        test.setLabTechnicianName(technicianName != null ? technicianName : "Senior Pathologist Tech");
        test.setFindingsReport(findings);
        test.setObservedValue(observedValue);
        test.setNormalRange(normalRange);
        test.setInterpretation(interpretation != null ? interpretation : "NORMAL");
        test.setIsLabApproved(true);
        test.setApprovingConsultantName(consultantName != null ? consultantName : "Dr. S. K. Gupta (Chief Pathologist)");
        test.setApprovingConsultantRegNo(consultantRegNo != null ? consultantRegNo : "MCI-PATH-90122");
        test.setLabApprovalStampSeal("LAB-STAMP-" + (consultantRegNo != null ? consultantRegNo.replace(" ", "-") : "PATH-01") + "-VERIFIED");
        test.setStatus("APPROVED_STAMPED");
        test.setCompletedAt(LocalDateTime.now());

        return labTestRepository.save(test);
    }

    public List<LabTestRequest> getAllLabTests() {
        return labTestRepository.findAll();
    }

    /**
     * Digital Consent / NOC Execution
     */
    public DigitalConsentAgreement signConsentAgreement(Long consentId, String patientSignatureText) {
        Optional<DigitalConsentAgreement> opt = consentRepository.findById(consentId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Consent agreement not found: " + consentId);
        }

        DigitalConsentAgreement consent = opt.get();
        consent.setIsPatientSigned(true);
        consent.setPatientSignatureText(patientSignatureText);
        consent.setPatientSignedAt(LocalDateTime.now());
        consent.setIsDoctorWitnessed(true);
        consent.setDoctorWitnessStampSeal("WITNESS-STAMP-" + consent.getDoctorRegNo().replace(" ", "-") + "-LEGAL");
        consent.setStatus("SIGNED_AND_VERIFIED");

        return consentRepository.save(consent);
    }

    public List<DigitalConsentAgreement> getAllConsents() {
        return consentRepository.findAll();
    }

    /**
     * Medical Instrument Refurbishing and Sterilization
     */
    public MedicalInstrument recordInstrumentSterilizationOrRefurbish(Long instrumentId, String officerName, String notes, boolean isFullRefurbish) {
        Optional<MedicalInstrument> opt = instrumentRepository.findById(instrumentId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Instrument not found: " + instrumentId);
        }

        MedicalInstrument instrument = opt.get();
        if (isFullRefurbish) {
            instrument.setTotalSterilizationCycles(0);
            instrument.setStatus("READY_FOR_USE");
            instrument.setLastRefurbishedAt(LocalDateTime.now());
            instrument.setRefurbishingNotes("Full refurbishment & calibration overhaul: " + notes);
            instrument.setCertifiedSafeByOfficer(officerName);
        } else {
            instrument.setTotalSterilizationCycles(instrument.getTotalSterilizationCycles() + 1);
            instrument.setLastSterilizedAt(LocalDateTime.now());
            if (instrument.getTotalSterilizationCycles() >= instrument.getMaxSafeCycles()) {
                instrument.setStatus("REFURBISHMENT_REQUIRED");
            } else {
                instrument.setStatus("READY_FOR_USE");
            }
        }

        return instrumentRepository.save(instrument);
    }

    public List<MedicalInstrument> getAllInstruments() {
        return instrumentRepository.findAll();
    }

    /**
     * Emergency Preparedness Counter Measure Points
     */
    public List<CounterMeasurePoint> getAllCounterMeasurePoints() {
        return counterMeasureRepository.findAll();
    }

    public CounterMeasurePoint auditCounterMeasurePoint(Long pointId, int score, String status, String officerName) {
        Optional<CounterMeasurePoint> opt = counterMeasureRepository.findById(pointId);
        if (opt.isEmpty()) {
            throw new RuntimeException("Counter measure point not found: " + pointId);
        }

        CounterMeasurePoint point = opt.get();
        point.setReadinessScore(score);
        point.setStatus(status);
        point.setOfficerInChargeName(officerName);
        point.setLastAuditedAt(LocalDateTime.now());
        return counterMeasureRepository.save(point);
    }

    /**
     * Doctor Commission & Performance Incentive Calculation
     */
    public void recordDoctorConsultation(String doctorRegNo, String doctorName, String dept, String doctorType, double wellnessScore) {
        DoctorIncentiveLedger ledger = incentiveRepository.findByDoctorRegNo(doctorRegNo).orElseGet(() -> {
            DoctorIncentiveLedger newLedger = new DoctorIncentiveLedger();
            newLedger.setDoctorRegNo(doctorRegNo);
            newLedger.setDoctorName(doctorName);
            newLedger.setDepartment(dept);
            newLedger.setDoctorType(doctorType != null ? doctorType : "CONSULTING_DOCTOR");
            newLedger.setCommissionPerCase(400.0);
            return newLedger;
        });

        int cases = ledger.getTotalCasesTreated() + 1;
        int successful = ledger.getSuccessfulResolutionsCount() + 1;
        double newAvgScore = ((ledger.getAveragePatientWellnessScore() * ledger.getTotalCasesTreated()) + wellnessScore) / cases;

        double commission = cases * ledger.getCommissionPerCase();
        double wellnessBonus = newAvgScore >= 4.5 ? (cases * 150.0) : 0.0;

        ledger.setTotalCasesTreated(cases);
        ledger.setSuccessfulResolutionsCount(successful);
        ledger.setAveragePatientWellnessScore(Math.round(newAvgScore * 100.0) / 100.0);
        ledger.setTotalCommissionEarned(commission);
        ledger.setWellnessBonusEarned(wellnessBonus);
        ledger.setUpdatedAt(LocalDateTime.now());

        incentiveRepository.save(ledger);
    }

    public List<DoctorIncentiveLedger> getDoctorIncentives() {
        return incentiveRepository.findAll();
    }

    private void createDefaultConsentForProforma(PatientProforma p) {
        DigitalConsentAgreement consent = new DigitalConsentAgreement();
        consent.setProformaId(p.getId());
        consent.setPatientName(p.getPatientName());
        consent.setPatientEmail(p.getPatientEmail());
        consent.setDoctorName(p.getAssignedDoctorName() != null ? p.getAssignedDoctorName() : "Attending Specialist");
        consent.setDoctorRegNo(p.getAssignedDoctorRegNo() != null ? p.getAssignedDoctorRegNo() : "MCI-HOSP-001");

        if (p.getAiTriageDepartment() == MedicalDepartment.PSYCHIATRY) {
            consent.setConsentType("PSYCHIATRIC_EVALUATION_CONSENT");
            consent.setTitle("Informed Consent & Confidentiality NOC for Psychiatric Assessment");
            consent.setTermsAndConditions("1. Patient agrees to comprehensive psychiatric assessment and psychotherapy evaluation. 2. Medical records are kept strictly confidential under mental healthcare standards. 3. Immediate crisis protocol will be initiated only under risk of harm.");
        } else if (p.getAiTriageDepartment() == MedicalDepartment.RADIOLOGY) {
            consent.setConsentType("RADIOLOGY_CONTRAST_NOC");
            consent.setTitle("Diagnostic Imaging & Contrast Administration Informed Consent NOC");
            consent.setTermsAndConditions("1. Patient gives consent for X-Ray/CT/MRI diagnostic imaging. 2. Patient confirms no unverified metal implants or contrast allergy history. 3. Safety radiation protocols verified by radiology team.");
        } else {
            consent.setConsentType("PROCEDURE_NOC");
            consent.setTitle("Standard Outpatient Consultation & Clinical Examination NOC");
            consent.setTermsAndConditions("1. Consent for non-invasive clinical vitals inspection and diagnostic triage. 2. Treatment plan and medications explained by attending doctor.");
        }

        consentRepository.save(consent);
    }

    private String generateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.substring(0, 24).toUpperCase();
        } catch (Exception e) {
            return "HASH-" + System.currentTimeMillis();
        }
    }

    private void seedSampleHealthcareData() {
        log.info("Seeding initial Hospital & Clinical Administration ecosystem...");

        // 1. Seed Sample Proformas
        PatientProforma p1 = triageAndRegisterPatient(
                "Sneha Kapoor",
                "patient@nagarseva.com",
                29,
                "Female",
                "A+",
                "+91 98111 22334",
                "Severe insomnia, panic episodes during work hours, persistent hopelessness and acute mood fluctuations for 3 weeks.",
                true
        );

        PatientProforma p2 = triageAndRegisterPatient(
                "Rohit Verma",
                "rohit.v@example.com",
                44,
                "Male",
                "O+",
                "+91 98222 33445",
                "Sudden sharp twist in left knee during football, loud popping sound with severe swelling and inability to bear weight.",
                false
        );

        PatientProforma p3 = triageAndRegisterPatient(
                "Meenakshi Sundaram",
                "meenakshi.s@example.com",
                58,
                "Female",
                "B+",
                "+91 98333 44556",
                "Heavy pressure over mid-chest radiating into left jaw, shortness of breath on climbing single flight of stairs.",
                false
        );

        // 2. Seed Stamped Prescriptions
        createAndStampPrescription(
                p1.getId(),
                p1.getPatientName(),
                p1.getPatientEmail(),
                "Dr. Ananya Roy (MD Psychiatry)",
                "MCI-PSY-44912",
                "Psychiatry & Behavioral Health",
                "CONSULTING_DOCTOR",
                "Generalized Anxiety Disorder with Secondary Sleep-Wake Cycle Disturbance",
                "[{\"name\":\"Tab. Escitalopram 10mg\",\"dosage\":\"10 mg\",\"frequency\":\"1-0-0 (Morning after breakfast)\",\"duration\":\"30 Days\",\"instructions\":\"Do not skip dose; avoid sudden withdrawal\"},{\"name\":\"Tab. Clonazepam 0.25mg\",\"dosage\":\"0.25 mg\",\"frequency\":\"0-0-1 (Bedtime SOS)\",\"duration\":\"10 Days\",\"instructions\":\"For acute panic/insomnia episodes\"}]",
                "Sleep hygiene protocols: Zero blue screen exposure 1 hour before sleep; 20 minutes morning mindfulness walk; follow-up in 4 weeks.",
                true
        );

        // 3. Seed Lab Test Requisitions
        LabTestRequest lab1 = createLabTestRequest(
                p2.getId(),
                p2.getPatientName(),
                p2.getPatientEmail(),
                "RADIOLOGY",
                "MRI Left Knee Joint (High Resolution)",
                "Evaluate ACL / Meniscus integrity following acute sports trauma with audible pop.",
                "Dr. Vikram Malhotra (MD Radiology)",
                "MCI-RAD-88120"
        );

        approveLabTestResult(
                lab1.getId(),
                "Sanjay Gupta (Senior MRI Technologist)",
                "Grade II sprain of Anterior Cruciate Ligament (ACL). Intact Medial and Lateral Menisci. Moderate joint effusion noted in suprapatellar bursa.",
                "Grade II ACL Sprain",
                "Intact Ligamentous Anatomy",
                "ABNORMAL_CRITICAL",
                "Dr. Vikram Malhotra (MD Radiology)",
                "MCI-RAD-88120"
        );

        // 4. Seed Medical Instruments
        MedicalInstrument i1 = new MedicalInstrument();
        i1.setName("Laparoscopic High-Definition Camera & Trocar Kit");
        i1.setDepartment("GENERAL_SURGERY");
        i1.setSerialNumber("INST-LAP-88901");
        i1.setTotalSterilizationCycles(42);
        i1.setMaxSafeCycles(120);
        i1.setStatus("READY_FOR_USE");
        i1.setLastSterilizedAt(LocalDateTime.now().minusHours(4));
        i1.setCertifiedSafeByOfficer("Officer K. Ramachandran (OT In-Charge)");
        instrumentRepository.save(i1);

        MedicalInstrument i2 = new MedicalInstrument();
        i2.setName("Clinical 32-Channel EEG Electroencephalograph Headset");
        i2.setDepartment("PSYCHIATRY");
        i2.setSerialNumber("INST-EEG-10492");
        i2.setTotalSterilizationCycles(98);
        i2.setMaxSafeCycles(100);
        i2.setStatus("REFURBISHMENT_REQUIRED");
        i2.setLastSterilizedAt(LocalDateTime.now().minusDays(1));
        i2.setRefurbishingNotes("Sensor calibration threshold nearing maximum recommended baseline. Sensor probe refurbishment scheduled.");
        i2.setCertifiedSafeByOfficer("Dr. Ananya Roy");
        instrumentRepository.save(i2);

        MedicalInstrument i3 = new MedicalInstrument();
        i3.setName("Digital Direct Radiography Flat Panel Detector");
        i3.setDepartment("RADIOLOGY");
        i3.setSerialNumber("INST-RAD-99201");
        i3.setTotalSterilizationCycles(12);
        i3.setMaxSafeCycles(200);
        i3.setStatus("READY_FOR_USE");
        i3.setLastSterilizedAt(LocalDateTime.now().minusHours(8));
        i3.setCertifiedSafeByOfficer("Dr. Vikram Malhotra");
        instrumentRepository.save(i3);

        // 5. Seed Emergency Counter Measure Points
        CounterMeasurePoint cmp1 = new CounterMeasurePoint();
        cmp1.setPointCode("CMP-TRAUMA-01");
        cmp1.setPointName("Trauma Bay Rapid Resuscitation Crash Cart Alpha");
        cmp1.setLocationArea("Ground Floor - Emergency Trauma Center");
        cmp1.setReadinessScore(100);
        cmp1.setSuppliesChecklist("[{\"item\":\"Defibrillator Pads\",\"status\":\"OK\"},{\"item\":\"Emergency Epinephrine Ampoules\",\"status\":\"OK\"},{\"item\":\"Intubation Stylet & Laryngoscope\",\"status\":\"OK\"}]");
        cmp1.setOfficerInChargeName("Nurse Supervisor Deepa Nair");
        cmp1.setOfficerInChargeContact("+91 98000 11223");
        cmp1.setStatus("READY_OPERATIONAL");
        counterMeasureRepository.save(cmp1);

        CounterMeasurePoint cmp2 = new CounterMeasurePoint();
        cmp2.setPointCode("CMP-O2-MANIFOLD");
        cmp2.setPointName("Central Oxygen Cylinder Manifold & Reserve Bank");
        cmp2.setLocationArea("Utility Wing - Sub-Level 1");
        cmp2.setReadinessScore(95);
        cmp2.setSuppliesChecklist("[{\"item\":\"Liquid O2 Bulk Tank Pressure\",\"status\":\"98% Capacity\"},{\"item\":\"Secondary Manifold 40-Cylinder Reserve\",\"status\":\"Tested & Ready\"}]");
        cmp2.setOfficerInChargeName("Eng. Harish Rawat (Biomedical Lead)");
        cmp2.setOfficerInChargeContact("+91 98000 44556");
        cmp2.setStatus("READY_OPERATIONAL");
        counterMeasureRepository.save(cmp2);

        // 6. Seed Doctor Incentive Ledgers
        recordDoctorConsultation("MCI-PSY-44912", "Dr. Ananya Roy (MD Psychiatry)", "Psychiatry & Behavioral Health", "CONSULTING_DOCTOR", 4.9);
        recordDoctorConsultation("MCI-RAD-88120", "Dr. Vikram Malhotra (MD Radiology)", "Radiology & Imaging", "RESIDING_DOCTOR", 4.8);
        recordDoctorConsultation("MCI-GEN-10294", "Dr. Rajeshwar Sharma (MD Medicine)", "General Medicine", "RESIDING_DOCTOR", 4.7);

        log.info("Hospital ecosystem seeded successfully with sample proformas, instruments, stamps, and counter-measure points.");
    }
}
