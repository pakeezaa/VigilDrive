package com.example.vigildrive;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends LifecycleActivity {

    private static final long SPLASH_MS = 3000;

    private View root, ring1, ring2, scanLine;
    private ImageView eye;
    private TextView title, tagline;

    private final List<Animator> loopAnimators = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable goNext = () ->
            root.animate().alpha(0f).setDuration(450).withEndAction(this::openNextScreen).start();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        root = findViewById(R.id.root);
        ring1 = findViewById(R.id.ring1);
        ring2 = findViewById(R.id.ring2);
        scanLine = findViewById(R.id.scanLine);
        eye = findViewById(R.id.eye);
        title = findViewById(R.id.title);
        tagline = findViewById(R.id.tagline);
    }

    // Animations start when the screen becomes visible...
    @Override
    protected void onStart() {
        super.onStart();
        root.setAlpha(1f);
        startAnimations();
        handler.postDelayed(goNext, SPLASH_MS);
    }

    // ...and stop when it is hidden, so nothing runs in the background
    @Override
    protected void onStop() {
        handler.removeCallbacks(goNext);
        for (Animator a : loopAnimators) a.cancel();
        loopAnimators.clear();
        root.animate().cancel();
        super.onStop();
    }

    private void startAnimations() {
        // 1. The eye pops in
        eye.setScaleX(0.3f);
        eye.setScaleY(0.3f);
        eye.setAlpha(0f);
        eye.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(800).setInterpolator(new OvershootInterpolator()).start();

        // 2. Two pulsing rings, one after the other
        pulse(ring1, 0);
        pulse(ring2, 800);

        // 3. A scanning line sweeps up and down over the eye
        scanLine.setAlpha(0.95f);
        ObjectAnimator scan = ObjectAnimator.ofFloat(scanLine, "translationY", -dp(34), dp(34));
        scan.setDuration(1100);
        scan.setRepeatMode(ValueAnimator.REVERSE);
        scan.setRepeatCount(ValueAnimator.INFINITE);
        scan.start();
        loopAnimators.add(scan);

        // 4. Name and tagline slide up and fade in
        title.setTranslationY(dp(24));
        title.animate().alpha(1f).translationY(0f).setStartDelay(500).setDuration(700).start();
        tagline.setTranslationY(dp(24));
        tagline.animate().alpha(1f).translationY(0f).setStartDelay(800).setDuration(700).start();
    }

    private void pulse(View ring, long delay) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(ring, "scaleX", 0.6f, 1.5f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(ring, "scaleY", 0.6f, 1.5f);
        ObjectAnimator al = ObjectAnimator.ofFloat(ring, "alpha", 0.8f, 0f);
        for (ObjectAnimator a : new ObjectAnimator[]{sx, sy, al}) a.setRepeatCount(ValueAnimator.INFINITE);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy, al);
        set.setDuration(1600);
        set.setStartDelay(delay);
        set.start();
        loopAnimators.add(set);
    }

    // Logged in already? Go straight to the app. Otherwise go to Login.
    private void openNextScreen() {
        boolean loggedIn = new Prefs(this).isLoggedIn();
        startActivity(new Intent(this, loggedIn ? MainActivity.class : AuthActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}