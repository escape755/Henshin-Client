package com.origin.launcher.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.Uri;
import com.bumptech.glide.Glide;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; 
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.animation.OvershootInterpolator;
import android.os.Handler;
import androidx.core.content.res.ResourcesCompat;
import android.content.res.ColorStateList;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;

import java.util.List;
import java.util.ArrayList;

import androidx.annotation.NonNull;

import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import com.origin.launcher.Launcher.MinecraftLauncher;
import com.origin.launcher.versions.GameVersion;
import com.origin.launcher.versions.VersionManager;
import com.origin.launcher.databinding.ActivityMainBinding;
import com.origin.launcher.utils.FeatureSettings;
import com.origin.launcher.animation.DynamicAnim;
import com.origin.launcher.fragments.HomeFragment;
import com.origin.launcher.fragments.AccountFragment;
import android.widget.Button;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModSizeStore;
import com.origin.launcher.fragments.SettingsFragment;
import com.origin.launcher.manager.ThemeManager;
import com.origin.launcher.utils.ThemeUtils;
import com.origin.launcher.discord.DiscordRPCHelper;
import com.origin.launcher.discord.DiscordLoginActivity;
import com.origin.launcher.R;

public class MainActivity extends BaseThemedActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_DISCLAIMER_SHOWN = "disclaimer_shown";
    private static final String KEY_THEMES_DIALOG_SHOWN = "themes_dialog_shown";
    private static final String KEY_STORAGE_PERMS_ASKED = "storage_perms_asked";
    private static final int REQ_STORAGE_PERMS = 100;

    private SettingsFragment settingsFragment;
    private AccountFragment accountFragment;
    private int currentFragmentIndex = 0;
    private LinearProgressIndicator globalProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        checkFirstLaunch();
        InbuiltModSizeStore.getInstance().init(getApplicationContext());
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
        
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            String presenceActivity = "";
            int newIndex = -1;
            
            if (item.getItemId() == R.id.navigation_account) {
                if (accountFragment == null) {
                    accountFragment = new AccountFragment();
                }
                selectedFragment = accountFragment;
                presenceActivity = "In Account";
                newIndex = 0;
            } else if (item.getItemId() == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
                presenceActivity = "In Home";
                newIndex = 1;
            } else if (item.getItemId() == R.id.navigation_settings) {
                if (settingsFragment == null) {
                    settingsFragment = new SettingsFragment();
                }
                selectedFragment = settingsFragment;
                presenceActivity = "In Settings";
                newIndex = 2;
            }

            if (selectedFragment != null) {
                boolean isForward = newIndex > getCurrentFragmentIndex();
                navigateToFragmentWithAnimation(selectedFragment, isForward);
                setCurrentFragmentIndex(newIndex);
                DiscordRPCHelper.getInstance().updatePresence(presenceActivity, "Using the best MCPE Client");
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
            setCurrentFragmentIndex(1);
        }
        // Home is the middle tab and the landing page.
        bottomNavigationView.getMenu().findItem(R.id.navigation_home).setChecked(true);
    }
    
    private int getCurrentFragmentIndex() {
        return currentFragmentIndex;
    }

    private void setCurrentFragmentIndex(int index) {
        this.currentFragmentIndex = index;
    }

    private void navigateToFragmentWithAnimation(Fragment fragment, boolean isForward) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        
        if (isForward) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right, 
                R.anim.slide_out_left, 
                R.anim.slide_in_left,  
                R.anim.slide_out_right 
            );
        } else {
            transaction.setCustomAnimations(
                R.anim.slide_in_left,  
                R.anim.slide_out_right,  
                R.anim.slide_in_right,  
                R.anim.slide_out_left 
            );
        }
        
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);
        boolean storageAsked = prefs.getBoolean(KEY_STORAGE_PERMS_ASKED, false);

        if (isFirstLaunch) {
            if (!storageAsked) {
                ensureStorageAccess(prefs);
                return;
            }

            showFirstLaunchDialog(prefs, disclaimerShown);
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else if (!storageAsked) {
            ensureStorageAccess(prefs);
        } else if (!disclaimerShown) {
            showDisclaimerDialog(prefs);
        }
    }

    private void ensureStorageAccess(SharedPreferences prefs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_STORAGE_PERMS);
                return;
            }
        } else {
            int read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (read != PackageManager.PERMISSION_GRANTED || write != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        REQ_STORAGE_PERMS);
                return;
            }
        }
        
        prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
        continueFirstLaunchFlow(prefs);
    }

    private void createNoMediaFile() {
        try {
            java.io.File baseDir = new java.io.File(android.os.Environment.getExternalStorageDirectory(), "games/henshin_app");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            java.io.File noMediaFile = new java.io.File(baseDir, ".nomedia");
            if (!noMediaFile.exists()) {
                noMediaFile.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void continueFirstLaunchFlow(SharedPreferences prefs) {
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        boolean disclaimerShown = prefs.getBoolean(KEY_DISCLAIMER_SHOWN, false);

        if (isFirstLaunch) {
            showFirstLaunchDialog(prefs, disclaimerShown);
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else if (!disclaimerShown) {
            showDisclaimerDialog(prefs);
        }
    }

    private void showFirstLaunchDialog(SharedPreferences prefs, boolean disclaimerShown) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Welcome to Henshin Client")
                .setMessage("Launch Minecraft once before doing anything, to make the config load properly")
                .setIcon(R.drawable.ic_info)
                .setPositiveButton("Proceed", (dialog, which) -> {
                    dialog.dismiss();
                    if (!disclaimerShown) {
                        showDisclaimerDialog(prefs);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void showDisclaimerDialog(SharedPreferences prefs) {
        new MaterialAlertDialogBuilder(this, com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog)
                .setTitle("Important Disclaimer")
                .setMessage("This application is not affiliated with, endorsed by, or related to Mojang Studios, Microsoft Corporation, or any of their subsidiaries. "
                        + "Minecraft is a trademark of Mojang Studios. This is an independent third-party launcher. "
                        + "By clicking 'I Understand', you acknowledge that you use this launcher at your own risk and that the developers are not responsible for any issues that may arise.")
                .setIcon(R.drawable.ic_warning)
                .setPositiveButton("I Understand", (dialog, which) -> {
                    dialog.dismiss();
                    prefs.edit().putBoolean(KEY_DISCLAIMER_SHOWN, true).apply();
                })
                .setCancelable(false)
                .show();
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        Log.d(TAG, "MainActivity onActivityResult: requestCode=" + requestCode + 
              ", resultCode=" + resultCode + ", data=" + (data != null ? "present" : "null"));
              
        if (requestCode == REQ_STORAGE_PERMS) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            createNoMediaFile();
            prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
            continueFirstLaunchFlow(prefs);
            return;
        }
        
        if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE && accountFragment != null) {
            accountFragment.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode == DiscordLoginActivity.DISCORD_LOGIN_REQUEST_CODE && settingsFragment != null) {
            Log.d(TAG, "Forwarding Discord login result to SettingsFragment");
            settingsFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE_PERMS) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_STORAGE_PERMS_ASKED, true).apply();
            continueFirstLaunchFlow(prefs);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        DiscordRPCHelper.getInstance().updatePresence("Using Henshin Client", "Using the best MCPE Client");
    }
    
    @Override
    protected void onApplyTheme() {
        super.onApplyTheme();
        
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            try {
                int currentBackground = Color.parseColor("#141414"); 
                if (bottomNavigationView.getBackground() != null) {
                    try {
                        currentBackground = ((android.graphics.drawable.ColorDrawable) bottomNavigationView.getBackground()).getColor();
                    } catch (Exception e) {
                    }
                }
                
                int targetBackground = ThemeManager.getInstance().getColor("surface");
                
                ThemeUtils.animateBackgroundColorTransition(bottomNavigationView, currentBackground, targetBackground, 300);
                
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            } catch (Exception e) {
                ThemeUtils.applyThemeToBottomNavigation(bottomNavigationView);
            }
        }
    }

    public void showGlobalProgress(int max) {
        if (globalProgress == null) {
            globalProgress = findViewById(R.id.global_download_progress);
        }
        if (globalProgress != null) {
            if (max > 0) {
                globalProgress.setIndeterminate(false);
                globalProgress.setMax(max);
                globalProgress.setProgress(0);
            } else {
                globalProgress.setIndeterminate(true);
            }
            globalProgress.setVisibility(View.VISIBLE);
            globalProgress.bringToFront();
        }
    }

    public void updateGlobalProgress(int value) {
        if (globalProgress != null) {
            globalProgress.setIndeterminate(false);
            globalProgress.setProgressCompat(value, true);
        }
    }

    public void hideGlobalProgress() {
        if (globalProgress != null) {
            globalProgress.setVisibility(View.GONE);
            globalProgress.setIndeterminate(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        DiscordRPCHelper.getInstance().updatePresence("Henshin Client", "Using the best MCPE Client");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DiscordRPCHelper.getInstance().cleanup();
    }
}
