import React, { useState } from 'react';
import {
  Box, Chip, Divider, Drawer, IconButton, List,
  ListItem, ListItemButton, ListItemText, Tooltip, Typography
} from '@mui/material';
import HistoryIcon from '@mui/icons-material/History';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import CloseIcon from '@mui/icons-material/Close';

const STATUS_ICON = {
  FOUND: <CheckCircleIcon fontSize="small" color="success" />,
  NO_SAFE_SUBSTITUTE: <ErrorIcon fontSize="small" color="error" />,
  NO_COVERED_SUBSTITUTE: <WarningIcon fontSize="small" color="warning" />,
  UNIQUE_DRUG_NO_EQUIVALENT: <ErrorIcon fontSize="small" color="info" />,
};

const STATUS_LABEL = {
  FOUND: 'Found',
  NO_SAFE_SUBSTITUTE: 'No Safe Sub',
  NO_COVERED_SUBSTITUTE: 'Not Covered',
  UNIQUE_DRUG_NO_EQUIVALENT: 'Unique Drug',
};

/**
 * Side drawer showing the session history of substitution requests.
 * Note: history is in-memory only (not persisted) — HIPAA compliant.
 */
export default function HistoryDrawer({ history, onSelect }) {
  const [open, setOpen] = useState(false);

  if (!history || history.length === 0) return null;

  return (
    <>
      <Tooltip title="Session history (not persisted)">
        <IconButton
          onClick={() => setOpen(true)}
          sx={{ position: 'fixed', bottom: 24, right: 24, bgcolor: 'white',
                boxShadow: 2, '&:hover': { bgcolor: 'grey.100' } }}
        >
          <HistoryIcon color="primary" />
          <Box sx={{
            position: 'absolute', top: 4, right: 4,
            bgcolor: 'primary.main', color: 'white',
            borderRadius: '50%', width: 16, height: 16,
            fontSize: 10, display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            {history.length}
          </Box>
        </IconButton>
      </Tooltip>

      <Drawer anchor="right" open={open} onClose={() => setOpen(false)}>
        <Box sx={{ width: 320, p: 2 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
            <Typography variant="subtitle1" fontWeight={600}>
              Session History
            </Typography>
            <IconButton size="small" onClick={() => setOpen(false)}>
              <CloseIcon fontSize="small" />
            </IconButton>
          </Box>
          <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
            In-memory only — cleared when you close the browser (HIPAA)
          </Typography>
          <Divider />
          <List dense disablePadding>
            {history.map((item, idx) => (
              <ListItem key={idx} disablePadding>
                <ListItemButton
                  onClick={() => { onSelect(item); setOpen(false); }}
                  sx={{ borderRadius: 1, my: 0.25 }}
                >
                  <Box sx={{ mr: 1.5, mt: 0.25 }}>
                    {STATUS_ICON[item.result?.status] || <HistoryIcon fontSize="small" color="disabled" />}
                  </Box>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" fontWeight={500} noWrap sx={{ maxWidth: 180 }}>
                          {item.request?.drugName} {item.request?.dosage || ''}
                        </Typography>
                        <Chip
                          label={STATUS_LABEL[item.result?.status] || '?'}
                          size="small"
                          sx={{ fontSize: '0.65rem', height: 18 }}
                        />
                      </Box>
                    }
                    secondary={
                      <Typography variant="caption" color="text.disabled">
                        {item.request?.memberId} · {new Date(item.timestamp).toLocaleTimeString()}
                      </Typography>
                    }
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Box>
      </Drawer>
    </>
  );
}
