import React, { useState } from 'react';
import {
  Alert, AlertTitle, Box, Button, Collapse, Typography
} from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

export default function HipaaDisclaimer() {
  const [expanded, setExpanded] = useState(false);

  return (
    <Alert
      severity="warning"
      icon={<WarningAmberIcon />}
      sx={{ mb: 3, borderRadius: 2 }}
      action={
        <Button color="inherit" size="small" onClick={() => setExpanded(!expanded)}>
          {expanded ? 'Less' : 'More'}
        </Button>
      }
    >
      <AlertTitle sx={{ fontWeight: 600 }}>
        For Demonstration Purposes Only — Not for Clinical Use
      </AlertTitle>
      <Typography variant="body2">
        All recommendations must be reviewed by a licensed pharmacist or physician before dispensing.
      </Typography>
      <Collapse in={expanded}>
        <Box sx={{ mt: 1 }}>
          <Typography variant="body2" sx={{ mb: 0.5 }}>
            <strong>HIPAA Notice:</strong> Do not enter real patient names, date of birth, Social Security Numbers,
            or any Protected Health Information (PHI). Use pseudonymized member IDs only (format: MBR-XXXX).
          </Typography>
          <Typography variant="body2" sx={{ mb: 0.5 }}>
            <strong>Clinical Disclaimer:</strong> This AI system is a decision-support tool only. Drug interaction
            checks, formulary data, and equivalency assessments shown here are based on demo data and must
            not replace clinical judgment.
          </Typography>
          <Typography variant="body2">
            <strong>Data:</strong> OpenFDA public drug data is used for demonstration. Formulary coverage
            shown is illustrative only and does not reflect any real insurance plan.
          </Typography>
        </Box>
      </Collapse>
    </Alert>
  );
}
