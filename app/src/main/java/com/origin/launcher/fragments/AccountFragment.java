package com.origin.launcher.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.origin.launcher.R;
import com.origin.launcher.discord.DiscordHolder;
import com.origin.launcher.discord.DiscordLoginActivity;
import com.origin.launcher.discord.DiscordManager;
import com.origin.launcher.discord.DiscordRPCHelper;

/** Account page: profile header + Discord login (Rich Presence) + client info. */
public class AccountFragment extends BaseThemedFragment implements DiscordManager.DiscordLoginCallback {

    private DiscordManager discord;
    private final Handler main = new Handler(Looper.getMainLooper());

    private TextView avatar;
    private TextView nameText;
    private TextView subText;
    private TextView statusText;
    private MaterialButton loginButton;

    private int dp(float v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }
    private int color(int res) { return ContextCompat.getColor(requireContext(), res); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        discord = DiscordHolder.get(requireActivity());
        discord.setCallback(this);
        discord.setFragment(this);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(color(R.color.background));
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(-1, -2));

        TextView title = text("Account", 32, color(R.color.onBackground), false);
        title.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        root.addView(title, lp(0, 0, 0, dp(24)));

        // ---- profile hero ----
        MaterialCardView hero = card();
        LinearLayout heroRow = new LinearLayout(requireContext());
        heroRow.setGravity(Gravity.CENTER_VERTICAL);
        heroRow.setPadding(dp(20), dp(20), dp(20), dp(20));

        avatar = text("H", 28, Color.WHITE, true);
        avatar.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFFFF1A00);
        avatar.setBackground(circle);
        heroRow.addView(avatar, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout names = new LinearLayout(requireContext());
        names.setOrientation(LinearLayout.VERTICAL);
        nameText = text("Guest", 20, color(R.color.onSurface), true);
        subText = text("Not signed in", 13, color(R.color.onSurfaceVariant), false);
        names.addView(nameText);
        names.addView(subText);
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, -2, 1f);
        nl.leftMargin = dp(16);
        heroRow.addView(names, nl);
        hero.addView(heroRow);
        root.addView(hero, lp(0, 0, 0, dp(16)));

        // ---- discord ----
        MaterialCardView dc = card();
        LinearLayout dbox = new LinearLayout(requireContext());
        dbox.setOrientation(LinearLayout.VERTICAL);
        dbox.setPadding(dp(24), dp(20), dp(24), dp(20));
        dbox.addView(text("Discord", 20, color(R.color.onSurface), true));
        statusText = text("Not connected", 14, 0xFFF44336, true);
        dbox.addView(statusText, lp(0, dp(8), 0, dp(14)));
        loginButton = new MaterialButton(requireContext());
        loginButton.setText("Login with Discord");
        loginButton.setAllCaps(false);
        loginButton.setCornerRadius(dp(28));
        loginButton.setOnClickListener(v -> onLoginClicked());
        dbox.addView(loginButton, new LinearLayout.LayoutParams(-1, -2));
        dbox.addView(text("Connect Discord to show what you are playing (Rich Presence).", 12,
                color(R.color.onSurfaceVariant), false), lp(0, dp(12), 0, 0));
        dc.addView(dbox);
        root.addView(dc, lp(0, 0, 0, dp(16)));

        // ---- client info ----
        MaterialCardView info = card();
        LinearLayout ibox = new LinearLayout(requireContext());
        ibox.setOrientation(LinearLayout.VERTICAL);
        ibox.setPadding(dp(24), dp(20), dp(24), dp(20));
        ibox.addView(text("Henshin Client", 20, color(R.color.onSurface), true));
        ibox.addView(text(getString(R.string.henshinversion), 14, color(R.color.onSurfaceVariant), false),
                lp(0, dp(6), 0, 0));
        ibox.addView(text("In game: type .help in the chat for client commands.", 12,
                color(R.color.onSurfaceVariant), false), lp(0, dp(10), 0, 0));
        info.addView(ibox);
        root.addView(info, lp(0, 0, 0, 0));

        refresh();
        return scroll;
    }

    private void onLoginClicked() {
        if (discord.isLoggedIn()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout from Discord")
                    .setMessage("This will also disconnect Rich Presence.")
                    .setPositiveButton("Logout", (d, w) -> discord.logout())
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            loginButton.setEnabled(false);
            loginButton.setText("Connecting...");
            discord.login();
            main.postDelayed(() -> { if (isAdded() && !discord.isLoggedIn()) refresh(); }, 30000);
        }
    }

    private void refresh() {
        if (!isAdded() || loginButton == null) return;
        if (discord.isLoggedIn() && discord.getCurrentUser() != null) {
            DiscordManager.DiscordUser u = discord.getCurrentUser();
            String n = u.displayName != null ? u.displayName : u.username;
            nameText.setText(n);
            subText.setText("Signed in with Discord");
            avatar.setText(n == null || n.isEmpty() ? "H" : n.substring(0, 1).toUpperCase());
            statusText.setText("Connected");
            statusText.setTextColor(0xFF4CAF50);
            loginButton.setText("Logout");
            loginButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
        } else {
            nameText.setText("Guest");
            subText.setText("Not signed in");
            avatar.setText("H");
            statusText.setText("Not connected");
            statusText.setTextColor(0xFFF44336);
            loginButton.setText("Login with Discord");
            loginButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF5865F2));
        }
        loginButton.setEnabled(true);
    }

    // ---- helpers ----
    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(requireContext());
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private LinearLayout.LayoutParams lp(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(l, t, r, b);
        return p;
    }

    private MaterialCardView card() {
        MaterialCardView c = new MaterialCardView(requireContext());
        c.setRadius(dp(16));
        c.setCardElevation(dp(8));
        c.setCardBackgroundColor(color(R.color.surface));
        c.setStrokeWidth(dp(1));
        c.setStrokeColor(color(R.color.outline));
        return c;
    }

    // ---- Discord callbacks ----
    @Override public void onLoginSuccess(DiscordManager.DiscordUser user) {
        if (isAdded()) { Toast.makeText(getContext(), "Logged in as " + user.displayName, Toast.LENGTH_SHORT).show(); main.post(this::refresh); }
    }
    @Override public void onLoginError(String error) {
        if (isAdded()) { Toast.makeText(getContext(), "Discord login failed: " + error, Toast.LENGTH_LONG).show(); main.post(this::refresh); }
    }
    @Override public void onLogout() {
        if (isAdded()) { Toast.makeText(getContext(), "Logged out from Discord", Toast.LENGTH_SHORT).show(); main.post(this::refresh); }
    }
    @Override public void onRPCConnected() {
        if (isAdded()) discord.updatePresence("In Account", "Using Henshin Client");
    }
    @Override public void onRPCDisconnected() {}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE && discord != null) {
            discord.handleLoginResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (discord != null) {
            discord.setCallback(this);
            discord.setFragment(this);
            if (discord.isLoggedIn() && !discord.isRPCConnected()) discord.startRPC();
        }
        DiscordRPCHelper.getInstance().updateMenuPresence("Account");
        refresh();
    }
}
