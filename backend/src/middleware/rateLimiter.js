import { config } from '../config/env.js';
import { logger } from '../utils/logger.js';

// In-memory sliding window rate limiter
const clientRecords = new Map();

// Periodic cleanup every 5 minutes to prevent memory leak
setInterval(() => {
  const now = Date.now();
  for (const [key, record] of clientRecords.entries()) {
    if (now - record.windowStart > config.rateLimitWindowMs * 2) {
      clientRecords.delete(key);
    }
  }
}, 300000).unref();

export function rateLimiter(req, res, next) {
  // Exclude health check
  if (req.path === '/health' || req.path === '/') {
    return next();
  }

  // Derive client identity from IP and optional user-agent/app key
  const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || 
                   req.socket?.remoteAddress || 
                   'unknown';
  const clientId = `${clientIp}`;

  const now = Date.now();
  let record = clientRecords.get(clientId);

  if (!record || (now - record.windowStart > config.rateLimitWindowMs)) {
    record = {
      windowStart: now,
      count: 1,
      burstWindowStart: now,
      burstCount: 1
    };
    clientRecords.set(clientId, record);
  } else {
    // Check burst window (10s)
    if (now - record.burstWindowStart > 10000) {
      record.burstWindowStart = now;
      record.burstCount = 1;
    } else {
      record.burstCount += 1;
    }

    record.count += 1;
  }

  // Check burst limit
  if (record.burstCount > config.rateLimitBurstMax) {
    logger.warn('RATE_LIMIT_BURST_EXCEEDED', { clientId, burstCount: record.burstCount });
    res.setHeader('Retry-After', '10');
    return res.status(429).json({
      success: false,
      error_code: 'RATE_LIMIT_EXCEEDED',
      message: 'Too many requests in a short interval. Please wait a few seconds.',
      request_id: req.id
    });
  }

  // Check window limit
  if (record.count > config.rateLimitMaxRequests) {
    const timeRemainingSec = Math.ceil((config.rateLimitWindowMs - (now - record.windowStart)) / 1000);
    logger.warn('RATE_LIMIT_WINDOW_EXCEEDED', { clientId, count: record.count });
    res.setHeader('Retry-After', String(timeRemainingSec > 0 ? timeRemainingSec : 60));
    return res.status(429).json({
      success: false,
      error_code: 'RATE_LIMIT_EXCEEDED',
      message: 'Rate limit exceeded. Please wait a minute before making another request.',
      request_id: req.id
    });
  }

  // Set standard rate limit headers
  res.setHeader('X-RateLimit-Limit', String(config.rateLimitMaxRequests));
  res.setHeader('X-RateLimit-Remaining', String(Math.max(0, config.rateLimitMaxRequests - record.count)));

  next();
}
