import React, { useEffect, useState } from 'react';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

const DEPARTMENT_SCHEDULES = [
  {
    department: 'Psychiatry & Behavioral Health',
    icon: '🧠',
    leadDoctor: 'Dr. Ananya Roy (MD Psychiatry)',
    residingDoctors: 2,
    consultingDoctors: 4,
    shiftTiming: '02:00 PM - 07:00 PM',
    teleConsultAvailable: true,
    status: 'ACTIVE_CONSULTING',
  },
  {
    department: 'Radiology & Imaging',
    icon: '🩻',
    leadDoctor: 'Dr. Vikram Malhotra (MD Radiology)',
    residingDoctors: 3,
    consultingDoctors: 2,
    shiftTiming: '08:00 AM - 02:00 PM (Emergency 24x7)',
    teleConsultAvailable: false,
    status: 'ACTIVE_IMAGING',
  },
  {
    department: 'Cardiology & Intensive Heart Care',
    icon: '❤️',
    leadDoctor: 'Dr. Sameer Deshmukh (DM Cardiology)',
    residingDoctors: 4,
    consultingDoctors: 3,
    shiftTiming: '09:00 AM - 05:00 PM',
    teleConsultAvailable: true,
    status: 'ACTIVE_CONSULTING',
  },
  {
    department: 'Orthopedics & Joint Care',
    icon: '🦴',
    leadDoctor: 'Dr. Alok Verma (MS Ortho)',
    residingDoctors: 2,
    consultingDoctors: 3,
    shiftTiming: '09:00 AM - 04:00 PM',
    teleConsultAvailable: false,
    status: 'ACTIVE_CONSULTING',
  },
  {
    department: 'General Internal Medicine',
    icon: '🩺',
    leadDoctor: 'Dr. Rajeshwar Sharma (MD Medicine)',
    residingDoctors: 5,
    consultingDoctors: 6,
    shiftTiming: '08:00 AM - 08:00 PM',
    teleConsultAvailable: true,
    status: 'ACTIVE_OPD',
  },
  {
    department: 'Emergency & Trauma Care',
    icon: '🚨',
    leadDoctor: 'Dr. Priya Nambiar (CMO Emergency)',
    residingDoctors: 8,
    consultingDoctors: 4,
    shiftTiming: '24 Hours Open (Continuous Shift)',
    teleConsultAvailable: false,
    status: 'HIGH_PRIORITY_READY',
  },
];

