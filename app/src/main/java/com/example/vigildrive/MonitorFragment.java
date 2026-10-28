package com.example.vigildrive;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker;
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MonitorFragment extends LifecycleFragment {

    private static final String TAG = "VigilDrive";

    private static final int[] LEFT_EYE  = {362, 385, 387, 263, 373, 380};
    private static final int[] RIGHT_EYE = {33, 160, 158, 133, 153, 144};
    private static final float EAR_THRESHOLD = 0.20f;     // below this = eye closed
    private static final long CLOSED_LIMIT_MS = 2000;     // eyes closed 2 s = beep

    private PreviewView previewView;
    private TextView statusText, placeholder, tvDrowsy, tvAway;
    private Chronometer chrono;
    private Button btnToggle;

    private Prefs prefs;
    private BeepPlayer beep;
    private FaceLandmarker landmarker;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    private volatile boolean running = false;
    private long awayLimitMs;                              // from Settings (default 30 s)
    private long closedSince = 0, awaySince = 0;
    private int drowsyCount = 0, awayCount = 0;
    private boolean wasDrowsy = false, wasAway = false;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startMonitoring();
                else Toast.makeText(requireContext(),
                        "Camera permission is required", Toast.LENGTH_LONG).show();
            });

    public MonitorFragment() { super(R.layout.fragment_monitor); }

    // ---------------- Fragment lifecycle ----------------

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());
        previewView = view.findViewById(R.id.previewView);
        statusText = view.findViewById(R.id.statusText);
        placeholder = view.findViewById(R.id.placeholder);
        tvDrowsy = view.findViewById(R.id.tvDrowsy);
        tvAway = view.findViewById(R.id.tvAway);
        chrono = view.findViewById(R.id.chrono);
        btnToggle = view.findViewById(R.id.btnToggle);
        cameraExecutor = Executors.newSingleThreadExecutor();

        btnToggle.setOnClickListener(v -> {
            if (running) stopMonitoring();
            else requestStart();
        });
    }

    // Settings are read EVERY time the screen comes back, so changes made in the
    // Settings tab (volume, tone, delay) apply immediately.
    @Override
    public void onResume() {
        super.onResume();
        awayLimitMs = prefs.getAwaySeconds() * 1000L;
        beep = new BeepPlayer(prefs);
    }

    @Override
    public void onPause() {
        if (beep != null) {
            beep.release();
            beep = null;
        }
        super.onPause();
    }

    // The camera cannot run in the background, so monitoring stops when the screen is hidden
    @Override
    public void onStop() {
        stopMonitoring();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        stopMonitoring();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        super.onDestroyView();
    }

    // ---------------- Start / stop ----------------

    private void requestStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startMonitoring();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startMonitoring() {
        if (running) return;

        // Load the TFLite-based face model
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build();
            FaceLandmarker.FaceLandmarkerOptions options =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setRunningMode(RunningMode.LIVE_STREAM)
                            .setNumFaces(1)
                            .setResultListener((result, input) -> onResult(result))
                            .setErrorListener(e -> Log.e(TAG, "Model error: " + e.getMessage()))
                            .build();
            landmarker = FaceLandmarker.createFromOptions(requireContext(), options);
        } catch (Exception e) {
            Log.e(TAG, "Model load failed", e);
            Toast.makeText(requireContext(), "Could not load the AI model", Toast.LENGTH_LONG).show();
            return;
        }

        // Reset counters and timers
        awayLimitMs = prefs.getAwaySeconds() * 1000L;
        closedSince = 0;
        awaySince = 0;
        drowsyCount = 0;
        awayCount = 0;
        wasDrowsy = false;
        wasAway = false;
        running = true;

        // Screen changes
        tvDrowsy.setText("0");
        tvAway.setText("0");
        placeholder.setVisibility(View.GONE);
        statusText.setText("Alert - driving safely");
        btnToggle.setText("Stop Monitoring");
        btnToggle.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.alert_red)));
        chrono.setBase(SystemClock.elapsedRealtime());
        chrono.start();

        // Start the front camera
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                if (!isAdded() || !running) return;
                cameraProvider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this::analyze);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);
            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void stopMonitoring() {
        if (!running && landmarker == null) return;
        running = false;

        if (cameraProvider != null) cameraProvider.unbindAll();
        if (landmarker != null) {
            try { landmarker.close(); } catch (Exception ignored) { }
            landmarker = null;
        }

        if (chrono != null) chrono.stop();
        if (statusText != null && isAdded()) {
            statusText.setText("Monitoring is off");
            statusText.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
            placeholder.setVisibility(View.VISIBLE);
            btnToggle.setText("Start Monitoring");
            btnToggle.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.primary)));
        }
    }

    // ---------------- Each camera frame goes to the model ----------------

    private void analyze(ImageProxy proxy) {
        FaceLandmarker lm = landmarker;
        if (!running || lm == null) {
            proxy.close();
            return;
        }
        try {
            Bitmap bitmap = Bitmap.createBitmap(proxy.getWidth(), proxy.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(proxy.getPlanes()[0].getBuffer());

            Matrix matrix = new Matrix();
            matrix.postRotate(proxy.getImageInfo().getRotationDegrees());
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            lm.detectAsync(new BitmapImageBuilder(rotated).build(), SystemClock.uptimeMillis());
        } catch (Exception e) {
            Log.e(TAG, "analyze failed", e);
        } finally {
            proxy.close();
        }
    }

    // ---------------- Model output: decide, count, beep ----------------

    private void onResult(FaceLandmarkerResult result) {
        if (!running) return;
        long now = SystemClock.uptimeMillis();
        boolean eyesClosed = false;
        boolean away = false;

        if (result.faceLandmarks().isEmpty()) {
            away = true;   // no face visible = not looking at the road
        } else {
            List<NormalizedLandmark> lm = result.faceLandmarks().get(0);
            float avgEar = (ear(lm, LEFT_EYE) + ear(lm, RIGHT_EYE)) / 2f;
            eyesClosed = avgEar < EAR_THRESHOLD;

            float ratio = (lm.get(1).x() - lm.get(234).x())
                    / (lm.get(454).x() - lm.get(234).x());
            away = ratio < 0.3f || ratio > 0.7f;   // 0.5 = facing straight ahead
        }

        if (eyesClosed) { if (closedSince == 0) closedSince = now; } else closedSince = 0;
        if (away)       { if (awaySince == 0)   awaySince = now;   } else awaySince = 0;

        boolean drowsy = closedSince != 0 && (now - closedSince) > CLOSED_LIMIT_MS;
        boolean lookingAway = awaySince != 0 && (now - awaySince) > awayLimitMs;   // 30 s by default

        if (drowsy && !wasDrowsy) drowsyCount++;
        if (lookingAway && !wasAway) awayCount++;
        wasDrowsy = drowsy;
        wasAway = lookingAway;

        final boolean alarm = drowsy || lookingAway;
        BeepPlayer b = beep;
        if (alarm && b != null) b.beepThrottled(now);

        final String message = drowsy ? "Eyes closed! Wake up!"
                : lookingAway ? "Look at the road!"
                  : "Alert - driving safely";
        final int d = drowsyCount, a = awayCount;

        Activity act = getActivity();
        if (act == null) return;
        act.runOnUiThread(() -> {
            if (!isAdded() || !running) return;
            statusText.setText(message);
            statusText.setBackgroundColor(ContextCompat.getColor(requireContext(),
                    alarm ? R.color.alert_red : R.color.primary));
            tvDrowsy.setText(String.valueOf(d));
            tvAway.setText(String.valueOf(a));
        });
    }

    // Eye Aspect Ratio = (vertical distances) / (2 * horizontal distance)
    private float ear(List<NormalizedLandmark> lm, int[] i) {
        float v1 = dist(lm.get(i[1]), lm.get(i[5]));
        float v2 = dist(lm.get(i[2]), lm.get(i[4]));
        float h  = dist(lm.get(i[0]), lm.get(i[3]));
        return (v1 + v2) / (2f * h);
    }

    private float dist(NormalizedLandmark a, NormalizedLandmark b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}