import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

export default function PatientVault() {
  const { user } = useAuth();
  const [proformas, setProformas] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [labTests, setLabTests] = useState([]);
  const [consents, setConsents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('PRESCRIPTIONS'); // 'PRESCRIPTIONS' | 'PROFORMAS' | 'LABS' | 'CONSENTS'

  // Digital NOC Signing Modal
  const [signingConsent, setSigningConsent] = useState(null);
  const [signatureText, setSignatureText] = useState('');

  useEffect(() => {
    fetchVaultData();
  }, []);

  const fetchVaultData = async () => {
    setLoading(true);
    setError('');
    try {
      const [pRes, rxRes, labRes, cRes] = await Promise.allSettled([
        apiClient.get('/api/health/proformas/patient'),
        apiClient.get('/api/health/prescriptions'),
        apiClient.get('/api/health/lab-tests'),
        apiClient.get('/api/health/consents'),
      ]);

      if (pRes.status === 'fulfilled') setProformas(Array.isArray(pRes.value.data) ? pRes.value.data : []);
      if (rxRes.status === 'fulfilled') setPrescriptions(Array.isArray(rxRes.value.data) ? rxRes.value.data : []);
      if (labRes.status === 'fulfilled') setLabTests(Array.isArray(labRes.value.data) ? labRes.value.data : []);
      if (cRes.status === 'fulfilled') setConsents(Array.isArray(cRes.value.data) ? cRes.value.data : []);
    } catch (err) {
      console.error('Error loading patient vault data:', err);
      setError('Unable to load health vault records.');
    } finally {
      setLoading(false);
    }
  };

  const handleSignConsent = async (e) => {
    e.preventDefault();
    if (!signingConsent || !signatureText.trim()) return;

    try {
      await apiClient.patch(`/api/health/consents/${signingConsent.id}/sign`, {
        patientSignatureText: signatureText.trim(),
      });
      alert('✓ Digital NOC / Informed Consent E-Signed and Legally Witnessed!');
      setSigningConsent(null);
      setSignatureText('');
      fetchVaultData();
    } catch (err) {
      console.error('Consent signing error:', err);
      alert('Failed to sign consent agreement.');
    }
  };

  const parseMedications = (jsonStr) => {
    try {
      return JSON.parse(jsonStr || '[]');
    } catch {
      return [];
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100/90 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-violet-50 border border-violet-200 rounded-full text-xs font-bold text-violet-800 mb-2">
            <span>📁</span>
            <span>Verifiable Electronic Health Records & Stamped Prescriptions</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Patient Digital Health Vault
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-1">
            Centrally maintained, tamper-evident clinical files, stamped prescriptions, diagnostic lab reports, and legal consent agreements.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/opd-triage"
            className="px-4 py-2 bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs font-bold rounded-full shadow-sm transition inline-flex items-center gap-1.5"
          >
            <span>➕</span>
            <span>New OPD Triage</span>
          </Link>
          <button
            onClick={fetchVaultData}
            className="px-3.5 py-2 bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 text-xs font-bold rounded-full transition shadow-xs"
          >
            🔄 Refresh
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 bg-white p-1.5 rounded-2xl shadow-sm border border-gray-100 overflow-x-auto">
        {[
          { id: 'PRESCRIPTIONS', label: `💊 Stamped Prescriptions (${prescriptions.length})` },
          { id: 'PROFORMAS', label: `📋 Clinical Proformas (${proformas.length})` },
          { id: 'LABS', label: `🔬 Lab Diagnostic Reports (${labTests.length})` },
          { id: 'CONSENTS', label: `✍️ Digital NOC & Consents (${consents.length})` },
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
          Accessing your encrypted health vault...
        </div>
      )}

      {/* Content Feed */}
      {!loading && (
        <div className="space-y-4">
          {/* Prescriptions Tab */}
          {activeTab === 'PRESCRIPTIONS' && (
            <div className="space-y-4">
              {prescriptions.map(rx => (
                <div key={rx.id} className="bg-white rounded-3xl p-6 sm:p-7 shadow-sm border border-gray-100 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-gray-100">
                    <div>
                      <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                        Official Stamped Prescription #{rx.id}
                      </span>
                      <h3 className="text-lg font-extrabold text-gray-900 mt-0.5">
                        {rx.diagnosisSummary || 'Clinical Consultation'}
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Attending Doctor: <span className="font-bold text-gray-800">{rx.doctorName}</span> • Reg: <span className="font-mono">{rx.doctorRegNo}</span>
                      </p>
                    </div>

                    {rx.isStamped ? (
                      <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center gap-2">
                        <span className="text-2xl">🏛️</span>
                        <div className="text-[11px]">
                          <span className="font-extrabold text-emerald-900 block">MCI OFFICIAL STAMP APPROVED</span>
                          <span className="font-mono text-emerald-700">{rx.digitalStampSeal}</span>
                        </div>
                      </div>
                    ) : (
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200">
                        Draft In Progress
                      </span>
                    )}
                  </div>

                  {/* Medications Table */}
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-gray-600 mb-2">
                      Prescribed Medicines & Instructions
                    </h4>
                    <div className="space-y-2">
                      {parseMedications(rx.medicationsJson).map((med, idx) => (
                        <div key={idx} className="p-3 bg-gray-50 rounded-xl border border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-2 text-xs">
                          <div>
                            <span className="font-extrabold text-gray-900">{med.name}</span>
                            <span className="text-gray-500 text-[11px] ml-2">({med.dosage})</span>
                          </div>
                          <div className="flex items-center gap-3 text-[11px]">
                            <span className="font-semibold text-violet-900 bg-violet-50 px-2 py-0.5 rounded-md">
                              ⏰ {med.frequency}
                            </span>
                            <span className="font-semibold text-gray-700">
                              📅 {med.duration}
                            </span>
                            <span className="text-gray-500 italic">
                              {med.instructions}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  {rx.lifestyleAdvice && (
                    <div className="p-3 bg-violet-50/50 rounded-xl border border-violet-100 text-xs">
                      <span className="font-bold text-violet-900">Doctor's Lifestyle & Follow-Up Advice: </span>
                      <span className="text-gray-700">{rx.lifestyleAdvice}</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Proformas Tab */}
          {activeTab === 'PROFORMAS' && (
            <div className="space-y-4">
              {proformas.map(p => (
                <div key={p.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-gray-100">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-base font-extrabold text-gray-900">
                          {p.patientName}
                        </span>
                        <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-violet-100 text-violet-900">
                          {p.tokenNumber}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Department: <span className="font-semibold text-gray-700">{p.aiTriageDepartment}</span> • Doctor: <span className="font-semibold text-gray-700">{p.assignedDoctorName}</span>
                      </p>
                    </div>

                    <div className="text-xs font-mono font-semibold text-gray-500 bg-gray-50 px-3 py-1.5 rounded-xl border border-gray-100">
                      SHA-256: {p.verificationHash || 'VERIFIED-HASH'}
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
                    <div>
                      <span className="font-bold text-gray-700 uppercase tracking-wider text-[11px] block mb-1">
                        Reported Symptoms:
                      </span>
                      <p className="p-3 bg-gray-50 rounded-xl border border-gray-100 text-gray-800 leading-relaxed">
                        {p.chiefComplaint}
                      </p>
                    </div>
                    <div>
                      <span className="font-bold text-violet-900 uppercase tracking-wider text-[11px] block mb-1">
                        AI Clinical Assessment:
                      </span>
                      <p className="p-3 bg-violet-50/50 rounded-xl border border-violet-100 text-gray-700 leading-relaxed">
                        {p.aiClinicalRationale}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Lab Reports Tab */}
          {activeTab === 'LABS' && (
            <div className="space-y-4">
              {labTests.map(lab => (
                <div key={lab.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-3 border-b border-gray-100">
                    <div>
                      <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                        {lab.testCategory} Diagnostic Investigation
                      </span>
                      <h3 className="text-lg font-extrabold text-gray-900">
                        {lab.testName}
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Ordered by: <span className="font-semibold text-gray-700">{lab.orderingDoctorName}</span> ({lab.orderingDoctorRegNo})
                      </p>
                    </div>

                    {lab.isLabApproved ? (
                      <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-2xl flex items-center gap-2">
                        <span className="text-2xl">🔬</span>
                        <div className="text-[11px]">
                          <span className="font-extrabold text-emerald-900 block">PATHOLOGIST STAMP VERIFIED</span>
                          <span className="font-mono text-emerald-700">{lab.labApprovalStampSeal}</span>
                        </div>
                      </div>
                    ) : (
                      <span className="px-3 py-1 rounded-full text-xs font-bold bg-amber-50 text-amber-800 border border-amber-200">
                        Sample In Laboratory Analysis
                      </span>
                    )}
                  </div>

                  {lab.findingsReport && (
                    <div className="p-4 bg-gray-50 rounded-2xl border border-gray-100 space-y-2 text-xs">
                      <span className="font-bold text-gray-800 block">Diagnostic Findings & Interpretation:</span>
                      <p className="text-gray-700 leading-relaxed">{lab.findingsReport}</p>
                      <div className="pt-2 border-t border-gray-200/60 flex items-center justify-between text-[11px]">
                        <span>Observed: <strong className="text-gray-900">{lab.observedValue}</strong> (Normal: {lab.normalRange})</span>
                        <span className={`px-2 py-0.5 rounded-full font-bold ${
                          lab.interpretation === 'NORMAL' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'
                        }`}>
                          {lab.interpretation}
                        </span>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}

          {/* Consents & NOC Tab */}
          {activeTab === 'CONSENTS' && (
            <div className="space-y-4">
              {consents.map(c => (
                <div key={c.id} className="bg-white rounded-3xl p-6 shadow-sm border border-gray-100 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-3 border-b border-gray-100">
                    <div>
                      <span className="text-[11px] font-bold text-violet-700 uppercase tracking-wider">
                        {c.consentType}
                      </span>
                      <h3 className="text-base font-extrabold text-gray-900 mt-0.5">
                        {c.title}
                      </h3>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Witnessing Doctor: <span className="font-semibold text-gray-800">{c.doctorName}</span> ({c.doctorRegNo})
                      </p>
                    </div>

                    {c.isPatientSigned ? (
                      <div className="p-2.5 bg-emerald-50 border border-emerald-200 rounded-2xl text-[11px] space-y-0.5">
                        <span className="font-extrabold text-emerald-900 block">✓ E-SIGNED & LEGALLY WITNESSED</span>
                        <span className="text-emerald-700 block italic">Signed by: "{c.patientSignatureText}"</span>
                        <span className="font-mono text-[10px] text-gray-500 block">{c.doctorWitnessStampSeal}</span>
                      </div>
                    ) : (
                      <button
                        onClick={() => { setSigningConsent(c); setSignatureText(c.patientName || ''); }}
                        className="px-4 py-2 bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs font-bold rounded-full shadow-sm transition"
                      >
                        ✍️ E-Sign NOC Consent
                      </button>
                    )}
                  </div>

                  <div className="p-3 bg-gray-50 rounded-xl border border-gray-100 text-xs text-gray-700 leading-relaxed">
                    {c.termsAndConditions}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Digital Consent E-Sign Modal */}
      {signingConsent && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl shadow-2xl border border-gray-100 w-full max-w-lg p-6 sm:p-8 animate-in fade-in zoom-in-95 duration-150 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-gray-100">
              <div>
                <h3 className="text-lg font-extrabold text-gray-900 tracking-tight">
                  E-Sign Informed Consent & NOC
                </h3>
                <p className="text-xs text-gray-400 mt-0.5">
                  {signingConsent.title}
                </p>
              </div>
              <button
                onClick={() => setSigningConsent(null)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-600 flex items-center justify-center text-xs font-bold"
              >
                ✕
              </button>
            </div>

            <div className="p-3 bg-gray-50 rounded-2xl border border-gray-100 text-xs text-gray-700 max-h-40 overflow-y-auto leading-relaxed">
              {signingConsent.termsAndConditions}
            </div>

            <form onSubmit={handleSignConsent} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 mb-1">
                  Type Full Legal Name as Digital Signature
                </label>
                <input
                  type="text"
                  required
                  value={signatureText}
                  onChange={(e) => setSignatureText(e.target.value)}
                  placeholder="e.g. Sneha Kapoor"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setSigningConsent(null)}
                  className="px-4 py-2 rounded-full text-xs font-semibold text-gray-600 hover:bg-gray-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-full bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs font-bold shadow-sm transition"
                >
                  Confirm Legal E-Signature →
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
