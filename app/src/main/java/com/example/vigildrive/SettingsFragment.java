package com.example.vigildrive;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends LifecycleFragment {

    private Prefs prefs;
    private BeepPlayer beep;
    private boolean loading = false;   // true while we fill the controls (no saving, no beeping)

    private SwitchMaterial swBeep;
    private SeekBar sbVolume, sbAway;
    private TextView tvVolume, tvAway;
    private RadioGroup rgTone;

    public SettingsFragment() { super(R.layout.fragment_settings); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());

        swBeep = view.findViewById(R.id.swBeep);
        sbVolume = view.findViewById(R.id.sbVolume);
        sbAway = view.findViewById(R.id.sbAway);
        tvVolume = view.findViewById(R.id.tvVolume);
        tvAway = view.findViewById(R.id.tvAway);
        rgTone = view.findViewById(R.id.rgTone);

        loadValues();

        swBeep.setOnCheckedChangeListener((b, checked) -> {
            if (!loading) prefs.setBeepOn(checked);
        });

        sbVolume.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                tvVolume.setText("Beep volume: " + p + "%");
                if (fromUser && !loading) prefs.setVolume(p);
            }
            @Override public void onStopTrackingTouch(SeekBar s) { testBeep(); }
        });

        rgTone.setOnCheckedChangeListener((group, id) -> {
            if (loading) return;
            int index = id == R.id.rbSoft ? 0 : id == R.id.rbNormal ? 1 : 2;
            prefs.setToneIndex(index);
            testBeep();
        });

        sbAway.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int seconds = 10 + p;                      // range 10 to 60 seconds
                tvAway.setText("Beep after looking away: " + seconds + " s");
                if (fromUser && !loading) prefs.setAwaySeconds(seconds);
            }
        });

        view.findViewById(R.id.btnTest).setOnClickListener(v -> testBeep());
        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            prefs.resetSettings();
            loadValues();
        });
    }

    // Reads every setting from SharedPreferences and shows it on screen
    private void loadValues() {
        loading = true;
        swBeep.setChecked(prefs.isBeepOn());
        sbVolume.setProgress(prefs.getVolume());
        tvVolume.setText("Beep volume: " + prefs.getVolume() + "%");
        int tone = prefs.getToneIndex();
        rgTone.check(tone == 0 ? R.id.rbSoft : tone == 1 ? R.id.rbNormal : R.id.rbLoud);
        sbAway.setProgress(prefs.getAwaySeconds() - 10);
        tvAway.setText("Beep after looking away: " + prefs.getAwaySeconds() + " s");
        loading = false;
    }

    private void testBeep() {
        if (beep != null) beep.release();
        beep = new BeepPlayer(prefs);   // picks up the newest volume
        beep.beep(true);
    }

    @Override
    public void onPause() {
        if (beep != null) {
            beep.release();
            beep = null;
        }
        super.onPause();
    }

    // Small helper so we only override the methods we need
    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { }
        @Override public void onStartTrackingTouch(SeekBar s) { }
        @Override public void onStopTrackingTouch(SeekBar s) { }
    }
}