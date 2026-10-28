package com.example.vigildrive;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class LifecycleActivity extends AppCompatActivity {
    private static final String TAG = "LIFECYCLE";

    private void log(String callback) {
        Log.d(TAG, getClass().getSimpleName() + " -> " + callback);
    }

    @Override protected void onCreate(@Nullable Bundle b) { super.onCreate(b); log("onCreate"); }
    @Override protected void onStart()   { super.onStart();   log("onStart"); }
    @Override protected void onResume()  { super.onResume();  log("onResume"); }
    @Override protected void onPause()   { log("onPause");    super.onPause(); }
    @Override protected void onStop()    { log("onStop");     super.onStop(); }
    @Override protected void onRestart() { super.onRestart(); log("onRestart"); }
    @Override protected void onDestroy() { log("onDestroy");  super.onDestroy(); }
}