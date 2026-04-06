import React from 'react';
import {
  Alert, AlertTitle, Box, Card, CardContent, Chip,
  Divider, Grid, LinearProgress, Stack, Tooltip, Typography
} from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import StorageIcon from '@mui/icons-material/Storage';
import CandidateTable from './CandidateTable';

const STATUS_CONFIG = {
  FOUND: {
    severity: 'success',
    icon: <CheckCircleOutlineIcon />,
    label: 'Safe Substitute Found',
  },
  NO_SAFE_SUBSTITUTE: {
    severity: 'error',
    icon: <ErrorOutlineIcon />,
    label: 'No Safe Substitute Available',
  },
  NO_COVERED_SUBSTITUTE: {
    severity: 'warning',
    icon: <WarningAmberIcon />,
    label: 'No Covered Substitute (Safe Options Exist)',
  },
  UNIQUE_DRUG_NO_EQUIVALENT: {
    severity: 'info',
    icon: <InfoOutlinedIcon />,
    label: 'Unique Drug — No Therapeutic Equivalent',
  },
};

const TierBadge = ({ tier }) => {
  const colors = { 1: 'success', 2: 'primary', 3: 'warning', 4: 'error' };
  return (
    <Chip
      label={`Tier ${tier}`}
      color={colors[tier] || 'default'}
      size="small"
      sx={{ fontWeight: 600 }}
    />
  );
};

const DrugCard = ({ drug, title, isConditional }) => {
  if (!drug) return null;
  return (
    <Card
      variant="outlined"
      sx={{
        borderRadius: 2,
        borderColor: isConditional ? 'warning.main' : 'success.main',
        borderWidth: 2
      }}
    >
      <CardContent sx={{ p: 2.5 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1.5 }}>
          <Box>
            <Typography variant="caption" color="text.secondary" textTransform="uppercase" letterSpacing={0.5}>
              {title}
            </Typography>
            <Typography variant="h6" fontWeight={700}>
              {drug.drugName || drug.genericName}
            </Typography>
            {drug.brandName && drug.brandName !== drug.drugName && (
              <Typography variant="body2" color="text.secondary">
                Brand: {drug.brandName}
              </Typography>
            )}
          </Box>
          <Stack direction="row" spacing={1} alignItems="center">
            {drug.formularyTier && <TierBadge tier={drug.formularyTier} />}
            {drug.memberCopay && (
              <Chip label={drug.memberCopay + '/mo'} size="small" variant="outlined" />
            )}
          </Stack>
        </Box>

        <Divider sx={{ my: 1.5 }} />

        <Grid container spacing={1.5}>
          {drug.dosage && (
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" color="text.secondary">Dosage</Typography>
              <Typography variant="body2" fontWeight={500}>{drug.dosage}</Typography>
            </Grid>
          )}
          {drug.drugClass && (
            <Grid item xs={12} sm={6}>
              <Typography variant="caption" color="text.secondary">Drug Class</Typography>
              <Typography variant="body2" fontWeight={500}>{drug.drugClass}</Typography>
            </Grid>
          )}
          {drug.equivalencyNote && (
            <Grid item xs={12}>
              <Typography variant="caption" color="text.secondary">Equivalency Note</Typography>
              <Typography variant="body2">{drug.equivalencyNote}</Typography>
            </Grid>
          )}
          <Grid item xs={6}>
            <Typography variant="caption" color="text.secondary">Allergy Check</Typography>
            <Chip
              label={drug.allergyCheckResult || 'N/A'}
              size="small"
              color={drug.allergyCheckResult === 'PASSED' ? 'success' : drug.allergyCheckResult === 'FAILED' ? 'error' : 'default'}
              sx={{ display: 'flex', mt: 0.25, width: 'fit-content' }}
            />
          </Grid>
          <Grid item xs={6}>
            <Typography variant="caption" color="text.secondary">Interaction Check</Typography>
            <Chip
              label={drug.interactionCheckResult || 'N/A'}
              size="small"
              color={drug.interactionCheckResult === 'PASSED' ? 'success' : drug.interactionCheckResult === 'FAILED' ? 'error' : 'default'}
              sx={{ display: 'flex', mt: 0.25, width: 'fit-content' }}
            />
          </Grid>
          {drug.requiresPrescriberApproval && (
            <Grid item xs={12}>
              <Alert severity="warning" sx={{ py: 0.5, mt: 0.5 }}>
                <Typography variant="body2">
                  <strong>Prescriber Approval Required:</strong> {drug.prescriberApprovalReason}
                </Typography>
              </Alert>
            </Grid>
          )}
        </Grid>
      </CardContent>
    </Card>
  );
};

