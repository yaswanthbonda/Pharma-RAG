import axios from 'axios';

const BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 60000, // 60s — LLM calls can be slow
  headers: { 'Content-Type': 'application/json' },
});

/**
 * Submits a drug substitution request to the RAG pipeline.
 * @param {Object} request - SubstitutionRequest payload
 * @returns {Promise<Object>} - SubstitutionResponse
 */
export const getSubstitutionRecommendation = async (request) => {
  const response = await api.post('/api/v1/substitution/recommend', request);
  return response.data;
};

/**
 * Health check
 */
export const checkHealth = async () => {
  const response = await api.get('/api/v1/health');
  return response.data;
};

export default api;
