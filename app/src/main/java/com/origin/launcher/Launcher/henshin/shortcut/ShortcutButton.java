package com.origin.launcher.Launcher.henshin.shortcut;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import com.origin.launcher.Launcher.henshin.core.Module;

/**
 * One floating module shortcut, styled like AbySm's OverlayShortcutButton: a compact rounded pill
 * with the module name. Tap = toggle the module, drag = move it (always, no edit mode).
 * Lit border / brighter text while the module is on.
 */
final class ShortcutButton {
    // Same palette as AbySm
    private static final int ON_BORDER = 0xFFC9CED6, OFF_BORDER = 0x60686873;
    private static final int ON_TEXT = 0xFFE2E5EA, OFF_TEXT = 0xFFA8ADB5;
    private static final int[] ON_BG = {0xFF2A2A2F, 0xFF18181C};
    private static final int[] OFF_BG = {0xFF1A1A1F, 0xFF111114};

    private final Activity activity;
    private final Module module;
    private final ShortcutManager owner;
    private final WindowManager wm;
    private final float density;
    private final int touchSlop;
    private TextView view;
    private WindowManager.LayoutParams lp;
    private boolean shown;

    private float downRawX, downRawY, startX, startY;
    private boolean dragging;

    ShortcutButton(Activity a, Module m, ShortcutManager owner) {
        this.activity = a;
        this.module = m;
        this.owner = owner;
        this.wm = (WindowManager) a.getSystemService(Context.WINDOW_SERVICE);
        this.density = a.getResources().getDisplayMetrics().density;
        this.touchSlop = ViewConfiguration.get(a).getScaledTouchSlop();
    }

    private int dp(float v) { return (int) (v * density + 0.5f); }

    void show(int x, int y) {
        if (shown || activity.isFinishing() || activity.isDestroyed()) return;

        view = new TextView(activity);
        view.setText(module.name);
        view.setGravity(Gravity.CENTER);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        view.setMaxLines(1);
        view.setPadding(dp(10), dp(5), dp(10), dp(5));
        view.setOnTouchListener(this::onTouch);
        refresh();

        lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        lp.x = clampX(x);
        lp.y = clampY(y);
        lp.token = activity.getWindow().getDecorView().getWindowToken();
        try {
            wm.addView(view, lp);
            shown = true;
            // keep it on screen once its real size is known (e.g. after a rotation)
            view.post(() -> {
                if (!shown) return;
                lp.x = clampX(lp.x);
                lp.y = clampY(lp.y);
                try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
            });
        } catch (Exception e) {
            view = null;
        }
    }

    void hide() {
        if (!shown || view == null) return;
        try { wm.removeView(view); } catch (Exception ignored) {}
        view = null;
        shown = false;
    }

    /** Re-applies colours (on/off state) and the global opacity. */
    void refresh() {
        if (view == null) return;
        boolean on = module.isEnabled();
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, on ? ON_BG : OFF_BG);
        g.setCornerRadius(dp(10));
        g.setStroke((int) (1.5f * density + 0.5f), on ? ON_BORDER : OFF_BORDER);
        view.setBackground(g);
        view.setTextColor(on ? ON_TEXT : OFF_TEXT);
        view.setAlpha(owner.getOpacity() / 100f);
    }

    private int clampX(int x) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int w = view != null && view.getWidth() > 0 ? view.getWidth() : 0;
        return Math.max(0, Math.min(x, Math.max(0, dm.widthPixels - w)));
    }

    private int clampY(int y) {
        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        int h = view != null && view.getHeight() > 0 ? view.getHeight() : 0;
        return Math.max(0, Math.min(y, Math.max(0, dm.heightPixels - h)));
    }

    private boolean onTouch(View v, MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = e.getRawX();
                downRawY = e.getRawY();
                startX = lp.x;
                startY = lp.y;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getRawX() - downRawX, dy = e.getRawY() - downRawY;
                if (!dragging && Math.abs(dx) + Math.abs(dy) > touchSlop) dragging = true;
                if (dragging && shown) {
                    lp.x = clampX((int) (startX + dx));
                    lp.y = clampY((int) (startY + dy));
                    try { wm.updateViewLayout(view, lp); } catch (Exception ignored) {}
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    owner.onMoved(module.name, lp.x, lp.y);
                } else {
                    module.toggle();
                    refresh();
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                // El sistema canceló el gesto (notificación, gesto de navegación...): cerrar el arrastre
                // y guardar donde quedó el botón, sin disparar el toggle.
                if (dragging) owner.onMoved(module.name, lp.x, lp.y);
                dragging = false;
                return true;
            default:
                return true;
        }
    }
}
