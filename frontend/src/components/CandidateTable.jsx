import React from 'react';
import {
  Box, Chip, Paper, Table, TableBody, TableCell,
  TableContainer, TableHead, TableRow, Typography, Tooltip
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';

const GateCell = ({ passed, label }) => {
  if (passed === null || passed === undefined) {
    return (
      <Tooltip title="Not evaluated">
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, color: 'text.disabled' }}>
          <HelpOutlineIcon fontSize="small" />
          <Typography variant="caption">N/A</Typography>
        </Box>
      </Tooltip>
    );
  }
  return (
    <Tooltip title={passed ? `${label}: Passed` : `${label}: Failed`}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        {passed
          ? <CheckCircleIcon fontSize="small" color="success" />
          : <CancelIcon fontSize="small" color="error" />}
        <Typography variant="caption" color={passed ? 'success.main' : 'error.main'}>
          {passed ? 'Pass' : 'Fail'}
        </Typography>
      </Box>
    </Tooltip>
  );
};

export default function CandidateTable({ candidates }) {
  if (!candidates || candidates.length === 0) return null;

  return (
    <Box>
      <Typography variant="subtitle1" fontWeight={600} gutterBottom>
        All Evaluated Candidates
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 1.5 }}>
        Every drug was checked against 4 safety gates in sequence.
        A single failure excludes that candidate.
      </Typography>
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
        <Table size="small">
          <TableHead>
            <TableRow sx={{ bgcolor: 'grey.50' }}>
              <TableCell sx={{ fontWeight: 600 }}>Drug</TableCell>
              <TableCell align="center" sx={{ fontWeight: 600 }}>
                <Tooltip title="Gate 1: Same drug class and clinical effect">
                  <span>Gate 1: Equivalency</span>
                </Tooltip>
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 600 }}>
                <Tooltip title="Gate 2: Covered under member's insurance plan">
                  <span>Gate 2: Formulary</span>
                </Tooltip>
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 600 }}>
                <Tooltip title="Gate 3: No major drug interactions">
                  <span>Gate 3: Interactions</span>
                </Tooltip>
              </TableCell>
              <TableCell align="center" sx={{ fontWeight: 600 }}>
                <Tooltip title="Gate 4: No allergy contraindications">
                  <span>Gate 4: Allergies</span>
                </Tooltip>
              </TableCell>
              <TableCell sx={{ fontWeight: 600 }}>Outcome</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {candidates.map((c, idx) => {
              const allPassed = c.equivalencyPassed && c.formularyPassed
                && c.interactionPassed && c.allergyPassed;
              return (
                <TableRow
                  key={idx}
                  sx={{
                    bgcolor: allPassed ? 'success.50' : 'transparent',
                    '&:last-child td': { border: 0 }
                  }}
                >
                  <TableCell>
                    <Typography variant="body2" fontWeight={allPassed ? 600 : 400}>
                      {c.drugName}
                    </Typography>
                  </TableCell>
                  <TableCell align="center">
                    <GateCell passed={c.equivalencyPassed} label="Equivalency" />
                  </TableCell>
                  <TableCell align="center">
                    <GateCell passed={c.formularyPassed} label="Formulary" />
                  </TableCell>
                  <TableCell align="center">
                    <GateCell passed={c.interactionPassed} label="Interactions" />
                  </TableCell>
                  <TableCell align="center">
                    <GateCell passed={c.allergyPassed} label="Allergies" />
                  </TableCell>
                  <TableCell>
                    {allPassed
                      ? <Chip label="Recommended" color="success" size="small" />
                      : <Chip
                          label={c.exclusionReason || 'Excluded'}
                          color="default" size="small" variant="outlined"
                          sx={{ maxWidth: 160, fontSize: '0.7rem' }}
                        />
                    }
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
