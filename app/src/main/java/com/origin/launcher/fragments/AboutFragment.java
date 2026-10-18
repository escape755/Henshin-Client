package com.origin.launcher.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.origin.launcher.R;
import com.origin.launcher.discord.DiscordRPCHelper;

public class AboutFragment extends BaseThemedFragment {

    private static final String TAG = "AboutFragment";
    private TextView versionText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        versionText = view.findViewById(R.id.version_text);
        loadVersionInformation();
        return view;
    }

    private void loadVersionInformation() {
        try {
            PackageInfo packageInfo = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            versionText.setText("Henshin Client v" + packageInfo.versionName
                    + " (Build " + packageInfo.versionCode + ")");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting app version", e);
            versionText.setText("Henshin Client - Version unavailable");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        DiscordRPCHelper.getInstance().updateMenuPresence("About");
    }

    @Override
    public void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updateIdlePresence();
    }
}
