export const logger = {
  info(event, meta = {}) {
    this._log('INFO', event, meta);
  },
  warn(event, meta = {}) {
    this._log('WARN', event, meta);
  },
  error(event, meta = {}) {
    this._log('ERROR', event, meta);
  },

  _log(level, event, meta) {
    const sanitizedMeta = { ...meta };
    // Redact any potential sensitive fields
    const sensitiveKeys = ['key', 'apiKey', 'geminiApiKey', 'token', 'auth', 'password', 'secret', 'authorization'];
    for (const key of Object.keys(sanitizedMeta)) {
      if (sensitiveKeys.some(s => key.toLowerCase().includes(s))) {
        sanitizedMeta[key] = '[REDACTED]';
      }
    }

    const logEntry = {
      timestamp: new Date().toISOString(),
      level,
      event,
      ...sanitizedMeta
    };

    const out = JSON.stringify(logEntry);
    if (level === 'ERROR') {
      console.error(out);
    } else if (level === 'WARN') {
      console.warn(out);
    } else {
      console.log(out);
    }
  }
};
