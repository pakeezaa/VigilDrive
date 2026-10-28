package com.example.vigildrive;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LifecycleFragment extends Fragment {
    private static final String TAG = "LIFECYCLE";

    public LifecycleFragment(int layoutId) { super(layoutId); }

    private void log(String callback) {
        Log.d(TAG, getClass().getSimpleName() + " -> " + callback);
    }

    @Override public void onAttach(@NonNull Context c) { super.onAttach(c); log("onAttach"); }
    @Override public void onCreate(@Nullable Bundle b) { super.onCreate(b); log("onCreate"); }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle b) {
        super.onViewCreated(v, b); log("onViewCreated");
    }
    @Override public void onStart()  { super.onStart();  log("onStart"); }
    @Override public void onResume() { super.onResume(); log("onResume"); }
    @Override public void onPause()  { log("onPause");   super.onPause(); }
    @Override public void onStop()   { log("onStop");    super.onStop(); }
    @Override public void onDestroyView() { log("onDestroyView"); super.onDestroyView(); }
    @Override public void onDestroy() { log("onDestroy"); super.onDestroy(); }
    @Override public void onDetach()  { log("onDetach");  super.onDetach(); }
}