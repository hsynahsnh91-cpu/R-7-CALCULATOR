import { config } from '../config/env.js';
import { callGeminiApi } from './geminiService.js';
import { logger } from '../utils/logger.js';

export async function executeWithModelRouting(payload, mode = 'solve') {
  // Determine candidate sequence based on mode and configuration
  const primary = (mode === 'fast') ? config.fastModel : config.primaryModel;
  const fallback = config.fallbackModel;
  const modelsToTry = [primary];
  if (fallback && fallback !== primary) {
    modelsToTry.push(fallback);
  }

  let lastError = null;
  const startTime = Date.now();

  for (let i = 0; i < modelsToTry.length; i++) {
    const model = modelsToTry[i];
    const isPrimary = (i === 0);
    const maxAttempts = isPrimary ? 2 : 1; // Retry primary once on transient spike

    for (let attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        const result = await callGeminiApi(model, payload);
        const duration = Date.now() - startTime;
        logger.info('MODEL_CALL_SUCCESS', {
          model,
          attempt,
          durationMs: duration,
          isFallback: (model !== primary)
        });
        return {
          ...result,
          durationMs: duration,
          wasFallback: (model !== primary)
        };
      } catch (err) {
        lastError = err;
        logger.warn('MODEL_CALL_FAILED', {
          model,
          attempt,
          errorCode: err.errorCode,
          statusCode: err.statusCode,
          message: err.message
        });

        // Only retry transient failures (429, 500, 502, 503, 504, timeout)
        const isTransient = [429, 500, 502, 503, 504].includes(err.statusCode) || err.errorCode === 'AI_TIMEOUT';
        if (!isTransient) {
          // Permanent client or request error - break immediately
          throw err;
        }

        if (attempt < maxAttempts) {
          // Exponential backoff with jitter
          const baseDelay = err.retryAfterMs || (attempt * 1200);
          const jitter = Math.floor(Math.random() * 400);
          await sleep(baseDelay + jitter);
        }
      }
    }
  }

  // All attempts exhausted
  throw lastError;
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
