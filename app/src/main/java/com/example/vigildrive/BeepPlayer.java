package com.example.vigildrive;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class BeepPlayer {
    // Soft, Normal, Loud
    private static final int[] TONES = {
            ToneGenerator.TONE_PROP_BEEP,
            ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
            ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
    };

    private final Prefs prefs;
    private ToneGenerator tone;
    private long lastBeep = 0;

    public BeepPlayer(Prefs prefs) {
        this.prefs = prefs;
        try {
            tone = new ToneGenerator(AudioManager.STREAM_ALARM, prefs.getVolume());
        } catch (RuntimeException e) {
            tone = null;
        }
    }

    // Plays one beep. force = true ignores the on/off switch (used by "Test beep")
    public void beep(boolean force) {
        if (tone == null || (!force && !prefs.isBeepOn())) return;
        tone.startTone(TONES[prefs.getToneIndex()], 600);
    }

    // Repeats about every 0.8 s while an alert is active
    public void beepThrottled(long nowMs) {
        if (nowMs - lastBeep > 800) {
            lastBeep = nowMs;
            beep(false);
        }
    }

    public void release() {
        if (tone != null) {
            tone.release();
            tone = null;
        }
    }
}