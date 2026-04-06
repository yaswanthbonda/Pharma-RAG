import React, { useState } from 'react';
import {
  Box, Card, CardContent, Chip, Collapse,
  Divider, IconButton, Stack, Tooltip, Typography
} from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import ExpandLessIcon from '@mui/icons-material/ExpandLess';

const PIPELINE_STEPS = [
  {
    step: '1',
    label: 'Embed query',
    detail: 'Drug name + allergies + plan converted to a 384-dim vector',
    tech: 'Claude API',
    color: '#7B1FA2',
  },
  {
    step: '2',
    label: 'Vector search',
    detail: 'Top-5 semantically similar drugs retrieved from ChromaDB',
    tech: 'ChromaDB',
    color: '#1565C0',
  },
  {
    step: '3',
    label: 'Build prompt',
    detail: 'Retrieved drug docs injected into chain-of-thought prompt',
    tech: 'RAG',
    color: '#00695C',
  },
  {
    step: '4',
    label: 'Claude reasons',
    detail: 'Evaluates 4 safety gates: equivalency → formulary → interactions → allergies',
    tech: 'Claude claude-sonnet-4-6',
    color: '#E65100',
  },
  {
    step: '5',
    label: 'Structured response',
    detail: 'JSON with recommendation, confidence score, and source citations',
    tech: 'Spring Boot',
    color: '#558B2F',
  },
];

const SAFETY_GATES = [
  { gate: 'Gate 1', label: 'Therapeutic equivalency', desc: 'Same drug class and clinical effect' },
  { gate: 'Gate 2', label: 'Formulary coverage', desc: 'Covered under member\'s insurance plan' },
  { gate: 'Gate 3', label: 'Drug interactions', desc: 'No major interactions with current medications' },
  { gate: 'Gate 4', label: 'Allergy check', desc: 'No contraindications with documented allergies' },
];

export default function RagInfoPanel() {
  const [open, setOpen] = useState(false);

  return (
    <Card variant="outlined" sx={{ borderRadius: 2, mb: 2.5, borderColor: 'primary.light' }}>
      <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
        <Box
          sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer' }}
          onClick={() => setOpen(o => !o)}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <InfoOutlinedIcon fontSize="small" color="primary" />
            <Typography variant="subtitle2" fontWeight={600} color="primary.main">
              How this works — RAG Pipeline Explained
            </Typography>
          </Box>
          <IconButton size="small">
            {open ? <ExpandLessIcon fontSize="small" /> : <ExpandMoreIcon fontSize="small" />}
          </IconButton>
        </Box>

        <Collapse in={open}>
          <Divider sx={{ my: 1.5 }} />

          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 1.5 }}>
            This app uses Retrieval-Augmented Generation (RAG) with Anthropic Claude to find
            safe drug alternatives. Here's exactly what happens when you submit a request:
          </Typography>

          {/* Pipeline steps */}
          <Stack spacing={1} sx={{ mb: 2 }}>
            {PIPELINE_STEPS.map((s) => (
              <Box key={s.step} sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5 }}>
                <Box sx={{
                  width: 24, height: 24, borderRadius: '50%', flexShrink: 0,
                  bgcolor: s.color, display: 'flex', alignItems: 'center',
                  justifyContent: 'center', mt: 0.25
                }}>
                  <Typography variant="caption" color="white" fontWeight={700}>{s.step}</Typography>
                </Box>
                <Box sx={{ flex: 1 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="body2" fontWeight={600}>{s.label}</Typography>
                    <Chip label={s.tech} size="small" sx={{ fontSize: '0.65rem', height: 18 }} />
                  </Box>
                  <Typography variant="caption" color="text.secondary">{s.detail}</Typography>
                </Box>
              </Box>
            ))}
          </Stack>

          <Divider sx={{ my: 1.5 }} />

          {/* Safety gates */}
          <Typography variant="caption" fontWeight={600} color="text.secondary"
            textTransform="uppercase" letterSpacing={0.5} display="block" sx={{ mb: 1 }}>
            4 Safety Gates (evaluated in order — one failure excludes the drug)
          </Typography>
          <Stack spacing={0.75}>
            {SAFETY_GATES.map((g) => (
              <Box key={g.gate} sx={{ display: 'flex', gap: 1, alignItems: 'flex-start' }}>
                <Chip label={g.gate} size="small" color="primary" variant="outlined"
                  sx={{ fontSize: '0.65rem', height: 18, flexShrink: 0, mt: 0.15 }} />
                <Box>
                  <Typography variant="caption" fontWeight={600}>{g.label}</Typography>
                  <Typography variant="caption" color="text.secondary" display="block">{g.desc}</Typography>
                </Box>
              </Box>
            ))}
          </Stack>

          <Divider sx={{ my: 1.5 }} />

          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.75 }}>
            {['React 18', 'MUI v5', 'Java 17', 'Spring Boot 3',
              'ChromaDB', 'OpenFDA API', 'Anthropic Claude', 'Docker Compose'].map(t => (
              <Chip key={t} label={t} size="small" variant="outlined"
                sx={{ fontSize: '0.65rem', height: 18 }} />
            ))}
          </Box>
        </Collapse>
      </CardContent>
    </Card>
  );
}
