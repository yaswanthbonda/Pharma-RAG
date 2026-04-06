import React, { useState } from 'react';
import {
  Box, Button, Card, CardContent, Chip, Divider,
  FormControl, Grid, InputLabel, MenuItem, Select,
  TextField, Typography, CircularProgress
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';

const REASONS = [
  { value: 'OUT_OF_STOCK', label: 'Out of Stock' },
  { value: 'COST_REDUCTION', label: 'Cost Reduction' },
  { value: 'FORMULARY_CHANGE', label: 'Formulary Change' },
  { value: 'PATIENT_PREFERENCE', label: 'Patient Preference' },
];

const COMMON_ALLERGIES = ['Penicillin', 'Sulfa', 'Aspirin/NSAIDs', 'Shellfish', 'Latex', 'Codeine'];

const INITIAL_STATE = {
  drugName: '',
  dosage: '',
  memberId: 'MBR-',
  planId: 'STANDARD-PPO-2024',
  drugClass: '',
  reason: 'OUT_OF_STOCK',
  allergies: [],
  currentMedications: [],
};

export default function DrugSearchForm({ onSubmit, loading }) {
  const [form, setForm] = useState(INITIAL_STATE);
  const [allergyInput, setAllergyInput] = useState('');
  const [medInput, setMedInput] = useState('');
  const [errors, setErrors] = useState({});

  const validate = () => {
    const e = {};
    if (!form.drugName.trim()) e.drugName = 'Drug name is required';
    if (!form.memberId.match(/^MBR-[A-Z0-9]{4,12}$/))
      e.memberId = 'Format: MBR-XXXX (letters/numbers only, no real names)';
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleChange = (field) => (e) => {
    setForm(prev => ({ ...prev, [field]: e.target.value }));
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: undefined }));
  };

  const addAllergy = () => {
    const val = allergyInput.trim();
    if (val && !form.allergies.includes(val)) {
      setForm(prev => ({ ...prev, allergies: [...prev.allergies, val] }));
    }
    setAllergyInput('');
  };

  const removeAllergy = (a) =>
    setForm(prev => ({ ...prev, allergies: prev.allergies.filter(x => x !== a) }));

  const addMedication = () => {
    const val = medInput.trim();
    if (val && !form.currentMedications.includes(val)) {
      setForm(prev => ({ ...prev, currentMedications: [...prev.currentMedications, val] }));
    }
    setMedInput('');
  };

  const removeMed = (m) =>
    setForm(prev => ({ ...prev, currentMedications: prev.currentMedications.filter(x => x !== m) }));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (validate()) onSubmit(form);
  };

  return (
    <Card elevation={2} sx={{ borderRadius: 3 }}>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" fontWeight={600} gutterBottom>
          Drug Substitution Request
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Enter the prescribed drug and member details to find safe alternatives.
        </Typography>

        <Box component="form" onSubmit={handleSubmit}>
          <Grid container spacing={2.5}>

            {/* Drug Name */}
            <Grid item xs={12} sm={8}>
              <TextField
                fullWidth label="Prescribed Drug Name *"
                placeholder="e.g. Lipitor, Atorvastatin, Bactrim"
                value={form.drugName}
                onChange={handleChange('drugName')}
                error={!!errors.drugName}
                helperText={errors.drugName}
                disabled={loading}
              />
            </Grid>

            {/* Dosage */}
            <Grid item xs={12} sm={4}>
              <TextField
                fullWidth label="Dosage"
                placeholder="e.g. 40mg"
                value={form.dosage}
                onChange={handleChange('dosage')}
                disabled={loading}
              />
            </Grid>

            {/* Member ID */}
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Member ID *"
                placeholder="MBR-ABC1234"
                value={form.memberId}
                onChange={handleChange('memberId')}
                error={!!errors.memberId}
                helperText={errors.memberId || 'Use pseudonymized ID only — no real names (HIPAA)'}
                disabled={loading}
              />
            </Grid>

            {/* Plan ID */}
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Insurance Plan ID"
                placeholder="STANDARD-PPO-2024"
                value={form.planId}
                onChange={handleChange('planId')}
                disabled={loading}
              />
            </Grid>

            {/* Drug Class */}
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth label="Drug Class (optional)"
                placeholder="e.g. statin, antibiotic, beta blocker"
                value={form.drugClass}
                onChange={handleChange('drugClass')}
                disabled={loading}
              />
            </Grid>

            {/* Reason */}
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Reason for Substitution</InputLabel>
                <Select
                  value={form.reason}
                  label="Reason for Substitution"
                  onChange={handleChange('reason')}
                  disabled={loading}
                >
                  {REASONS.map(r => (
                    <MenuItem key={r.value} value={r.value}>{r.label}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>

            {/* Allergies */}
            <Grid item xs={12}>
              <Divider sx={{ mb: 2 }} />
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                Documented Allergies (generic drug classes only)
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, mb: 1, flexWrap: 'wrap' }}>
                {COMMON_ALLERGIES.map(a => (
                  <Chip
                    key={a} label={a} size="small" variant="outlined"
                    clickable={!loading}
                    color={form.allergies.includes(a) ? 'error' : 'default'}
                    onClick={() => form.allergies.includes(a) ? removeAllergy(a)
                      : setForm(prev => ({ ...prev, allergies: [...prev.allergies, a] }))}
                  />
                ))}
              </Box>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                <TextField
                  size="small" label="Add custom allergy"
                  value={allergyInput}
                  onChange={e => setAllergyInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addAllergy())}
                  disabled={loading}
                  sx={{ flex: 1 }}
                />
                <Button
                  variant="outlined" size="medium" onClick={addAllergy}
                  disabled={loading || !allergyInput.trim()}
                  startIcon={<AddCircleOutlineIcon />}
                >
                  Add
                </Button>
              </Box>
              {form.allergies.length > 0 && (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                  {form.allergies.map(a => (
                    <Chip
                      key={a} label={a} size="small" color="error"
                      onDelete={() => removeAllergy(a)} disabled={loading}
                    />
                  ))}
                </Box>
              )}
            </Grid>

            {/* Current Medications */}
            <Grid item xs={12}>
              <Typography variant="subtitle2" fontWeight={600} gutterBottom>
                Current Medications (generic names only — no dosing details)
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                <TextField
                  size="small" label="Add medication"
                  placeholder="e.g. Warfarin, Metformin"
                  value={medInput}
                  onChange={e => setMedInput(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addMedication())}
                  disabled={loading}
                  sx={{ flex: 1 }}
                />
                <Button
                  variant="outlined" size="medium" onClick={addMedication}
                  disabled={loading || !medInput.trim()}
                  startIcon={<AddCircleOutlineIcon />}
                >
                  Add
                </Button>
              </Box>
              {form.currentMedications.length > 0 && (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
                  {form.currentMedications.map(m => (
                    <Chip
                      key={m} label={m} size="small" color="primary" variant="outlined"
                      onDelete={() => removeMed(m)} disabled={loading}
                    />
                  ))}
                </Box>
              )}
            </Grid>

            {/* Submit */}
            <Grid item xs={12}>
              <Divider sx={{ mb: 2 }} />
              <Button
                type="submit" variant="contained" size="large" fullWidth
                disabled={loading}
                startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <SearchIcon />}
                sx={{ py: 1.5, fontWeight: 600, borderRadius: 2 }}
              >
                {loading ? 'Analyzing with Claude AI...' : 'Find Safe Alternatives'}
              </Button>
            </Grid>

          </Grid>
        </Box>
      </CardContent>
    </Card>
  );
}
