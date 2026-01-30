package com.example.geschenkplaner;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Login-Status + Registrierung
 * Hinweis: Passwort später Firebase/Backend nutzen
 */
public final class AuthManager {

    private static final String PREFS = "auth_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";

    private AuthManager() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isLoggedIn(Context ctx) {
        return prefs(ctx).getBoolean(KEY_LOGGED_IN, false);
    }

    public static void logout(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, false).apply();
    }

    public static boolean register(Context ctx, String email, String password) {
        // Überschreibt vorhandenen Account
        prefs(ctx).edit()
                .putString(KEY_EMAIL, email)
                .putString(KEY_PASSWORD, password)
                .putBoolean(KEY_LOGGED_IN, true)
                .apply();
        return true;
    }

    public static boolean login(Context ctx, String email, String password) {
        String savedEmail = prefs(ctx).getString(KEY_EMAIL, null);
        String savedPass = prefs(ctx).getString(KEY_PASSWORD, null);

        boolean ok = email != null && password != null
                && email.equals(savedEmail)
                && password.equals(savedPass);

        if (ok) {
            prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, true).apply();
        }
        return ok;
    }
}
