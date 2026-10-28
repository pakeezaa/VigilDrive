package com.example.vigildrive;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileFragment extends LifecycleFragment {

    private Prefs prefs;
    private TextView tvAvatar;
    private TextInputLayout tilName, tilPhone;
    private TextInputEditText etName, etEmail, etPhone;

    public ProfileFragment() { super(R.layout.fragment_profile); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = new Prefs(requireContext());

        tvAvatar = view.findViewById(R.id.tvAvatar);
        tilName = view.findViewById(R.id.tilName);
        tilPhone = view.findViewById(R.id.tilPhone);
        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);

        // Fill the form from SharedPreferences
        etName.setText(prefs.getName());
        etEmail.setText(prefs.getEmail());
        etPhone.setText(prefs.getPhone());
        updateAvatar(prefs.getName());

        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
    }

    private void saveProfile() {
        tilName.setError(null);
        tilPhone.setError(null);

        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String phone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();

        if (name.isEmpty()) { tilName.setError("Name cannot be empty"); return; }
        if (phone.length() < 7) { tilPhone.setError("Enter a valid phone number"); return; }

        prefs.updateProfile(name, phone);
        updateAvatar(name);
        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
    }

    private void updateAvatar(String name) {
        tvAvatar.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
    }

    private void logout() {
        prefs.setLoggedIn(false);
        Intent i = new Intent(requireContext(), AuthActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }
}