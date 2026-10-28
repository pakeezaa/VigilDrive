package com.example.vigildrive;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginFragment extends LifecycleFragment {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Prefs prefs;

    public LoginFragment() { super(R.layout.fragment_login); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());

        tilEmail = view.findViewById(R.id.tilEmail);
        tilPassword = view.findViewById(R.id.tilPassword);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        TextView tvGoRegister = view.findViewById(R.id.tvGoRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoRegister.setOnClickListener(v -> ((AuthActivity) requireActivity()).showRegister());
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
        String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();

        if (email.isEmpty()) { tilEmail.setError("Enter your email"); return; }
        if (pass.isEmpty()) { tilPassword.setError("Enter your password"); return; }

        if (!prefs.hasAccount()) {
            Toast.makeText(requireContext(),
                    "No account found. Please register first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (prefs.checkLogin(email, pass)) {
            prefs.setLoggedIn(true);
            ((AuthActivity) requireActivity()).goToMain();
        } else {
            tilPassword.setError("Wrong email or password");
        }
    }
}