export default function HospitalOperations() {
  const { user } = useAuth();
  const [instruments, setInstruments] = useState([]);
  const [counterMeasures, setCounterMeasures] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('INSTRUMENTS'); // 'INSTRUMENTS' | 'EMERGENCY' | 'SCHEDULES' | 'DOCTORS'

  useEffect(() => {
    fetchOperationsData();
  }, []);

  const fetchOperationsData = async () => {
    setLoading(true);
    setError('');
    try {
      const [iRes, cRes] = await Promise.allSettled([
        apiClient.get('/api/health/instruments'),
        apiClient.get('/api/health/counter-measures'),
      ]);

      if (iRes.status === 'fulfilled') setInstruments(Array.isArray(iRes.value.data) ? iRes.value.data : []);
      if (cRes.status === 'fulfilled') setCounterMeasures(Array.isArray(cRes.value.data) ? cRes.value.data : []);
    } catch (err) {
      console.error('Error loading operations data:', err);
      setError('Unable to load hospital operations records.');
    } finally {
      setLoading(false);
    }
  };

  const handleMaintainInstrument = async (id, isFullRefurbish) => {
    try {
      await apiClient.patch(`/api/health/instruments/${id}/maintenance`, {
        officerName: user?.name || 'Chief Biomedical Engineer',
        notes: isFullRefurbish ? 'Complete calibration & probe replacement' : 'Autoclave steam sterilization cycle',
        isFullRefurbish,
      });
      alert(isFullRefurbish ? '✓ Instrument Refurbished & Recalibrated!' : '✓ Sterilization Cycle Logged!');
      fetchOperationsData();
    } catch (err) {
      console.error('Maintenance error:', err);
      alert('Failed to update instrument status.');
    }
  };

  const handleAuditPoint = async (id) => {
    try {
      await apiClient.patch(`/api/health/counter-measures/${id}/audit`, {
        readinessScore: 100,
        status: 'READY_OPERATIONAL',
        officerName: user?.name || 'Hospital Quality Auditor',
      });
      alert('✓ Emergency Counter-Measure Point Audited & Certified 100% Operational!');
      fetchOperationsData();
    } catch (err) {
      console.error('Audit error:', err);
      alert('Failed to audit counter-measure point.');
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100/90 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-amber-50 border border-amber-200 rounded-full text-xs font-bold text-amber-900 mb-2">
            <span>🏥</span>
            <span>Hospital Administration, Instrument Refurbishing & Emergency SOPs</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Hospital Operations & Clinical Preparedness
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-1">
            Department scheduling, medical equipment sterilization lifecycle, counter-measure points, and verified doctor credentials.
          </p>
        </div>

        <button
          onClick={fetchOperationsData}
          className="px-4 py-2 bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 text-xs font-bold rounded-full transition shadow-xs self-start md:self-auto"
        >
          🔄 Refresh Data
        </button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 bg-white p-1.5 rounded-2xl shadow-sm border border-gray-100 overflow-x-auto">
        {[
          { id: 'INSTRUMENTS', label: `🛠️ Instrument Refurbishing (${instruments.length})` },
          { id: 'EMERGENCY', label: `🚨 Counter-Measure Points (${counterMeasures.length})` },
          { id: 'SCHEDULES', label: `⏰ Department Shift Rosters (${DEPARTMENT_SCHEDULES.length})` },
          { id: 'DOCTORS', label: `⭐ Trusted Doctor Directory` },
        ].map(t => (
          <button
            key={t.id}
            onClick={() => setActiveTab(t.id)}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition whitespace-nowrap ${
              activeTab === t.id
                ? 'bg-[#7c5cff] text-white shadow-xs'
                : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && (
        <div className="p-4 bg-rose-50 text-rose-800 text-xs rounded-2xl border border-rose-200">
          {error}
        </div>
      )}

      {loading && (
        <div className="py-16 text-center text-xs text-gray-400">
          <div className="w-8 h-8 border-2 border-[#7c5cff] border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
          Loading operations data...
        </div>
      )}

      {!loading && (
        <div className="space-y-4">
          {/* Instruments Tab */}
          {activeTab === 'INSTRUMENTS' && (
            <div className="space-y-4">
              <div className="bg-gradient-to-r from-blue-50 to-indigo-50 p-4 rounded-2xl border border-blue-100 text-xs text-blue-900">
                <span className="font-bold">Medical Instrument Lifecycle Policy:</span> All surgical and diagnostic instruments undergo autoclave sterilization tracking. Instruments nearing maximum cycle limits are automatically flagged for refurbishment and recalibration.
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {instruments.map(inst => {
                  const cyclePercentage = Math.min(100, Math.round((inst.totalSterilizationCycles / inst.maxSafeCycles) * 100));
                  return (
                    <div key={inst.id} className="bg-white rounded-3xl p-5 shadow-sm border border-gray-100 flex flex-col justify-between space-y-4">
                      <div className="space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="px-2 py-0.5 rounded-md text-[10px] font-mono font-bold bg-gray-100 text-gray-700">
                            {inst.serialNumber}
                          </span>
                          <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-bold ${
                            inst.status === 'READY_FOR_USE'
                              ? 'bg-emerald-100 text-emerald-800'
                              : 'bg-amber-100 text-amber-800 animate-pulse'
                          }`}>
                            {inst.status.replace('_', ' ')}
                          </span>
                        </div>

                        <h3 className="text-sm font-extrabold text-gray-900">
                          {inst.name}
                        </h3>
                        <p className="text-xs text-gray-500">
                          Dept: <span className="font-semibold text-gray-700">{inst.department}</span>
                        </p>

                        {/* Sterilization Cycles Progress Bar */}
                        <div className="space-y-1 pt-1">
                          <div className="flex justify-between text-[11px]">
                            <span className="text-gray-500">Sterilization Cycles:</span>
                            <span className="font-bold text-gray-900">{inst.totalSterilizationCycles} / {inst.maxSafeCycles}</span>
                          </div>
                          <div className="w-full h-2 bg-gray-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full rounded-full ${cyclePercentage > 80 ? 'bg-amber-500' : 'bg-emerald-500'}`}
                              style={{ width: `${cyclePercentage}%` }}
                            ></div>
                          </div>
                        </div>

                        {inst.refurbishingNotes && (
                          <p className="text-[11px] text-gray-600 bg-gray-50 p-2.5 rounded-xl border border-gray-100 italic">
                            {inst.refurbishingNotes}
                          </p>
                        )}
                      </div>

                      <div className="flex items-center gap-2 pt-2 border-t border-gray-100">
                        <button
                          onClick={() => handleMaintainInstrument(inst.id, false)}
                          className="flex-1 py-1.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 text-gray-800 text-[11px] font-bold rounded-xl transition"
                        >
                          🧼 Sterilize (+1)
                        </button>
                        <button
                          onClick={() => handleMaintainInstrument(inst.id, true)}
                          className="flex-1 py-1.5 bg-violet-50 hover:bg-violet-100 border border-violet-200 text-violet-900 text-[11px] font-bold rounded-xl transition"
                        >
                          🔧 Refurbish (Reset)
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Emergency Counter-Measure Points */}
          {activeTab === 'EMERGENCY' && (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {counterMeasures.map(point => (
                  <div key={point.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                    <div className="flex items-center justify-between">
                      <span className="px-2.5 py-0.5 rounded-full text-xs font-mono font-bold bg-rose-50 text-rose-800 border border-rose-200">
                        {point.pointCode}
                      </span>
                      <span className="text-xs font-extrabold text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-full border border-emerald-200">
                        Readiness: {point.readinessScore}%
                      </span>
                    </div>

                    <div>
                      <h3 className="text-base font-extrabold text-gray-900">
                        {point.pointName}
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        📍 Location: <span className="font-semibold text-gray-700">{point.locationArea}</span>
                      </p>
                      <p className="text-xs text-gray-500 mt-0.5">
                        In-Charge: <span className="font-semibold text-gray-700">{point.officerInChargeName}</span> ({point.officerInChargeContact})
                      </p>
                    </div>

                    <div className="pt-2 border-t border-gray-100 flex items-center justify-between">
                      <span className="text-[11px] text-gray-400">
                        Status: <strong className="text-emerald-700">{point.status}</strong>
                      </span>
                      <button
                        onClick={() => handleAuditPoint(point.id)}
                        className="px-3.5 py-1.5 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 text-emerald-900 text-xs font-bold rounded-full transition"
                      >
                        ✓ Perform Audit & Certify
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Department Schedules */}
          {activeTab === 'SCHEDULES' && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {DEPARTMENT_SCHEDULES.map((dept, idx) => (
                <div key={idx} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-2xl">{dept.icon}</span>
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-violet-50 text-violet-800">
                      {dept.status}
                    </span>
                  </div>

                  <h3 className="text-base font-extrabold text-gray-900">
                    {dept.department}
                  </h3>

                  <div className="space-y-1.5 text-xs text-gray-600">
                    <p>Lead: <strong className="text-gray-800">{dept.leadDoctor}</strong></p>
                    <p>⏰ Shift: <strong className="text-gray-800">{dept.shiftTiming}</strong></p>
                    <p>Staff: <span>{dept.residingDoctors} Residing • {dept.consultingDoctors} Consulting</span></p>
                  </div>

                  <div className="pt-2 border-t border-gray-100 flex items-center justify-between text-[11px]">
                    <span className={dept.teleConsultAvailable ? 'text-blue-700 font-bold' : 'text-gray-400'}>
                      {dept.teleConsultAvailable ? '📹 Tele-Consult Active' : '🏥 In-Hospital Only'}
                    </span>
                    <span className="text-emerald-700 font-bold">✓ Tokens Open</span>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Trusted Doctor Directory */}
          {activeTab === 'DOCTORS' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {[
                { name: 'Dr. Ananya Roy', role: 'CONSULTING_DOCTOR', dept: 'Psychiatry & Behavioral Health', reg: 'MCI-PSY-44912', exp: '12 Years', rating: 4.9, cases: 480, trustedBadge: true },
                { name: 'Dr. Vikram Malhotra', role: 'RESIDING_DOCTOR', dept: 'Radiology & Imaging', reg: 'MCI-RAD-88120', exp: '15 Years', rating: 4.8, cases: 620, trustedBadge: true },
                { name: 'Dr. Sameer Deshmukh', role: 'CONSULTING_DOCTOR', dept: 'Cardiology', reg: 'MCI-CARD-55019', exp: '18 Years', rating: 4.9, cases: 890, trustedBadge: true },
                { name: 'Dr. Rajeshwar Sharma', role: 'RESIDING_DOCTOR', dept: 'General Internal Medicine', reg: 'MCI-GEN-10294', exp: '9 Years', rating: 4.7, cases: 1150, trustedBadge: true },
                { name: 'Dr. Priya Nambiar', role: 'CMO_OFFICER', dept: 'Emergency & Trauma Care', reg: 'MCI-CMO-00192', exp: '20 Years', rating: 5.0, cases: 1400, trustedBadge: true },
              ].map((doc, idx) => (
                <div key={idx} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200">
                      ✓ MCI Certified Doctor
                    </span>
                    <span className="font-extrabold text-xs text-amber-600">
                      ★ {doc.rating} / 5.0
                    </span>
                  </div>

                  <div>
                    <h3 className="text-base font-extrabold text-gray-900">
                      {doc.name}
                    </h3>
                    <p className="text-xs text-violet-700 font-semibold">{doc.dept}</p>
                    <p className="text-[11px] text-gray-400 mt-0.5">Reg: {doc.reg} • {doc.role.replace('_', ' ')}</p>
                  </div>

                  <div className="pt-2 border-t border-gray-100 grid grid-cols-2 gap-2 text-xs">
                    <div>
                      <span className="text-gray-400 text-[10px] block">Experience</span>
                      <span className="font-bold text-gray-800">{doc.exp}</span>
                    </div>
                    <div>
                      <span className="text-gray-400 text-[10px] block">Verified Cases</span>
                      <span className="font-bold text-gray-800">{doc.cases}+ Treated</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