const ConfidenceBar = ({ confidence }) => {
  const pct = Math.round((confidence || 0) * 100);
  const color = pct >= 80 ? 'success' : pct >= 60 ? 'warning' : 'error';
  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
        <Typography variant="caption" color="text.secondary">AI Confidence</Typography>
        <Typography variant="caption" fontWeight={600}>{pct}%</Typography>
      </Box>
      <LinearProgress
        variant="determinate"
        value={pct}
        color={color}
        sx={{ height: 8, borderRadius: 4 }}
      />
    </Box>
  );
};

export default function RecommendationCard({ result }) {
  if (!result) return null;

  const config = STATUS_CONFIG[result.status] || STATUS_CONFIG.NO_SAFE_SUBSTITUTE;
  const { ragMetadata } = result;

  return (
    <Box sx={{ mt: 3 }}>
      {/* Status banner */}
      <Alert severity={config.severity} icon={config.icon} sx={{ mb: 2.5, borderRadius: 2 }}>
        <AlertTitle sx={{ fontWeight: 700 }}>{config.label}</AlertTitle>
        <Typography variant="body2">{result.reason}</Typography>
      </Alert>

      {/* Escalation notice */}
      {result.escalationQueue && (
        <Alert severity="warning" sx={{ mb: 2, borderRadius: 2 }}>
          <AlertTitle>Escalation Required</AlertTitle>
          <Typography variant="body2">
            Routed to: <strong>{result.escalationQueue}</strong>
            {result.prescriberContactRequired && ' — Prescriber contact required.'}
          </Typography>
        </Alert>
      )}

      {/* Recommendations */}
      <Stack spacing={2} sx={{ mb: 3 }}>
        {result.primaryRecommendation && (
          <DrugCard
            drug={result.primaryRecommendation}
            title="Primary Recommendation"
            isConditional={false}
          />
        )}
        {result.conditionalOption && (
          <DrugCard
            drug={result.conditionalOption}
            title="Conditional Option"
            isConditional={true}
          />
        )}
      </Stack>

      {/* Confidence */}
      <Box sx={{ mb: 3 }}>
        <ConfidenceBar confidence={result.confidence} />
      </Box>

      {/* Candidate evaluation table */}
      {result.evaluatedCandidates && result.evaluatedCandidates.length > 0 && (
        <Box sx={{ mb: 3 }}>
          <CandidateTable candidates={result.evaluatedCandidates} />
        </Box>
      )}

      {/* RAG metadata */}
      {ragMetadata && (
        <Card variant="outlined" sx={{ borderRadius: 2, bgcolor: 'grey.50' }}>
          <CardContent sx={{ p: 2, '&:last-child': { pb: 2 } }}>
            <Typography variant="caption" color="text.secondary" fontWeight={600}
              textTransform="uppercase" letterSpacing={0.5} display="block" gutterBottom>
              RAG Pipeline Metadata
            </Typography>
            <Grid container spacing={1.5}>
              <Grid item xs={6} sm={3}>
                <Tooltip title="Documents retrieved from ChromaDB vector store">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <StorageIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Docs Retrieved
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {ragMetadata.documentsRetrieved}
                      </Typography>
                    </Box>
                  </Box>
                </Tooltip>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Top Similarity
                  </Typography>
                  <Typography variant="body2" fontWeight={600}>
                    {((ragMetadata.topSimilarityScore || 0) * 100).toFixed(1)}%
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Tooltip title="Time to retrieve from vector store">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <AccessTimeIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Retrieval
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {ragMetadata.retrievalTimeMs}ms
                      </Typography>
                    </Box>
                  </Box>
                </Tooltip>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Tooltip title="Time for Claude to generate the recommendation">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <AccessTimeIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        LLM (Claude)
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {ragMetadata.llmTimeMs}ms
                      </Typography>
                    </Box>
                  </Box>
                </Tooltip>
              </Grid>
            </Grid>
            {result.sourceDocuments && result.sourceDocuments.length > 0 && (
              <Box sx={{ mt: 1.5 }}>
                <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                  Source Documents
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {result.sourceDocuments.map((doc, i) => (
                    <Chip key={i} label={doc} size="small" variant="outlined" sx={{ fontSize: '0.65rem' }} />
                  ))}
                </Box>
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      {/* Timestamp */}
      {result.generatedAt && (
        <Typography variant="caption" color="text.disabled" display="block" sx={{ mt: 1, textAlign: 'right' }}>
          Generated: {new Date(result.generatedAt).toLocaleString()}
        </Typography>
      )}
    </Box>
  );
}
