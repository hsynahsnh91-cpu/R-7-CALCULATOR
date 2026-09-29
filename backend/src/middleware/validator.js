import { config } from '../config/env.js';

const VALID_SUBJECTS = new Set(['math', 'physics', 'engineering', 'programming', 'conversion', 'general']);
const VALID_MODES = new Set(['solve', 'explain', 'convert', 'fast', 'pro']);
const VALID_LANGUAGES = new Set(['ar', 'en']);

export function validateSolveRequest(req, res, next) {
  const body = req.body;

  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    return res.status(400).json({
      success: false,
      error_code: 'INVALID_REQUEST_BODY',
      message: 'Request body must be a valid JSON object.',
      request_id: req.id
    });
  }

  const { question, subject, mode, language } = body;

  // Validate question
  if (typeof question !== 'string' || question.trim().length === 0) {
    return res.status(400).json({
      success: false,
      error_code: 'MISSING_QUESTION',
      message: 'The "question" field is required and must not be empty.',
      request_id: req.id
    });
  }

  const trimmedQuestion = question.trim();
  if (trimmedQuestion.length > config.maxRequestLength) {
    return res.status(400).json({
      success: false,
      error_code: 'QUESTION_TOO_LONG',
      message: `The question exceeds the maximum allowed length of ${config.maxRequestLength} characters.`,
      request_id: req.id
    });
  }

  // Validate subject (default to 'math' if omitted)
  const normalizedSubject = (subject && typeof subject === 'string') ? subject.trim().toLowerCase() : 'math';
  if (!VALID_SUBJECTS.has(normalizedSubject)) {
    return res.status(400).json({
      success: false,
      error_code: 'INVALID_SUBJECT',
      message: `Invalid subject. Supported: ${Array.from(VALID_SUBJECTS).join(', ')}`,
      request_id: req.id
    });
  }

  // Validate mode (default to 'solve' if omitted)
  const normalizedMode = (mode && typeof mode === 'string') ? mode.trim().toLowerCase() : 'solve';
  if (!VALID_MODES.has(normalizedMode)) {
    return res.status(400).json({
      success: false,
      error_code: 'INVALID_MODE',
      message: `Invalid mode. Supported: ${Array.from(VALID_MODES).join(', ')}`,
      request_id: req.id
    });
  }

  // Validate or infer language
  let normalizedLanguage = (language && typeof language === 'string') ? language.trim().toLowerCase() : null;
  if (!normalizedLanguage || !VALID_LANGUAGES.has(normalizedLanguage)) {
    // Detect if question contains Arabic characters
    const hasArabic = /[\u0600-\u06FF]/.test(trimmedQuestion);
    normalizedLanguage = hasArabic ? 'ar' : 'en';
  }

  // Attach sanitized fields to req
  req.validatedInput = {
    question: trimmedQuestion,
    subject: normalizedSubject,
    mode: normalizedMode,
    language: normalizedLanguage,
    history: Array.isArray(body.history) ? body.history.slice(-6) : []
  };

  next();
}
