import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import apiClient from '../api/apiClient';
import { useAuth } from '../context/AuthContext';

const QUICK_SYMPTOMS = [
  {
    label: '🧠 Mental Health & Insomnia',
    text: 'Severe anxiety, panic episodes at work, persistent hopelessness and acute sleep disturbance for 3 weeks.',
  },
  {
    label: '🦴 Knee Trauma / Sports Injury',
    text: 'Sudden sharp twist in left knee during football, loud popping sound with severe swelling and inability to bear weight.',
  },
  {
    label: '❤️ Chest Heaviness & Palpitations',
    text: 'Heavy pressure over mid-chest radiating into left jaw, shortness of breath on climbing single flight of stairs.',
  },
  {
    label: '🩻 Head Injury / Suspected Fracture',
    text: 'Fell from stairs, intense localized bone pain and swelling, requiring X-ray / CT scan evaluation.',
  },
  {
    label: '🩺 Seasonal Fever & Weakness',
    text: 'High grade fever with chills, body ache, loss of appetite and extreme weakness for 2 days.',
  },
];

export default function OpdTriage() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    patientName: user?.name || '',
    patientEmail: user?.email || '',
    patientAge: 29,
    gender: 'Female',
    bloodGroup: 'B+',
    contactNumber: '+91 98765 43210',
    chiefComplaint: '',
    isTeleConsult: false,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [issuedProforma, setIssuedProforma] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.chiefComplaint.trim()) {
      setError('Please describe your condition or symptoms in your own words.');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const payload = {
        ...formData,
        patientName: formData.patientName || user?.name || 'Citizen Patient',
        patientEmail: formData.patientEmail || user?.email || 'patient@nagarseva.com',
      };
      const response = await apiClient.post('/api/health/triage', payload);
      setIssuedProforma(response.data);
    } catch (err) {
      console.error('Triage error:', err);
      setError('Unable to complete AI triage. Please ensure the backend hospital service is running.');
    } finally {
      setLoading(false);
    }
  };

  const getUrgencyBadge = (urgency) => {
    switch (urgency) {
      case 'CRITICAL':
        return 'bg-rose-100 text-rose-800 border border-rose-300 font-extrabold animate-pulse';
      case 'URGENT':
        return 'bg-amber-100 text-amber-800 border border-amber-300 font-bold';
      default:
        return 'bg-emerald-100 text-emerald-800 border border-emerald-300 font-semibold';
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Header Banner */}
      <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100/90 flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-violet-50 border border-violet-200 rounded-full text-xs font-bold text-violet-800 mb-2">
            <span>🩺</span>
            <span>AI-Powered Hospital OPD Triage & Commute Saver</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
            Smart OPD Registration & Layman Triage
          </h1>
          <p className="text-xs sm:text-sm text-gray-500 mt-1 max-w-2xl">
            Describe your symptoms in simple everyday words. Our clinical AI model triages your condition, issues a digital OPD token, allocates the right specialist, and saves hospital commute time.
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <Link
            to="/health-vault"
            className="px-4 py-2 bg-gray-50 hover:bg-gray-100 border border-gray-200 text-xs font-bold text-gray-700 rounded-full shadow-xs transition inline-flex items-center gap-1.5"
          >
            <span>📁</span>
            <span>My Health Vault</span>
          </Link>
        </div>
      </div>

      {error && (
        <div className="p-4 bg-rose-50 text-rose-800 text-xs rounded-2xl border border-rose-200">
          {error}
        </div>
      )}

      {/* Triage Output Card (If Token Issued) */}
      {issuedProforma && (
        <div className="bg-gradient-to-br from-violet-50/80 to-indigo-50/60 rounded-3xl p-6 sm:p-8 shadow-md border border-violet-200 animate-in fade-in zoom-in-95 duration-200 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-violet-200/80">
            <div>
              <span className="text-[11px] font-bold uppercase tracking-wider text-violet-600">
                Digital OPD Pass & Electronic Health Record Generated
              </span>
              <h2 className="text-2xl sm:text-3xl font-black text-violet-950 mt-0.5">
                Token #{issuedProforma.tokenNumber}
              </h2>
            </div>
            <div className="flex items-center gap-2">
              <span className={`px-3.5 py-1 rounded-full text-xs ${getUrgencyBadge(issuedProforma.aiTriageUrgency)}`}>
                {issuedProforma.aiTriageUrgency} Priority
              </span>
              {issuedProforma.isTeleConsult && (
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
                  📹 Tele-Consult Active
                </span>
              )}
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Allocated Department */}
            <div className="bg-white p-4 rounded-2xl border border-violet-100 shadow-xs">
              <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1">
                Allocated Department
              </p>
              <p className="text-base font-extrabold text-violet-900">
                🏥 {issuedProforma.aiTriageDepartment?.replace('_', ' ')}
              </p>
              <p className="text-xs text-gray-600 mt-1">
                Assigned to: <span className="font-semibold text-gray-800">{issuedProforma.assignedDoctorName}</span>
              </p>
              <span className="inline-block mt-2 px-2 py-0.5 rounded-md text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                ✓ Verified Doctor ({issuedProforma.assignedDoctorRegNo})
              </span>
            </div>

            {/* Commute Reducer & Queue Status */}
            <div className="bg-white p-4 rounded-2xl border border-violet-100 shadow-xs">
              <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1">
                Commute Time Optimizer
              </p>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-2xl font-black text-gray-900">
                  ~{issuedProforma.queueEstimatedWaitMinutes} Mins
                </span>
                <span className="text-xs font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-100">
                  Live Queue Est.
                </span>
              </div>
              <p className="text-[11px] text-gray-500 mt-1.5 leading-relaxed">
                {issuedProforma.isTeleConsult
                  ? 'Doctor video consult link will activate 5 minutes before your turn.'
                  : 'Start your hospital commute when the queue indicator reaches ~10 minutes.'}
              </p>
            </div>

            {/* Digital Security Hash */}
            <div className="bg-white p-4 rounded-2xl border border-violet-100 shadow-xs">
              <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-1">
                Verifiable Proforma Seal
              </p>
              <p className="text-xs font-mono font-semibold text-gray-700 break-all bg-gray-50 p-2 rounded-lg border border-gray-100">
                SHA-256: {issuedProforma.verificationHash || 'VERIFIED-SEAL'}
              </p>
              <p className="text-[10px] text-emerald-700 font-bold mt-1.5 flex items-center gap-1">
                <span>🛡️</span>
                <span>Tamper-Evident Medical Proforma</span>
              </p>
            </div>
          </div>

          {/* AI Clinical Assessment Rationale & Red Flag Advice */}
          <div className="bg-white p-5 rounded-2xl border border-violet-100 space-y-3">
            <div>
              <p className="text-xs font-bold text-violet-900 uppercase tracking-wider mb-1">
                🤖 AI Clinical Triage Rationale
              </p>
              <p className="text-xs text-gray-700 leading-relaxed bg-violet-50/50 p-3 rounded-xl border border-violet-100/60">
                {issuedProforma.aiClinicalRationale}
              </p>
            </div>

            <div className="pt-2 border-t border-gray-100">
              <p className="text-xs font-bold text-amber-800 flex items-center gap-1.5 mb-1">
                <span>⚠️</span>
                <span>Precautionary Patient Guidance</span>
              </p>
              <p className="text-xs text-gray-600">
                Please remain seated in the department waiting lounge or keep your phone nearby. If you experience sudden chest tightness or severe breathing difficulty, inform the triage nursing officer immediately.
              </p>
            </div>
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
            <Link
              to="/health-vault"
              className="px-5 py-2.5 bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs font-bold rounded-full shadow-sm transition inline-flex items-center gap-2"
            >
              <span>📁</span>
              <span>Open Patient Health Vault & Stamped Records →</span>
            </Link>
            <button
              onClick={() => setIssuedProforma(null)}
              className="px-4 py-2 bg-white hover:bg-gray-50 border border-gray-200 text-xs font-bold text-gray-700 rounded-full transition"
            >
              ➕ Register Another Patient
            </button>
          </div>
        </div>
      )}

      {/* Symptom Input Form */}
      {!issuedProforma && (
        <div className="bg-white rounded-3xl p-6 sm:p-8 shadow-sm border border-gray-100/90 space-y-6">
          {/* Quick Pre-Set Symptom Chips */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-gray-500 mb-2">
              ⚡ Quick Select Sample Conditions (Real Patient Narratives)
            </label>
            <div className="flex flex-wrap gap-2">
              {QUICK_SYMPTOMS.map((q, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setFormData(prev => ({ ...prev, chiefComplaint: q.text }))}
                  className="px-3 py-1.5 rounded-full text-xs font-semibold bg-gray-50 hover:bg-violet-50 hover:text-violet-700 border border-gray-200/80 transition text-gray-700 text-left"
                >
                  {q.label}
                </button>
              ))}
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="complaint" className="block text-xs font-bold uppercase tracking-wider text-gray-700 mb-1 flex items-center justify-between">
                <span>💬 Describe Your Symptoms In Your Own Words</span>
                <span className="text-[11px] text-[#7c5cff] font-semibold">Trained on real patient speech</span>
              </label>
              <textarea
                id="complaint"
                required
                rows={4}
                value={formData.chiefComplaint}
                onChange={(e) => setFormData(prev => ({ ...prev, chiefComplaint: e.target.value }))}
                placeholder="e.g. I have been feeling intense anxiety, mood swings, sleeplessness and loss of focus for 3 weeks... or My knee gave a popping sound during sports with severe swelling..."
                className="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-2xl text-xs sm:text-sm text-gray-900 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-600 mb-1">
                  Patient Full Name
                </label>
                <input
                  type="text"
                  required
                  value={formData.patientName}
                  onChange={(e) => setFormData(prev => ({ ...prev, patientName: e.target.value }))}
                  placeholder="e.g. Sneha Kapoor"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-600 mb-1">
                  Age & Gender
                </label>
                <div className="grid grid-cols-2 gap-1.5">
                  <input
                    type="number"
                    min="1"
                    max="110"
                    value={formData.patientAge}
                    onChange={(e) => setFormData(prev => ({ ...prev, patientAge: e.target.value }))}
                    className="w-full px-2.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                  />
                  <select
                    value={formData.gender}
                    onChange={(e) => setFormData(prev => ({ ...prev, gender: e.target.value }))}
                    className="w-full px-2 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                  >
                    <option value="Female">Female</option>
                    <option value="Male">Male</option>
                    <option value="Other">Other</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-600 mb-1">
                  Blood Group
                </label>
                <select
                  value={formData.bloodGroup}
                  onChange={(e) => setFormData(prev => ({ ...prev, bloodGroup: e.target.value }))}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-xs font-semibold text-gray-800 focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#7c5cff]"
                >
                  <option value="A+">A+</option>
                  <option value="A-">A-</option>
                  <option value="B+">B+</option>
                  <option value="B-">B-</option>
                  <option value="O+">O+</option>
                  <option value="O-">O-</option>
                  <option value="AB+">AB+</option>
                  <option value="AB-">AB-</option>
                </select>
              </div>
            </div>

            {/* Commute Mode Selector */}
            <div className="p-4 rounded-2xl border border-gray-200 bg-gray-50 flex items-center justify-between">
              <div>
                <p className="text-xs font-bold text-gray-900">Hospital Commute Reducer: Consultation Mode</p>
                <p className="text-[11px] text-gray-500">
                  Choose in-person hospital OPD arrival or switch directly to secure tele-consultation.
                </p>
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.isTeleConsult}
                  onChange={(e) => setFormData(prev => ({ ...prev, isTeleConsult: e.target.checked }))}
                  className="w-4 h-4 text-[#7c5cff] rounded focus:ring-[#7c5cff]"
                />
                <span className="text-xs font-bold text-violet-900">📹 Enable Tele-Consult</span>
              </label>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 px-6 rounded-full bg-[#7c5cff] hover:bg-[#6949f5] text-white text-xs sm:text-sm font-bold shadow-md transition disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
              >
                {loading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Clinical AI Triaging Symptoms & Generating Pass...</span>
                  </>
                ) : (
                  <span>Submit Symptoms for AI Department Routing & Digital OPD Token →</span>
                )}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
