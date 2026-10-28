package com.example.vigildrive;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Prefs {
    private static final String FILE = "vigildrive_prefs";
    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // ---------- Account ----------
    public void register(String name, String email, String phone, String password) {
        sp.edit()
                .putString("name", name)
                .putString("email", email.trim().toLowerCase())
                .putString("phone", phone)
                .putString("pass_hash", hash(password))   // password is never stored as plain text
                .putBoolean("logged_in", true)
                .apply();
    }

    public boolean checkLogin(String email, String password) {
        return email.trim().equalsIgnoreCase(sp.getString("email", ""))
                && hash(password).equals(sp.getString("pass_hash", ""));
    }

    public boolean hasAccount() { return sp.contains("email"); }

    // ---------- Session ----------
    public boolean isLoggedIn() { return sp.getBoolean("logged_in", false); }
    public void setLoggedIn(boolean v) { sp.edit().putBoolean("logged_in", v).apply(); }

    // ---------- Profile ----------
    public String getName()  { return sp.getString("name", ""); }
    public String getEmail() { return sp.getString("email", ""); }
    public String getPhone() { return sp.getString("phone", ""); }
    public void updateProfile(String name, String phone) {
        sp.edit().putString("name", name).putString("phone", phone).apply();
    }

    // ---------- Alert settings ----------
    public boolean isBeepOn()      { return sp.getBoolean("beep_on", true); }
    public int getVolume()         { return sp.getInt("beep_volume", 80); }     // 0 to 100
    public int getToneIndex()      { return sp.getInt("beep_tone", 1); }        // 0 soft, 1 normal, 2 loud
    public int getAwaySeconds()    { return sp.getInt("away_seconds", 30); }    // 10 to 60

    public void setBeepOn(boolean v)   { sp.edit().putBoolean("beep_on", v).apply(); }
    public void setVolume(int v)       { sp.edit().putInt("beep_volume", v).apply(); }
    public void setToneIndex(int v)    { sp.edit().putInt("beep_tone", v).apply(); }
    public void setAwaySeconds(int v)  { sp.edit().putInt("away_seconds", v).apply(); }

    public void resetSettings() {
        sp.edit().remove("beep_on").remove("beep_volume")
                .remove("beep_tone").remove("away_seconds").apply();
    }

    private static String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }
}