import React, { useState, useEffect } from 'react';
import {
  Alert, AppBar, Box, Container, Snackbar,
  ThemeProvider, Toolbar, Typography, createTheme
} from '@mui/material';
import LocalPharmacyIcon from '@mui/icons-material/LocalPharmacy';
import HipaaDisclaimer from './components/HipaaDisclaimer';
import DrugSearchForm from './components/DrugSearchForm';
import RecommendationCard from './components/RecommendationCard';
import LoadingSkeleton from './components/LoadingSkeleton';
import HistoryDrawer from './components/HistoryDrawer';
import RagInfoPanel from './components/RagInfoPanel';
import { getSubstitutionRecommendation } from './services/api';

const theme = createTheme({
  palette: {
    primary: { main: '#1565C0' },
    secondary: { main: '#00897B' },
    background: { default: '#F4F6F9' },
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
  },
  shape: { borderRadius: 8 },
  components: {
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: { root: { border: '1px solid #E0E0E0' } },
    },
    MuiButton: {
      styleOverrides: { root: { textTransform: 'none', fontWeight: 600 } },
    },
  },
});

export default function App() {
  const [loading, setLoading] = useState(false);
  const [loadingStep, setLoadingStep] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [history, setHistory] = useState([]);
  const [currentRequest, setCurrentRequest] = useState(null);

  // Simulate pipeline step progression for the loading UI
  useEffect(() => {
    if (!loading) { setLoadingStep(0); return; }
    const steps = [0, 1, 2, 3, 4];
    const delays = [0, 800, 1800, 3000, 5000];
    const timers = steps.map((step, i) =>
      setTimeout(() => setLoadingStep(step), delays[i])
    );
    return () => timers.forEach(clearTimeout);
  }, [loading]);

  const handleSubmit = async (formData) => {
    setLoading(true);
    setResult(null);
    setError(null);
    setCurrentRequest(formData);

    try {
      const response = await getSubstitutionRecommendation(formData);
      setResult(response);
      // Save to in-memory session history (never persisted — HIPAA)
      setHistory(prev => [{
        request: formData,
        result: response,
        timestamp: new Date().toISOString()
      }, ...prev].slice(0, 10)); // Keep last 10
    } catch (err) {
      const msg = err.response?.data?.message
        || err.message
        || 'An error occurred. Please try again.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleHistorySelect = (item) => {
    setResult(item.result);
    setCurrentRequest(item.request);
  };

  return (
    <ThemeProvider theme={theme}>
      <Box sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>

        {/* App bar */}
        <AppBar position="static" elevation={0} sx={{ borderBottom: '1px solid #1976D2' }}>
          <Toolbar>
            <LocalPharmacyIcon sx={{ mr: 1.5 }} />
            <Typography variant="h6" fontWeight={700} sx={{ flexGrow: 1 }}>
              PharmaSub
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.8 }}>
              AI Drug Substitution Advisor · Demo Only
            </Typography>
          </Toolbar>
        </AppBar>

        <Container maxWidth="lg" sx={{ py: 4 }}>

          {/* HIPAA disclaimer — always visible */}
          <HipaaDisclaimer />

          {/* Main content */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { md: '1fr 1fr' }, gap: 3 }}>

            {/* Left: form */}
            <Box>
              <RagInfoPanel />
              <DrugSearchForm onSubmit={handleSubmit} loading={loading} />
            </Box>

            {/* Right: results */}
            <Box>
              {!result && !loading && (
                <Box sx={{
                  height: '100%', minHeight: 300,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  border: '2px dashed #E0E0E0', borderRadius: 3,
                  color: 'text.disabled', p: 4, textAlign: 'center'
                }}>
                  <Box>
                    <LocalPharmacyIcon sx={{ fontSize: 48, mb: 1, opacity: 0.3 }} />
                    <Typography variant="body1">
                      Fill in the drug details and click<br />"Find Safe Alternatives"
                    </Typography>
                    <Typography variant="caption" sx={{ mt: 1, display: 'block' }}>
                      The RAG pipeline will search OpenFDA drug data<br />
                      and use Claude AI to evaluate safety across 4 gates
                    </Typography>
                  </Box>
                </Box>
              )}

              {loading && <LoadingSkeleton step={loadingStep} />}

              {result && <RecommendationCard result={result} />}
            </Box>
          </Box>

          {/* Footer */}
          <Box sx={{ mt: 4, textAlign: 'center', opacity: 0.5 }}>
            <Typography variant="caption">
              PharmaSub Demo · Built with React 18, Java 17 + Spring Boot 3, ChromaDB, Anthropic Claude ·
              OpenFDA data (public domain) · Not for clinical use
            </Typography>
          </Box>
        </Container>

        {/* Error snackbar */}
        <Snackbar
          open={!!error}
          autoHideDuration={8000}
          onClose={() => setError(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
        >
          <Alert severity="error" onClose={() => setError(null)} sx={{ width: '100%' }}>
            {error}
          </Alert>
        </Snackbar>

        {/* Session history drawer (in-memory only, HIPAA compliant) */}
        <HistoryDrawer history={history} onSelect={handleHistorySelect} />

      </Box>
    </ThemeProvider>
  );
}
