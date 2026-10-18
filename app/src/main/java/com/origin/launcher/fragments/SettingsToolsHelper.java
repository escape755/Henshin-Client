package com.origin.launcher.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.origin.launcher.R;
import com.origin.launcher.utils.ThemeUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * File management (backup / import, config export / import, options.txt editor) and the
 * resource installer, each wired to its own card on the Settings page.
 * Create it in Fragment.onCreate (it registers activity-result launchers) and call bind() from onCreateView.
 */
public class SettingsToolsHelper {

    private static final String[] ROOT_CANDIDATES = {
            "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/",
            "/storage/emulated/0/games/com.mojang/",
            "/storage/emulated/0/Android/data/com.mojang.minecraftpe/files/games/com.mojang/"
    };
    private static final String BACKUP_TARGET =
            "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/";
    private static final String OPTIONS_PATH =
            "/storage/emulated/0/Android/data/com.origin.launcher/files/games/com.mojang/minecraftpe/options.txt";
    private static final String CONFIG_PATH =
            "/storage/emulated/0/games/henshin_app/henshin_mods/config.json";
    private static final String FILE_PROVIDER_AUTHORITY = "com.origin.launcher.fileprovider";

    private final Fragment fragment;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent> saveBackupLauncher;
    private final ActivityResultLauncher<Intent> importBackupLauncher;
    private final ActivityResultLauncher<Intent> importConfigLauncher;

    // options.txt editor
    private File optionsFile;
    private String originalOptionsContent = "";
    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private EditText optionsTextEditor;
    private View optionsEditorLayout;
    private TextInputLayout searchInputLayout;
    private TextInputEditText searchEditText;
    private MaterialButton editOptionsButton;
    private String currentSearchTerm = "";
    private final List<Integer> searchMatches = new ArrayList<>();
    private int currentMatchIndex = -1;

