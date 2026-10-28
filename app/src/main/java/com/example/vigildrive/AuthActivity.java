package com.example.vigildrive;

import android.content.Intent;
import android.os.Bundle;

public class AuthActivity extends LifecycleActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null) {   // first launch only
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.authContainer, new LoginFragment())
                    .commit();
        }
    }

    public void showRegister() {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.authContainer, new RegisterFragment())
                .addToBackStack(null)
                .commit();
    }

    public void showLogin() {
        getSupportFragmentManager().popBackStack();
    }

    public void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}