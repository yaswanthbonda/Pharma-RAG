import React from 'react';
import { Box, Card, CardContent, Skeleton, Stack, Typography } from '@mui/material';

const PipelineStep = ({ label, active, done }) => (
  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, py: 0.75 }}>
    <Box sx={{
      width: 10, height: 10, borderRadius: '50%', flexShrink: 0,
      bgcolor: done ? 'success.main' : active ? 'primary.main' : 'grey.300',
      transition: 'background-color 0.4s',
      ...(active && {
        boxShadow: '0 0 0 4px rgba(25, 118, 210, 0.2)',
        animation: 'pulse 1.2s ease-in-out infinite',
      })
    }} />
    <Typography
      variant="body2"
      color={done ? 'success.main' : active ? 'primary.main' : 'text.disabled'}
      fontWeight={active ? 600 : 400}
    >
      {label}
    </Typography>
  </Box>
);

export default function LoadingSkeleton({ step = 0 }) {
  const steps = [
    'Embedding query vector...',
    'Searching ChromaDB for similar drugs...',
    'Building RAG prompt with retrieved context...',
    'Claude evaluating 4 safety gates...',
    'Parsing structured response...',
  ];

  return (
    <Box>
      {/* Pipeline progress */}
      <Card variant="outlined" sx={{ borderRadius: 2, mb: 2 }}>
        <CardContent sx={{ p: 2.5, '&:last-child': { pb: 2.5 } }}>
          <Typography variant="subtitle2" fontWeight={600} gutterBottom color="primary">
            RAG Pipeline Running
          </Typography>
          <Box>
            {steps.map((s, i) => (
              <PipelineStep
                key={i}
                label={s}
                active={i === step}
                done={i < step}
              />
            ))}
          </Box>
        </CardContent>
      </Card>

      {/* Result skeleton */}
      <Stack spacing={1.5}>
        <Skeleton variant="rounded" height={72} />
        <Card variant="outlined" sx={{ borderRadius: 2 }}>
          <CardContent sx={{ p: 2 }}>
            <Skeleton width="40%" height={20} sx={{ mb: 1 }} />
            <Skeleton width="70%" height={32} sx={{ mb: 1 }} />
            <Skeleton width="55%" height={18} />
            <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
              <Skeleton variant="rounded" width={80} height={28} />
              <Skeleton variant="rounded" width={60} height={28} />
            </Box>
          </CardContent>
        </Card>
        <Skeleton variant="rounded" height={180} />
      </Stack>

      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.5; }
        }
      `}</style>
    </Box>
  );
}
