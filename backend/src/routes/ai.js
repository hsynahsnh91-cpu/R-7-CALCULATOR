import { Router } from 'express';
import { validateSolveRequest } from '../middleware/validator.js';
import { trySolveLocally } from '../services/localSolver.js';
import { responseCache } from '../services/cacheService.js';
import { buildPromptPayload } from '../services/promptManager.js';
import { executeWithModelRouting } from '../services/modelRouter.js';
import { config } from '../config/env.js';
import { logger } from '../utils/logger.js';

const router = Router();

router.post('/api/v1/ai/solve', validateSolveRequest, async (req, res, next) => {
  const startTime = Date.now();
  const requestId = req.id;
  const { question, subject, mode, language, history } = req.validatedInput;

  try {
    // 1. LOCAL CALCULATION FIRST (Deterministic arithmetic & physics)
    const localResult = trySolveLocally(question, language);
    if (localResult) {
      const processingTime = Date.now() - startTime;
      logger.info('LOCAL_SOLVER_HIT', { requestId, subject, processingTimeMs: processingTime });
      return res.status(200).json({
        success: true,
        answer: localResult.answer,
        subject,
        ai_version: config.aiVersion,
        request_id: requestId,
        confidence: localResult.confidence,
        verification_status: localResult.verification_status,
        processing_time_ms: processingTime
      });
    }

    // 2. SAFE DETERMINISTIC CACHE
    const cached = responseCache.get(question, language, subject);
    if (cached) {
      const processingTime = Date.now() - startTime;
      logger.info('RESPONSE_CACHE_HIT', { requestId, subject, processingTimeMs: processingTime });
      return res.status(200).json({
        success: true,
        answer: cached.answer,
        subject,
        ai_version: config.aiVersion,
        request_id: requestId,
        confidence: cached.confidence || 'verified',
        verification_status: 'cached',
        processing_time_ms: processingTime
      });
    }

    // 3. BUILD SERVER-SIDE PROMPT
    const promptPayload = buildPromptPayload(req.validatedInput);

    // 4. CALL GEMINI VIA MODEL ROUTER WITH EXPONENTIAL BACKOFF
    const modelResult = await executeWithModelRouting(promptPayload, mode);
    const processingTime = Date.now() - startTime;

    // 5. CACHE SUCCESSFUL RESPONSE
    responseCache.set(question, language, subject, {
      answer: modelResult.text,
      confidence: 'verified'
    });

    logger.info('AI_SOLVE_SUCCESS', {
      requestId,
      subject,
      modelUsed: modelResult.model,
      processingTimeMs: processingTime
    });

    // 6. RETURN NORMALIZED RESPONSE
    return res.status(200).json({
      success: true,
      answer: modelResult.text,
      subject,
      ai_version: config.aiVersion,
      request_id: requestId,
      confidence: 'verified',
      verification_status: modelResult.wasFallback ? 'fallback_verified' : 'primary_verified',
      processing_time_ms: processingTime
    });
  } catch (error) {
    const processingTime = Date.now() - startTime;
    logger.error('AI_SOLVE_FAILED', {
      requestId,
      errorCode: error.errorCode,
      statusCode: error.statusCode,
      processingTimeMs: processingTime
    });

    // Translate rate limit and transient errors to clean messages
    let clientMessage = (language === 'ar')
      ? 'تعذر الحصول على رد من المساعد الذكي في الوقت الحالي. يُرجى إعادة المحاولة.'
      : 'Unable to retrieve a response from the AI assistant at this time. Please try again.';

    if (error.errorCode === 'AI_RATE_LIMITED' || error.statusCode === 429) {
      clientMessage = (language === 'ar')
        ? 'الخدمة الذكية مشغولة مؤقتًا بسبب كثرة الطلبات. يُرجى الانتظار بضع ثوانٍ والضغط على إعادة المحاولة.'
        : 'The AI service is temporarily busy due to high demand. Please wait a few seconds and try again.';
      res.setHeader('Retry-After', '8');
      return res.status(429).json({
        success: false,
        error_code: 'AI_RATE_LIMITED',
        message: clientMessage,
        request_id: requestId,
        processing_time_ms: processingTime
      });
    }

    if (error.errorCode === 'AI_SERVICE_UNAVAILABLE' || error.statusCode === 503) {
      clientMessage = (language === 'ar')
        ? 'خوادم الذكاء الاصطناعي تحت صيانة أو ضغط مؤقت. يُرجى إعادة المحاولة.'
        : 'AI servers are temporarily unavailable. Please retry in a moment.';
      return res.status(503).json({
        success: false,
        error_code: 'AI_SERVICE_UNAVAILABLE',
        message: clientMessage,
        request_id: requestId,
        processing_time_ms: processingTime
      });
    }

    if (error.errorCode === 'GEMINI_UNCONFIGURED') {
      clientMessage = (language === 'ar')
        ? 'خدمة الذكاء الاصطناعي قيد الإعداد على الخادم.'
        : 'AI service is currently being configured on the server.';
      return res.status(503).json({
        success: false,
        error_code: 'SERVICE_UNCONFIGURED',
        message: clientMessage,
        request_id: requestId,
        processing_time_ms: processingTime
      });
    }

    return res.status(error.statusCode || 500).json({
      success: false,
      error_code: error.errorCode || 'AI_COMMUNICATION_ERROR',
      message: clientMessage,
      request_id: requestId,
      processing_time_ms: processingTime
    });
  }
});

export default router;
