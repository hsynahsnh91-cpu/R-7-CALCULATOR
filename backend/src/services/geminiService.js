import { config } from '../config/env.js';
import { logger } from '../utils/logger.js';

export async function callGeminiApi(modelName, payload, timeoutMs = 25000) {
  if (!config.geminiApiKey) {
    const err = new Error('Server Gemini API key is missing or unconfigured.');
    err.statusCode = 503;
    err.errorCode = 'GEMINI_UNCONFIGURED';
    throw err;
  }

  const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${config.geminiApiKey}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const statusCode = response.status;
      const errorMsg = data?.error?.message || `HTTP ${statusCode}`;
      const err = new Error(errorMsg);
      err.statusCode = statusCode;
      err.geminiData = data;

      if (statusCode === 429) {
        err.errorCode = 'AI_RATE_LIMITED';
        // Extract retry delay if available in details
        const retryInfo = data?.error?.details?.find(d => d['@type']?.includes('RetryInfo'));
        if (retryInfo?.retryDelay) {
          err.retryAfterMs = parseDelay(retryInfo.retryDelay);
        }
      } else if (statusCode === 503 || statusCode === 502 || statusCode === 504) {
        err.errorCode = 'AI_SERVICE_UNAVAILABLE';
      } else if (statusCode === 400 || statusCode === 404) {
        err.errorCode = 'AI_INVALID_REQUEST';
      } else {
        err.errorCode = 'AI_COMMUNICATION_ERROR';
      }

      throw err;
    }

    // Validate response candidate
    const candidateText = data?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!candidateText || typeof candidateText !== 'string' || !candidateText.trim()) {
      const err = new Error('Model returned an empty response candidate.');
      err.statusCode = 502;
      err.errorCode = 'AI_EMPTY_RESPONSE';
      throw err;
    }

    return {
      text: candidateText.trim(),
      model: modelName,
      usage: data?.usageMetadata || null
    };
  } catch (error) {
    clearTimeout(timeoutId);
    if (error.name === 'AbortError') {
      const err = new Error('Connection to Gemini timed out.');
      err.statusCode = 504;
      err.errorCode = 'AI_TIMEOUT';
      throw err;
    }
    throw error;
  }
}

function parseDelay(str) {
  if (typeof str !== 'string') return 1000;
  const match = str.match(/(\d+(?:\.\d+)?)s?/);
  if (match) {
    return Math.min(10000, Math.floor(parseFloat(match[1]) * 1000));
  }
  return 1000;
}
