// In-memory LRU cache for deterministic questions
const CACHE_MAX_ENTRIES = 500;
const CACHE_TTL_MS = 3600000; // 1 hour

class ResponseCache {
  constructor() {
    this.cache = new Map();
  }

  _generateKey(question, language, subject) {
    return `${language}:${subject}:${question.trim().toLowerCase()}`;
  }

  get(question, language, subject) {
    const key = this._generateKey(question, language, subject);
    const entry = this.cache.get(key);
    if (!entry) return null;

    if (Date.now() > entry.expiresAt) {
      this.cache.delete(key);
      return null;
    }

    // Refresh LRU position
    this.cache.delete(key);
    this.cache.set(key, entry);
    return entry.value;
  }

  set(question, language, subject, value) {
    const key = this._generateKey(question, language, subject);
    if (this.cache.size >= CACHE_MAX_ENTRIES) {
      // Evict oldest entry
      const oldestKey = this.cache.keys().next().value;
      if (oldestKey) this.cache.delete(oldestKey);
    }

    this.cache.set(key, {
      value,
      expiresAt: Date.now() + CACHE_TTL_MS
    });
  }
}

export const responseCache = new ResponseCache();
