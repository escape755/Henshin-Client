package com.origin.launcher.Launcher.henshin.shortcut;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.origin.launcher.Launcher.henshin.core.HenshinSettings;
import com.origin.launcher.Launcher.henshin.core.Module;

/** One floating, draggable module toggle button. */
final class ShortcutButton {
    private static final int LONG_PRESS_MS = 600;

    private final Activity activity;
    private final Module module;
    private final ShortcutManager owner;
    private final WindowManager wm;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView view;
    private WindowManager.LayoutParams lp;
    private boolean shown;

    private float downRawX, downRawY, startX, startY;
    private boolean moved, longPressed;
    private final Runnable longPress = new Runnable() {
        @Override public void run() {
            if (HenshinSettings.isShortcutEditMode(activity) && !moved) {
                longPressed = true;
                owner.requestRemove(module.name);
            }
        }
    };

    ShortcutButton(Activity a, Module m, ShortcutManager owner) {
        this.activity = a;
        this.module = m;
        this.owner = owner;
        this.wm = (WindowManager) a.getSystemService(Context.WINDOW_SERVICE);
    }

    void show(int x, int y) {
        if (shown || activity.isFinishing() || activity.isDestroyed()) return;
        float d = activity.getResources().getDisplayMetrics().density;
        int size = (int) (owner.getSizeDp() * d);

        view = new TextView(activity);
        view.setText(shortLabel(module.name));
        view.setGravity(Gravity.CENTER);
        view.setTextColor(Color.WHITE);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, owner.getSizeDp() >= 60 ? 12 : 10);
        view.setMaxLines(2);
        view.setPadding(4, 4, 4, 4);
        view.setOnTouchListener(this::onTouch);
        refresh();

        lp = new WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = x;
        lp.y = y;
        lp.token = activity.getWindow().getDecorView().getWindowToken();
        try {
            wm.addView(view, lp);
            shown = true;
        } catch (Exception e) {
            view = null;
        }
    }

    void hide() {
        handler.removeCallbacks(longPress);
        if (!shown || view == null) return;
        try { wm.removeView(view); } catch (Exception ignored) {}
        view = null;
        shown = false;
    }

    void refresh() {
        if (view == null) return;
        boolean on = module.isEnabled();
        boolean edit = HenshinSettings.isShortcutEditMode(activity);
        int accent = HenshinSettings.getAccent(activity);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(on ? accent : 0xCC1B1B1F);
        g.setStroke((int) (activity.getResources().getDisplayMetrics().density * (edit ? 2.5f : 1.5f)),
                edit ? 0xFFFFFFFF : (on ? 0xFFFFFFFF : 0x66FFFFFF));
        view.setBackground(g);
        view.setAlpha(owner.getOpacity() / 100f);
    }

    private boolean onTouch(View v, MotionEvent e) {
        boolean edit = HenshinSettings.isShortcutEditMode(activity);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX(); downRawY = e.getRawY();
                startX = lp.x; startY = lp.y;
                moved = false; longPressed = false;
                if (edit) handler.postDelayed(longPress, LONG_PRESS_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!edit || longPressed) return true;
                float dx = e.getRawX() - downRawX, dy = e.getRawY() - downRawY;
                if (!moved && Math.abs(dx) + Math.abs(dy) > 12) { moved = true; handler.removeCallbacks(longPress); }
                if (moved && shown) {
                    lp.x = (int) Math.max(0, startX + dx);
                    lp.y = (int) Math.max(0, startY + dy);
                    try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
                }
                return true;
            case MotionEvent.ACTION_UP:
                handler.removeCallbacks(longPress);
                if (edit) {
                    if (moved && !longPressed) owner.onMoved(module.name, lp.x, lp.y);
                } else {
                    module.toggle();
                    refresh();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(longPress);
                return true;
            default:
                return true;
        }
    }

    private static String shortLabel(String n) { return n.length() > 9 ? n.substring(0, 8) + "." : n; }
}
