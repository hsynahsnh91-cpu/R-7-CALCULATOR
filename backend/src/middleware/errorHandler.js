import { logger } from '../utils/logger.js';

export function errorHandler(err, req, res, next) {
  const requestId = req.id || 'r7-unknown';
  const statusCode = err.statusCode || 500;
  const errorCode = err.errorCode || 'INTERNAL_SERVER_ERROR';

  logger.error('REQUEST_ERROR', {
    requestId,
    statusCode,
    errorCode,
    message: err.message,
    path: req.path
  });

  const safeMessage = (statusCode >= 500)
    ? 'An unexpected error occurred while processing your request. Please try again later.'
    : err.message;

  res.status(statusCode).json({
    success: false,
    error_code: errorCode,
    message: safeMessage,
    request_id: requestId
  });
}
