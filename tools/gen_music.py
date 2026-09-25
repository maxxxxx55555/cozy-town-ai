"""Generate an original cozy town theme as a seamless 48-second mono WAV.

The soundtrack is intentionally procedural: soft chord pads, a warm bass,
a restrained music-box arpeggio and a very light shaker. No copyrighted or
external samples are used. Run from the project root:

    python tools/gen_music.py
"""
from __future__ import annotations

import math
import struct
import wave
from pathlib import Path

RATE = 22_050
DURATION = 48.0
BPM = 72.0
BEAT = 60.0 / BPM
BAR = BEAT * 4.0
CHORD_SECONDS = BAR * 2.0
OUT = Path("assets/music/cozy_theme.wav")

# C, G, Am, F: warm, optimistic and readable without becoming distracting.
CHORDS = [
    (261.63, 329.63, 392.00),  # C major
    (196.00, 246.94, 293.66),  # G major
    (220.00, 261.63, 329.63),  # A minor
    (174.61, 220.00, 261.63),  # F major
]
BASS = [130.81, 98.00, 110.00, 87.31]
MELODY = [523.25, 659.25, 783.99, 659.25, 587.33, 493.88, 440.00, 493.88]


def soft_env(x: float, length: float, attack: float = 0.08, release: float = 0.22) -> float:
    if x < 0.0 or x >= length:
        return 0.0
    a = min(1.0, x / max(attack, 1e-6))
    r = min(1.0, (length - x) / max(release, 1e-6))
    return max(0.0, min(a, r))


def note(freq: float, t: float, length: float, gain: float, phase: float = 0.0) -> float:
    # A restrained triangle-ish tone: fundamental plus quiet octave.
    return gain * (math.sin(TAU * freq * t + phase) + 0.16 * math.sin(TAU * freq * 2.0 * t + phase))


TAU = math.tau


def main() -> None:
    total = int(RATE * DURATION)
    samples = [0.0] * total
    chord_count = int(math.ceil(DURATION / CHORD_SECONDS))
    for chord_index in range(chord_count):
        chord = CHORDS[chord_index % len(CHORDS)]
        bass_freq = BASS[chord_index % len(BASS)]
        start = chord_index * CHORD_SECONDS
        end = min(DURATION, start + CHORD_SECONDS)
        first = int(start * RATE)
        last = int(end * RATE)
        for i in range(first, last):
            local = (i / RATE) - start
            remaining = end - (i / RATE)
            pad_env = soft_env(local, CHORD_SECONDS, 0.28, 0.45)
            pad = sum(note(freq, local, CHORD_SECONDS, 0.075, 0.17 * n) for n, freq in enumerate(chord))
            bass_env = soft_env(local, CHORD_SECONDS, 0.04, 0.35)
            bass = note(bass_freq, local, CHORD_SECONDS, 0.11 * bass_env, 0.0)
            # Gentle pulse on each beat, never a harsh drum.
            beat_pos = local % BEAT
            pulse = 0.0
            if beat_pos < 0.08:
                pulse = 0.028 * (1.0 - beat_pos / 0.08) ** 2
            samples[i] += pad * pad_env + bass + pulse

        # Music-box phrase: one note every half beat, alternating octaves.
        phrase_start = int(start * RATE)
        phrase_end = int(end * RATE)
        step = int(BEAT * 0.5 * RATE)
        for step_index, note_index in enumerate(range(0, max(1, (phrase_end - phrase_start) // step))):
            i = phrase_start + step_index * step
            if i >= phrase_end:
                break
            freq = MELODY[(chord_index * 2 + step_index) % len(MELODY)]
            length = min(BEAT * 0.46, end - (i / RATE))
            note_frames = min(total, i + int(length * RATE))
            enriched = []
            for j in range(i, note_frames):
                note_local = (j - i) / RATE
                env = soft_env(note_local, length, 0.008, 0.12)
                bell = note(freq, note_local, length, 0.042 * env, 0.2)
                shimmer = 0.012 * env * math.sin(TAU * freq * 2.01 * note_local)
                enriched.append(samples[j] + bell + shimmer)
            samples[i:note_frames] = enriched

    # Gentle stereo-like movement isn't possible in mono, but a slow ambience
    # keeps the loop alive. Normalize conservatively for mobile playback.
    peak = max(abs(value) for value in samples) or 1.0
    scale = 0.78 / peak
    fade = int(RATE * 0.015)
    pcm = bytearray()
    for i, value in enumerate(samples):
        v = value * scale
        if i < fade:
            v *= i / fade
        if i >= total - fade:
            v *= max(0.0, (total - i) / fade)
        pcm.extend(struct.pack("<h", int(max(-1.0, min(1.0, v)) * 32767)))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(RATE)
        handle.writeframes(pcm)
    print(f"Generated {OUT} ({DURATION:.0f}s, {RATE} Hz, {OUT.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
