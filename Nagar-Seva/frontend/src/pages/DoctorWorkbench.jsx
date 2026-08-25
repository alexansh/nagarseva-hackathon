import React, { useEffect, useState, useMemo } from 'react';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function DoctorWorkbench() {
  const { user } = useAuth();
  const [proformas, setProformas] = useState([]);
  const [selectedProforma, setSelectedProforma] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Doctor Commission Stats
  const [incentives, setIncentives] = useState([]);

  // Prescription Form State
  const [rxForm, setRxForm] = useState({
    diagnosis: '',
    medications: [
      { name: 'Tab. Escitalopram 10mg', dosage: '10 mg', frequency: '1-0-0 (Morning)', duration: '30 Days', instructions: 'After breakfast' }
    ],
    lifestyleAdvice: 'Maintain hydration, avoid blue screen 1 hour before sleep.',
  });

  // Lab Request Form State
  const [labForm, setLabForm] = useState({
    testCategory: 'RADIOLOGY',
    testName: 'High Resolution MRI Brain / Contrast',
    clinicalIndication: 'Evaluate organic / structural intracranial etiology.',
  });

  // Handover Form State
  const [handoverModalOpen, setHandoverModalOpen] = useState(false);
  const [handoverForm, setHandoverForm] = useState({
    toDoctor: 'Dr. Vikram Malhotra (MD Radiology)',
    toDept: 'RADIOLOGY',
    reason: 'Urgent diagnostic imaging required for definitive diagnosis.',
  });

  const [activeTab, setActiveTab] = useState('PROFORMA'); // 'PROFORMA' | 'PRESCRIPTION' | 'LABS' | 'INCENTIVES'

  useEffect(() => {
    fetchInitialData();
  }, []);

  const fetchInitialData = async () => {
    setLoading(true);
    setError('');
    try {
      const [proformasRes, incentivesRes] = await Promise.allSettled([
        apiClient.get('/api/health/proformas'),
        apiClient.get('/api/health/doctor-incentives'),
      ]);

      if (proformasRes.status === 'fulfilled') {
        const data = Array.isArray(proformasRes.value.data) ? proformasRes.value.data : [];
        setProformas(data);
        if (data.length > 0 && !selectedProforma) {
          setSelectedProforma(data[0]);
        }
      }
      if (incentivesRes.status === 'fulfilled') {
        setIncentives(Array.isArray(incentivesRes.value.data) ? incentivesRes.value.data : []);
      }
    } catch (err) {
      console.error('Error loading doctor workbench data:', err);
      setError('Unable to load clinical records from backend hospital database.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddMedication = () => {
    setRxForm(prev => ({
      ...prev,
      medications: [
        ...prev.medications,
        { name: '', dosage: '', frequency: '1-0-1', duration: '7 Days', instructions: 'After meals' }
      ]
    }));
  };

  const handleUpdateMedication = (index, field, value) => {
    setRxForm(prev => {
      const updated = [...prev.medications];
      updated[index] = { ...updated[index], [field]: value };
      return { ...prev, medications: updated };
    });
  };

  const handleRemoveMedication = (index) => {
    setRxForm(prev => ({
      ...prev,
      medications: prev.medications.filter((_, idx) => idx !== index)
    }));
  };

  const handleIssueStampedPrescription = async (e) => {
    e.preventDefault();
    if (!selectedProforma) return;

    try {
      const payload = {
        proformaId: selectedProforma.id,
        patientName: selectedProforma.patientName,
        patientEmail: selectedProforma.patientEmail,
        doctorName: user?.name || selectedProforma.assignedDoctorName || 'Dr. Ananya Roy',
        doctorRegNo: selectedProforma.assignedDoctorRegNo || 'MCI-PSY-44912',
        doctorDepartment: selectedProforma.aiTriageDepartment || 'PSYCHIATRY',
        doctorType: selectedProforma.doctorRoleType || 'CONSULTING_DOCTOR',
        diagnosisSummary: rxForm.diagnosis || 'Clinical Diagnosis Confirmed',
        medicationsJson: JSON.stringify(rxForm.medications),
        lifestyleAdvice: rxForm.lifestyleAdvice,
        approveStampNow: true,
      };

      await apiClient.post('/api/health/prescriptions', payload);
      alert('✓ Official Digital Prescription Stamped & Synced to Patient Health Vault!');
      fetchInitialData();
    } catch (err) {
      console.error('Prescription stamping error:', err);
      alert('Failed to stamp prescription. Please verify backend connection.');
    }
  };

  const handleRequestLabTest = async (e) => {
    e.preventDefault();
    if (!selectedProforma) return;

    try {
      const payload = {
        proformaId: selectedProforma.id,
        patientName: selectedProforma.patientName,
        patientEmail: selectedProforma.patientEmail,
        testCategory: labForm.testCategory,
        testName: labForm.testName,
        clinicalIndication: labForm.clinicalIndication,
        orderingDoctorName: user?.name || selectedProforma.assignedDoctorName || 'Attending Physician',
        orderingDoctorRegNo: selectedProforma.assignedDoctorRegNo || 'MCI-DOC-102',
      };

      await apiClient.post('/api/health/lab-tests', payload);
      alert('✓ Diagnostic Lab Requisition Stamped & Sent to Laboratory Queue!');
      fetchInitialData();
    } catch (err) {
      console.error('Lab test request error:', err);
      alert('Failed to order lab test.');
    }
  };

  const handleExecuteHandover = async (e) => {
    e.preventDefault();
    if (!selectedProforma) return;

    try {
      const payload = {
        fromDoctorName: user?.name || selectedProforma.assignedDoctorName || 'Referring Doctor',
        fromDepartment: selectedProforma.aiTriageDepartment || 'GENERAL_MEDICINE',
        toDoctorName: handoverForm.toDoctor,
        toDepartment: handoverForm.toDept,
        reason: handoverForm.reason,
      };

      const res = await apiClient.post(`/api/health/proformas/${selectedProforma.id}/handover`, payload);
      alert(`✓ Inter-Department Case Handover Executed! AI SBAR Briefing generated for ${handoverForm.toDoctor}`);
      setHandoverModalOpen(false);
      fetchInitialData();
    } catch (err) {
      console.error('Handover error:', err);
      alert('Failed to execute case handover.');
    }
  };

  const filteredProformas = useMemo(() => {
    return proformas.filter(p => {
      if (departmentFilter !== 'ALL' && p.aiTriageDepartment !== departmentFilter) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const nameMatch = (p.patientName || '').toLowerCase().includes(q);
        const tokenMatch = (p.tokenNumber || '').toLowerCase().includes(q);
        const complaintMatch = (p.chiefComplaint || '').toLowerCase().includes(q);
        return nameMatch || tokenMatch || complaintMatch;
      }
      return true;
    });
  }, [proformas, departmentFilter, searchQuery]);

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100/90 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-emerald-50 border border-emerald-200 rounded-full text-xs font-bold text-emerald-800 mb-1.5">
            <span>🛡️</span>
            <span>Attending Physician Clinical Cockpit • MCI Stamped Portal</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Doctor Clinical Workbench
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-0.5">
            Active Doctor: <span className="font-bold text-gray-800">{user?.name || 'Dr. Ananya Roy (MD Psychiatry)'}</span> • Reg: <span className="font-mono font-semibold text-gray-600">{user?.department || 'MCI-PSY-44912'}</span>
          </p>
        </div>

        {/* Tab Navigation */}
        <div className="flex items-center gap-1 bg-gray-100 p-1 rounded-full overflow-x-auto">
          {[
            { id: 'PROFORMA', label: '📋 Patient Proformas' },
            { id: 'PRESCRIPTION', label: '✍️ Stamp Prescription' },
            { id: 'LABS', label: '🔬 Requisition Labs' },
            { id: 'INCENTIVES', label: '💰 Performance Pay' },
          ].map(t => (
            <button
              key={t.id}
              onClick={() => setActiveTab(t.id)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-bold transition whitespace-nowrap ${
                activeTab === t.id
                  ? 'bg-[#7c5cff] text-white shadow-xs'
                  : 'text-gray-600 hover:text-gray-900 hover:bg-gray-200'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 text-rose-800 text-xs rounded-2xl border border-rose-200">
          {error}
        </div>
      )}

      {loading && (
        <div className="py-16 text-center text-xs text-gray-400">
          <div className="w-8 h-8 border-2 border-[#7c5cff] border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          Loading Doctor Workbench clinical records...
        </div>
      )}

      {/* Main Workspace Layout */}
      {!loading && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left Column: Patient Queue (4 Cols) */}
          <div className="lg:col-span-4 space-y-4">
            <div className="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-gray-400">
                  OPD Patients Queue ({filteredProformas.length})
                </span>
                <button
                  onClick={fetchInitialData}
                  className="text-xs font-semibold text-[#7c5cff] hover:underline"
                >
                  🔄 Refresh
                </button>
              </div>

              {/* Department Selector */}
              <select
                value={departmentFilter}
                onChange={(e) => setDepartmentFilter(e.target.value)}
                className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
              >
                <option value="ALL">🌐 All Medical Departments</option>
                <option value="PSYCHIATRY">🧠 Psychiatry & Behavioral Health</option>
                <option value="RADIOLOGY">🩻 Radiology & Imaging</option>
                <option value="CARDIOLOGY">❤️ Cardiology</option>
                <option value="ORTHOPEDICS">🦴 Orthopedics & Joint Care</option>
                <option value="GENERAL_MEDICINE">🩺 General Medicine</option>
                <option value="DERMATOLOGY">🧴 Dermatology</option>
                <option value="ENT">👂 ENT Clinic</option>
              </select>

              {/* Search */}
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search patient, token, symptoms..."
                className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
              />

              {/* Patient List */}
              <div className="space-y-2 max-h-[560px] overflow-y-auto pr-1">
                {filteredProformas.map(p => {
                  const isSelected = selectedProforma?.id === p.id;
                  return (
                    <div
                      key={p.id}
                      onClick={() => setSelectedProforma(p)}
                      className={`p-3.5 rounded-2xl border transition cursor-pointer text-left ${
                        isSelected
                          ? 'bg-violet-50/80 border-[#7c5cff] shadow-xs'
                          : 'bg-gray-50/60 border-gray-100 hover:bg-gray-100/80'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <span className="font-extrabold text-xs text-gray-900 truncate">
                          {p.patientName} ({p.patientAge}y, {p.gender?.[0]})
                        </span>
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-violet-100 text-violet-800">
                          {p.tokenNumber}
                        </span>
                      </div>
                      <p className="text-[11px] text-gray-500 line-clamp-2 leading-relaxed">
                        {p.chiefComplaint}
                      </p>
                      <div className="flex items-center justify-between gap-2 mt-2 pt-1 border-t border-gray-200/50 text-[10px]">
                        <span className="font-bold text-violet-900">
                          🏥 {p.aiTriageDepartment}
                        </span>
                        <span className="font-bold text-gray-400">
                          {p.status}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>

          {/* Right Column: Active Proforma / Clinical Suite (8 Cols) */}
          <div className="lg:col-span-8 space-y-4">
            {selectedProforma ? (
              <div className="space-y-4">
                {/* Patient Case Overview Card */}
                <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-gray-100">
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-xl font-extrabold text-gray-900">
                          {selectedProforma.patientName}
                        </h2>
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-violet-100 text-violet-900">
                          {selectedProforma.tokenNumber}
                        </span>
                        {selectedProforma.isTeleConsult && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                            📹 Tele-Consult
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Age: {selectedProforma.patientAge} • Gender: {selectedProforma.gender} • Blood Group: {selectedProforma.bloodGroup} • Contact: {selectedProforma.contactNumber}
                      </p>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setHandoverModalOpen(true)}
                        className="px-3.5 py-1.5 bg-amber-50 hover:bg-amber-100 border border-amber-200 text-amber-900 text-xs font-bold rounded-full transition shadow-xs flex items-center gap-1.5"
                      >
                        <span>🔄</span>
                        <span>Zero-Resistance Handover</span>
                      </button>
                    </div>
                  </div>

                  {/* Vitals Ribbon */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 bg-gray-50 p-3 rounded-2xl border border-gray-100 text-xs">
                    <div>
                      <span className="text-gray-400 text-[10px] uppercase font-bold block">Blood Pressure</span>
                      <span className="font-extrabold text-gray-800">{selectedProforma.vitalsBp || '120/80 mmHg'}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 text-[10px] uppercase font-bold block">Pulse Rate</span>
                      <span className="font-extrabold text-gray-800">{selectedProforma.vitalsPulse || '74 bpm'}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 text-[10px] uppercase font-bold block">Body Temp</span>
                      <span className="font-extrabold text-gray-800">{selectedProforma.vitalsTemp || '98.6 °F'}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 text-[10px] uppercase font-bold block">Oxygen SpO2</span>
                      <span className="font-extrabold text-emerald-700">{selectedProforma.vitalsSpo2 || '99%'}</span>
                    </div>
                  </div>

                  {/* Chief Complaint & AI Triage */}
                  <div className="space-y-2 text-xs">
                    <div>
                      <span className="font-bold text-gray-700 uppercase tracking-wider text-[11px]">
                        Patient Self-Told Chief Complaint:
                      </span>
                      <p className="text-gray-800 bg-violet-50/50 p-3 rounded-xl border border-violet-100/70 mt-1 leading-relaxed">
                        "{selectedProforma.chiefComplaint}"
                      </p>
                    </div>

                    <div>
                      <span className="font-bold text-violet-900 uppercase tracking-wider text-[11px]">
                        🤖 AI Clinical Triage Assessment:
                      </span>
                      <p className="text-gray-700 bg-gray-50 p-3 rounded-xl border border-gray-100 mt-1 leading-relaxed">
                        {selectedProforma.aiClinicalRationale}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Sub-View Depending on Active Tab */}
                {activeTab === 'PRESCRIPTION' && (
                  <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-5">
                    <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                      <div>
                        <h3 className="text-base font-extrabold text-gray-900">
                          ✍️ Stamped Digital Prescription Suite
                        </h3>
                        <p className="text-xs text-gray-500 mt-0.5">
                          Create tamper-evident prescription with official MCI signature stamp
                        </p>
                      </div>
                      <span className="px-3 py-1 bg-emerald-50 border border-emerald-200 rounded-full text-emerald-800 text-xs font-bold">
                        MCI Official Seal Enabled
                      </span>
                    </div>

                    <form onSubmit={handleIssueStampedPrescription} className="space-y-4">
                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Clinical Diagnosis Summary
                        </label>
                        <input
                          type="text"
                          required
                          value={rxForm.diagnosis}
                          onChange={(e) => setRxForm(prev => ({ ...prev, diagnosis: e.target.value }))}
                          placeholder="e.g. Generalized Anxiety with Moderate Insomnia / Grade II Ligament Sprain"
                          className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs sm:text-sm focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                        />
                      </div>

                      {/* Medication Schedule List */}
                      <div>
                        <div className="flex items-center justify-between mb-2">
                          <label className="block text-xs font-bold text-gray-700">
                            Prescribed Medications & Dosage Schedule
                          </label>
                          <button
                            type="button"
                            onClick={handleAddMedication}
                            className="text-xs font-bold text-[#7c5cff] hover:underline"
                          >
                            ➕ Add Medication
                          </button>
                        </div>

                        <div className="space-y-2">
                          {rxForm.medications.map((med, idx) => (
                            <div key={idx} className="p-3 bg-gray-50 border border-gray-200 rounded-2xl grid grid-cols-1 sm:grid-cols-12 gap-2 items-center text-xs">
                              <div className="sm:col-span-4">
                                <input
                                  type="text"
                                  placeholder="Medicine Name (e.g. Tab. Escitalopram 10mg)"
                                  value={med.name}
                                  onChange={(e) => handleUpdateMedication(idx, 'name', e.target.value)}
                                  className="w-full px-2.5 py-1.5 bg-white border border-gray-200 rounded-lg"
                                />
                              </div>
                              <div className="sm:col-span-2">
                                <input
                                  type="text"
                                  placeholder="Dosage (10mg)"
                                  value={med.dosage}
                                  onChange={(e) => handleUpdateMedication(idx, 'dosage', e.target.value)}
                                  className="w-full px-2.5 py-1.5 bg-white border border-gray-200 rounded-lg"
                                />
                              </div>
                              <div className="sm:col-span-3">
                                <input
                                  type="text"
                                  placeholder="Frequency (1-0-1)"
                                  value={med.frequency}
                                  onChange={(e) => handleUpdateMedication(idx, 'frequency', e.target.value)}
                                  className="w-full px-2.5 py-1.5 bg-white border border-gray-200 rounded-lg"
                                />
                              </div>
                              <div className="sm:col-span-2">
                                <input
                                  type="text"
                                  placeholder="Duration (30 Days)"
                                  value={med.duration}
                                  onChange={(e) => handleUpdateMedication(idx, 'duration', e.target.value)}
                                  className="w-full px-2.5 py-1.5 bg-white border border-gray-200 rounded-lg"
                                />
                              </div>
                              <div className="sm:col-span-1 text-center">
                                {rxForm.medications.length > 1 && (
                                  <button
                                    type="button"
                                    onClick={() => handleRemoveMedication(idx)}
                                    className="text-rose-600 font-bold hover:text-rose-800"
                                  >
                                    ✕
                                  </button>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Lifestyle Advice & Follow-up Guidance
                        </label>
                        <textarea
                          rows={2}
                          value={rxForm.lifestyleAdvice}
                          onChange={(e) => setRxForm(prev => ({ ...prev, lifestyleAdvice: e.target.value }))}
                          placeholder="e.g. 20 min morning mindfulness, avoid heavy weight lifting, follow-up in 3 weeks..."
                          className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                        />
                      </div>

                      <div className="pt-2">
                        <button
                          type="submit"
                          className="w-full py-3 px-6 rounded-full bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs sm:text-sm font-bold shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                        >
                          <span>🛡️ Apply MCI Digital Stamp Seal & Issue Prescription →</span>
                        </button>
                      </div>
                    </form>
                  </div>
                )}

                {activeTab === 'LABS' && (
                  <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                    <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                      <div>
                        <h3 className="text-base font-extrabold text-gray-900">
                          🔬 Diagnostic Lab Test Requisition
                        </h3>
                        <p className="text-xs text-gray-500 mt-0.5">
                          Order radiology imaging, pathology panels, and psychiatric inventories with doctor requisition stamp
                        </p>
                      </div>
                    </div>

                    <form onSubmit={handleRequestLabTest} className="space-y-4">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                        <div>
                          <label className="block text-xs font-bold text-gray-700 mb-1">
                            Diagnostic Category
                          </label>
                          <select
                            value={labForm.testCategory}
                            onChange={(e) => setLabForm(prev => ({ ...prev, testCategory: e.target.value }))}
                            className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                          >
                            <option value="RADIOLOGY">🩻 Radiology (MRI / CT / X-Ray)</option>
                            <option value="PATHOLOGY">🧪 Pathology (Blood / Urine / Bio)</option>
                            <option value="PSYCHIATRY_INVENTORY">🧠 Psychiatric Assessment Battery</option>
                            <option value="CARDIOLOGY_ECG">❤️ Cardiology (12-Lead ECG / Echo)</option>
                          </select>
                        </div>

                        <div>
                          <label className="block text-xs font-bold text-gray-700 mb-1">
                            Specific Investigation Name
                          </label>
                          <input
                            type="text"
                            required
                            value={labForm.testName}
                            onChange={(e) => setLabForm(prev => ({ ...prev, testName: e.target.value }))}
                            placeholder="e.g. MRI Left Knee / Complete Blood Count / HDRS Score"
                            className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-xs font-bold text-gray-700 mb-1">
                          Clinical Indication & Suspected Pathology
                        </label>
                        <textarea
                          rows={2}
                          required
                          value={labForm.clinicalIndication}
                          onChange={(e) => setLabForm(prev => ({ ...prev, clinicalIndication: e.target.value }))}
                          placeholder="e.g. Rule out ACL tear following acute sports trauma / Assess neuro-inflammatory markers..."
                          className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                        />
                      </div>

                      <div className="pt-2">
                        <button
                          type="submit"
                          className="w-full py-3 px-6 rounded-full bg-gray-900 hover:bg-black text-white text-xs sm:text-sm font-bold shadow-md transition flex items-center justify-center gap-2 cursor-pointer"
                        >
                          <span>🔬 Stamp Requisition & Send to Lab Queue →</span>
                        </button>
                      </div>
                    </form>
                  </div>
                )}

                {activeTab === 'INCENTIVES' && (
                  <div className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                    <div>
                      <h3 className="text-base font-extrabold text-gray-900">
                        💰 Doctor Performance & Wellness Incentive Ledger
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Performance-based commission calculated by case count and verified patient recovery wellness scores
                      </p>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                      {incentives.map(inc => (
                        <div key={inc.id} className="p-4 rounded-2xl bg-gradient-to-br from-violet-50/60 to-purple-50/40 border border-violet-100 space-y-2">
                          <span className="font-extrabold text-xs text-gray-900 block truncate">
                            {inc.doctorName}
                          </span>
                          <span className="text-[11px] text-gray-500 block">
                            Reg: {inc.doctorRegNo} • {inc.department}
                          </span>
                          <div className="pt-2 border-t border-violet-100 flex items-center justify-between text-xs">
                            <span className="text-gray-500">Cases Treated:</span>
                            <span className="font-extrabold text-gray-900">{inc.totalCasesTreated}</span>
                          </div>
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-gray-500">Wellness Score:</span>
                            <span className="font-extrabold text-emerald-700">★ {inc.averagePatientWellnessScore} / 5.0</span>
                          </div>
                          <div className="flex items-center justify-between text-xs">
                            <span className="text-gray-500">Commission Total:</span>
                            <span className="font-extrabold text-violet-900">₹{inc.totalCommissionEarned + inc.wellnessBonusEarned}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <div className="bg-white rounded-3xl p-12 text-center border border-gray-100 text-xs text-gray-400">
                Please select a patient from the left queue to open their clinical proforma.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Zero-Resistance Inter-Doctor Handover Modal */}
      {handoverModalOpen && selectedProforma && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-gray-100 w-full max-w-lg p-6 sm:p-8 animate-in fade-in zoom-in-95 duration-150 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-lg font-extrabold text-gray-900 tracking-tight">
                  Zero-Resistance Inter-Doctor Handover
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  Transfer Patient #{selectedProforma.tokenNumber} with automated AI clinical SBAR briefing
                </p>
              </div>
              <button
                onClick={() => setHandoverModalOpen(false)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center text-xs font-bold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleExecuteHandover} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Target Medical Department
                </label>
                <select
                  value={handoverForm.toDept}
                  onChange={(e) => setHandoverForm(prev => ({ ...prev, toDept: e.target.value }))}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                >
                  <option value="PSYCHIATRY">🧠 Psychiatry & Behavioral Health</option>
                  <option value="RADIOLOGY">🩻 Radiology & Diagnostic Imaging</option>
                  <option value="CARDIOLOGY">❤️ Cardiology & Critical Heart Care</option>
                  <option value="ORTHOPEDICS">🦴 Orthopedics & Joint Surgery</option>
                  <option value="GENERAL_MEDICINE">🩺 General Internal Medicine</option>
                  <option value="EMERGENCY_CARE">🚨 Emergency Resuscitation Center</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Receiving Specialist / Doctor Name
                </label>
                <input
                  type="text"
                  required
                  value={handoverForm.toDoctor}
                  onChange={(e) => setHandoverForm(prev => ({ ...prev, toDoctor: e.target.value }))}
                  placeholder="e.g. Dr. Vikram Malhotra (MD Radiology)"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Clinical Reason for Transfer
                </label>
                <textarea
                  rows={3}
                  required
                  value={handoverForm.reason}
                  onChange={(e) => setHandoverForm(prev => ({ ...prev, reason: e.target.value }))}
                  placeholder="e.g. High grade trauma requiring immediate radiological imaging and joint stabilization..."
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setHandoverModalOpen(false)}
                  className="px-4 py-2 rounded-full text-xs font-semibold text-gray-600 hover:bg-gray-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-full bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs font-bold shadow-sm transition"
                >
                  Confirm Handover & Generate SBAR →
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