    public SettingsToolsHelper(Fragment fragment) {
        this.fragment = fragment;
        saveBackupLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    Intent d = r.getData();
                    if (r.getResultCode() == Activity.RESULT_OK && d != null && d.getData() != null) {
                        File root = findRootDir();
                        if (root != null) createBackupAtLocation(d.getData(), root);
                    }
                });
        importBackupLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    Intent d = r.getData();
                    if (r.getResultCode() == Activity.RESULT_OK && d != null && d.getData() != null) {
                        importBackup(d.getData());
                    }
                });
        importConfigLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), r -> {
                    Intent d = r.getData();
                    if (r.getResultCode() == Activity.RESULT_OK && d != null && d.getData() != null) {
                        importConfig(d.getData());
                    }
                });
    }

    private Context ctx() { return fragment.requireContext(); }

    private void toast(String msg, boolean longer) {
        if (fragment.getContext() == null) return;
        Toast.makeText(fragment.getContext(), msg, longer ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------------------ wiring

    public void bind(View root) {
        View backup = root.findViewById(R.id.backup_button);
        View importBackup = root.findViewById(R.id.import_backup_button);
        View exportConfig = root.findViewById(R.id.export_config_button);
        View importConfig = root.findViewById(R.id.import_config_button);
        View resource = root.findViewById(R.id.resource_button);

        if (backup != null) backup.setOnClickListener(v -> {
            if (!hasStoragePermission()) { requestStoragePermissions(); return; }
            if (findRootDir() == null) { toast("No Minecraft data found to backup", true); return; }
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/zip");
            i.putExtra(Intent.EXTRA_TITLE, "mojang_backup.zip");
            saveBackupLauncher.launch(i);
        });

        if (importBackup != null) importBackup.setOnClickListener(v -> {
            if (!hasStoragePermission()) { requestStoragePermissions(); return; }
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("application/zip");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            importBackupLauncher.launch(Intent.createChooser(i, "Select Backup Zip File"));
        });

        if (exportConfig != null) exportConfig.setOnClickListener(v -> shareConfigFile());

        if (importConfig != null) importConfig.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            importConfigLauncher.launch(Intent.createChooser(i, "Select config.json"));
        });

        if (resource != null) resource.setOnClickListener(v -> {
            try {
                fragment.requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.slide_fade_in_right, R.anim.slide_out_right,
                                R.anim.slide_in_left, R.anim.slide_out_left)
                        .replace(R.id.fragment_container, new ResourceFragment())
                        .addToBackStack(null)
                        .commit();
            } catch (Exception e) {
                toast("Failed to open resource installer", false);
            }
        });

        bindOptionsEditor(root);
    }

    // ------------------------------------------------------------------------------ storage

    private File findRootDir() {
        List<String> paths = new ArrayList<>();
        for (String p : ROOT_CANDIDATES) paths.add(p);
        File ext = ctx().getExternalFilesDir(null);
        if (ext != null) paths.add(ext + "/games/com.mojang/");
        for (String path : paths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null && files.length > 0) return dir;
            }
        }
        return null;
    }

    private boolean hasStoragePermission() {
        Context c = ctx();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(c, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            toast("Please grant 'All files access' permission", true);
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + ctx().getPackageName()));
                fragment.startActivity(intent);
            } catch (Exception e) {
                fragment.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            fragment.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
    }

    // ------------------------------------------------------------------------------ backup

    private void createBackupAtLocation(Uri saveUri, File rootDir) {
        File[] files = rootDir.listFiles();
        if (files == null || files.length == 0) {
            toast("No files found to backup in: " + rootDir.getAbsolutePath(), true);
            return;
        }
        toast("Creating backup...", false);
        final Context app = ctx().getApplicationContext();
        io.execute(() -> {
            boolean ok = false;
            String error = null;
            try (OutputStream os = app.getContentResolver().openOutputStream(saveUri);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                zipRecursive(rootDir, rootDir.getAbsolutePath(), zos);
                ok = true;
            } catch (Exception e) {
                error = e.getMessage();
            }
            final boolean fOk = ok;
            final String fErr = error;
            postToast(app, fOk ? "Backup saved successfully!" : "Backup failed: " + fErr);
        });
    }

    private void zipRecursive(File f, String basePath, ZipOutputStream zos) throws IOException {
        if (f.isHidden()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) zipRecursive(c, basePath, zos);
            return;
        }
        if (!f.canRead()) return;
        String name = f.getAbsolutePath().replace(basePath, "").replaceFirst("^/", "");
        if (name.isEmpty()) name = f.getName();
        try (FileInputStream in = new FileInputStream(f)) {
            zos.putNextEntry(new ZipEntry(name));
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) zos.write(buf, 0, n);
            zos.closeEntry();
        } catch (IOException e) {
            // skip unreadable files, keep going
        }
    }

    private void importBackup(Uri zipUri) {
        final Context app = ctx().getApplicationContext();
        File target = new File(BACKUP_TARGET);
        if (!target.exists() && !target.mkdirs()) {
            toast("Could not create target directory: " + target.getAbsolutePath(), true);
            return;
        }
        toast("Importing backup...", false);
        io.execute(() -> {
            try (InputStream in = app.getContentResolver().openInputStream(zipUri)) {
                if (in == null) { postToast(app, "Could not read the selected file"); return; }
                extractZip(in, target);
                postToast(app, "Backup imported successfully!");
            } catch (Exception e) {
                postToast(app, "Import failed: " + e.getMessage());
            }
        });
    }

    private void extractZip(InputStream in, File targetDir) throws IOException {
        String canonicalRoot = targetDir.getCanonicalPath() + File.separator;
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry e;
            byte[] buf = new byte[8192];
            while ((e = zis.getNextEntry()) != null) {
                File out = new File(targetDir, e.getName());
                // zip-slip guard: never write outside the target folder
                if (!out.getCanonicalPath().startsWith(canonicalRoot)) continue;
                if (e.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) continue;
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) fos.write(buf, 0, n);
                    }
                }
                zis.closeEntry();
            }
        }
    }

    private void postToast(Context app, String msg) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> Toast.makeText(app, msg, Toast.LENGTH_LONG).show());
    }

    // ------------------------------------------------------------------------------ config

    private void shareConfigFile() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) { toast("Config doesn't exist yet", false); return; }
        try {
            Uri uri = FileProvider.getUriForFile(ctx(), FILE_PROVIDER_AUTHORITY, file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/json");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, "config.json");
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fragment.startActivity(Intent.createChooser(send, "Share config.json"));
        } catch (Exception e) {
            toast("Could not share config: " + e.getMessage(), true);
        }
    }

    private void importConfig(Uri uri) {
        final Context app = ctx().getApplicationContext();
        io.execute(() -> {
            try (InputStream in = app.getContentResolver().openInputStream(uri)) {
                if (in == null) { postToast(app, "Could not read the selected file"); return; }
                ByteArrayCopy copy = ByteArrayCopy.read(in);
                new org.json.JSONObject(new String(copy.data, "UTF-8")); // must be valid JSON
                File dest = new File(CONFIG_PATH);
                File dir = dest.getParentFile();
                if (dir != null && !dir.exists()) dir.mkdirs();
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(copy.data);
                }
                postToast(app, "Config imported");
            } catch (org.json.JSONException e) {
                postToast(app, "That file isn't a valid config.json");
            } catch (Exception e) {
                postToast(app, "Import failed: " + e.getMessage());
            }
        });
    }

    private static final class ByteArrayCopy {
        final byte[] data;
        private ByteArrayCopy(byte[] d) { data = d; }
        static ByteArrayCopy read(InputStream in) throws IOException {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return new ByteArrayCopy(bos.toByteArray());
        }
    }

    // ------------------------------------------------------------------------------ options.txt editor

    private void bindOptionsEditor(View root) {
        optionsFile = new File(OPTIONS_PATH);
        editOptionsButton = root.findViewById(R.id.editOptionsButton);
        TextView notFound = root.findViewById(R.id.optionsNotFoundText);
        optionsEditorLayout = root.findViewById(R.id.optionsEditorLayout);
        optionsTextEditor = root.findViewById(R.id.optionsTextEditor);
        searchInputLayout = root.findViewById(R.id.searchInputLayout);
        searchEditText = root.findViewById(R.id.searchEditText);
        if (editOptionsButton == null || optionsTextEditor == null) return;

        MaterialButton save = root.findViewById(R.id.saveOptionsButton);
        MaterialButton undo = root.findViewById(R.id.undoOptionsButton);
        MaterialButton redo = root.findViewById(R.id.redoOptionsButton);
        MaterialButton find = root.findViewById(R.id.searchOptionsButton);
        MaterialButton close = root.findViewById(R.id.closeEditorButton);

        ThemeUtils.applyThemeToButton(editOptionsButton, ctx());

        if (optionsFile.exists()) {
            editOptionsButton.setVisibility(View.VISIBLE);
            notFound.setVisibility(View.GONE);
            editOptionsButton.setOnClickListener(v -> {
                if (hasStoragePermission()) openOptionsEditor(); else requestStoragePermissions();
            });
        } else {
            editOptionsButton.setVisibility(View.GONE);
            notFound.setVisibility(View.VISIBLE);
        }

        if (save != null) save.setOnClickListener(v -> saveOptionsFile());
        if (undo != null) undo.setOnClickListener(v -> undoChanges());
        if (redo != null) redo.setOnClickListener(v -> redoChanges());
        if (close != null) close.setOnClickListener(v -> closeOptionsEditor());
        if (find != null) find.setOnClickListener(v -> {
            if (searchInputLayout.getVisibility() == View.GONE) {
                searchInputLayout.setVisibility(View.VISIBLE);
                searchEditText.requestFocus();
            } else {
                String term = searchEditText.getText().toString().trim();
                if (!term.isEmpty()) findNextMatch(term);
            }
        });

        searchEditText.addTextChangedListener(new SimpleWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String term = s.toString().trim();
                if (term.isEmpty()) clearSearchResults(); else searchInText(term);
            }
        });
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String term = searchEditText.getText().toString().trim();
                if (!term.isEmpty()) findNextMatch(term);
                return true;
            }
            return false;
        });

        optionsTextEditor.addTextChangedListener(new SimpleWatcher() {
            @Override public void afterTextChanged(Editable s) {
                String now = s.toString();
                if (!now.equals(originalOptionsContent) && !undoStack.isEmpty() && !now.equals(undoStack.peek())) {
                    undoStack.push(now);
                    redoStack.clear();
                }
            }
        });
        // the editor sits inside the page's ScrollView: let it scroll on its own
        optionsTextEditor.setOnTouchListener((v, event) -> {
            v.requestFocus();
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
    }

    private abstract static class SimpleWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {}
    }

    private void openOptionsEditor() {
        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(optionsFile))) {
                String line;
                while ((line = reader.readLine()) != null) content.append(line).append("\n");
            }
            originalOptionsContent = content.toString();
            optionsTextEditor.setText(originalOptionsContent);
            undoStack.clear();
            redoStack.clear();
            undoStack.push(originalOptionsContent);
            optionsEditorLayout.setVisibility(View.VISIBLE);
            editOptionsButton.setEnabled(false);
            editOptionsButton.setText("Editor Open");
        } catch (IOException e) {
            toast("Failed to load options.txt: " + e.getMessage(), true);
        }
    }

    private void saveOptionsFile() {
        try {
            String content = optionsTextEditor.getText().toString();
            try (FileWriter writer = new FileWriter(optionsFile)) { writer.write(content); }
            originalOptionsContent = content;
            toast("Options.txt saved successfully", false);
        } catch (IOException e) {
            toast("Failed to save options.txt: " + e.getMessage(), true);
        }
    }

    private void undoChanges() {
        if (undoStack.size() > 1) {
            int cursor = optionsTextEditor.getSelectionStart();
            redoStack.push(optionsTextEditor.getText().toString());
            undoStack.pop();
            String prev = undoStack.peek();
            optionsTextEditor.setText(prev);
            optionsTextEditor.setSelection(Math.max(0, Math.min(cursor, prev.length())));
        } else {
            toast("Nothing to undo", false);
        }
    }

    private void redoChanges() {
        if (!redoStack.isEmpty()) {
            int cursor = optionsTextEditor.getSelectionStart();
            String next = redoStack.pop();
            undoStack.push(next);
            optionsTextEditor.setText(next);
            optionsTextEditor.setSelection(Math.max(0, Math.min(cursor, next.length())));
        } else {
            toast("Nothing to redo", false);
        }
    }

    private void searchInText(String term) {
        currentSearchTerm = term;
        findAllMatches(term);
        SpannableString spannable = new SpannableString(optionsTextEditor.getText().toString());
        for (int idx : searchMatches) {
            spannable.setSpan(new BackgroundColorSpan(0xFFFFFF00), idx, idx + term.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        optionsTextEditor.setText(spannable);
        currentMatchIndex = -1;
    }

    private void findAllMatches(String term) {
        searchMatches.clear();
        String lower = optionsTextEditor.getText().toString().toLowerCase();
        String lowerTerm = term.toLowerCase();
        int i = lower.indexOf(lowerTerm);
        while (i >= 0) {
            searchMatches.add(i);
            i = lower.indexOf(lowerTerm, i + 1);
        }
    }

    private void clearSearchResults() {
        searchMatches.clear();
        currentMatchIndex = -1;
        currentSearchTerm = "";
        optionsTextEditor.setText(optionsTextEditor.getText().toString());
    }

    private void closeOptionsEditor() {
        optionsEditorLayout.setVisibility(View.GONE);
        searchInputLayout.setVisibility(View.GONE);
        editOptionsButton.setEnabled(true);
        editOptionsButton.setText("Edit options.txt");
        undoStack.clear();
        redoStack.clear();
    }

    private void findNextMatch(String term) {
        if (!term.equals(currentSearchTerm)) {
            currentSearchTerm = term;
            findAllMatches(term);
            currentMatchIndex = -1;
        }
        if (searchMatches.isEmpty()) { toast("No matches found", false); return; }
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size();
        int pos = searchMatches.get(currentMatchIndex);
        optionsTextEditor.setSelection(pos, pos + term.length());
        optionsTextEditor.requestFocus();
        android.text.Layout layout = optionsTextEditor.getLayout();
        if (layout != null) {
            int line = layout.getLineForOffset(pos);
            int top = layout.getLineTop(line);
            int lineH = layout.getLineBottom(line) - top;
            optionsTextEditor.scrollTo(0, Math.max(0, top - optionsTextEditor.getHeight() / 2 + lineH / 2));
        }
        toast("Match " + (currentMatchIndex + 1) + " of " + searchMatches.size(), false);
    }
}
