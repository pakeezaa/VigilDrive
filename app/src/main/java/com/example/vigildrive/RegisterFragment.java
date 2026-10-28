package com.example.vigildrive;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterFragment extends LifecycleFragment {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirm;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirm;
    private Prefs prefs;

    public RegisterFragment() { super(R.layout.fragment_register); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());

        tilName = view.findViewById(R.id.tilName);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPhone = view.findViewById(R.id.tilPhone);
        tilPassword = view.findViewById(R.id.tilPassword);
        tilConfirm = view.findViewById(R.id.tilConfirm);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirm = view.findViewById(R.id.etConfirm);

        view.findViewById(R.id.btnRegister).setOnClickListener(v -> attemptRegister());
        view.findViewById(R.id.tvGoLogin).setOnClickListener(v ->
                ((AuthActivity) requireActivity()).showLogin());
    }

    private String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void attemptRegister() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirm.setError(null);

        String name = text(etName);
        String email = text(etEmail);
        String phone = text(etPhone);
        String pass = text(etPassword);
        String confirm = text(etConfirm);

        if (name.isEmpty()) { tilName.setError("Enter your name"); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email"); return;
        }
        if (phone.length() < 7) { tilPhone.setError("Enter a valid phone number"); return; }
        if (pass.length() < 6) { tilPassword.setError("At least 6 characters"); return; }
        if (!pass.equals(confirm)) { tilConfirm.setError("Passwords do not match"); return; }

        prefs.register(name, email, phone, pass);   // also marks the user as logged in
        Toast.makeText(requireContext(), "Account created. Welcome, " + name + "!",
                Toast.LENGTH_SHORT).show();
        ((AuthActivity) requireActivity()).goToMain();
    }
}