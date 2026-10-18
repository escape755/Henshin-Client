package com.origin.launcher.Launcher.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.origin.launcher.R;
import com.origin.launcher.Launcher.inbuilt.gui.ClickGuiWindow;
import com.origin.launcher.Launcher.inbuilt.manager.InbuiltModManager;
import com.origin.launcher.Launcher.inbuilt.model.ModIds;

/**
 * Auto Clicker: tap the floating button to start/stop. While active it sends a quick
 * synthetic tap to the centre of the screen (the crosshair) at the configured CPS.
 * The CPS is edited from the Henshin ClickGUI (Combat > Auto Clicker).
 */
public class AutoClickerOverlay extends BaseOverlayButton {

    // Pointer id used for synthetic taps. Real fingers get the lowest free ids (0, 1, 2...),
    // so a high id avoids clashing with the joystick / camera finger.
    private static final int SYNTHETIC_POINTER_ID = 15;
    private static final long PRESS_DURATION_MS = 20;

    private final Handler clickHandler = new Handler(Looper.getMainLooper());
    private boolean isActive = false;

    private final Runnable clickRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isActive) return;
            // Do not tap while the ClickGUI is open, otherwise it would hit the game UI behind it.
            if (!com.origin.launcher.Launcher.henshin.gui.GuiLauncher.isOpen()) {
                performTap();
            }
            clickHandler.postDelayed(this, getIntervalMs());
        }
    };

    public AutoClickerOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.AUTO_CLICKER;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.ic_autoclicker;
    }

    @Override
    protected void onOverlayViewCreated(ImageButton btn) {}

    @Override
    protected void onButtonClick() {
        setActive(!isActive);
    }

    private void setActive(boolean active) {
        if (isActive == active) return;
        isActive = active;
        clickHandler.removeCallbacks(clickRunnable);
        if (active) {
            clickHandler.post(clickRunnable);
        }
        updateButtonState(active);
    }

    private long getIntervalMs() {
        int cps = InbuiltModManager.getInstance(activity).getAutoClickerCps();
        return Math.max(1L, 1000L / Math.max(1, cps));
    }

    private void performTap() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            setActive(false);
            return;
        }
        View decor = activity.getWindow().getDecorView();
        float x = decor.getWidth() > 0
                ? decor.getWidth() / 2f
                : activity.getResources().getDisplayMetrics().widthPixels / 2f;
        float y = decor.getHeight() > 0
                ? decor.getHeight() / 2f
                : activity.getResources().getDisplayMetrics().heightPixels / 2f;

        final long downTime = SystemClock.uptimeMillis();
        dispatchSyntheticTouch(MotionEvent.ACTION_DOWN, downTime, downTime, x, y);
        clickHandler.postDelayed(
                () -> dispatchSyntheticTouch(MotionEvent.ACTION_UP, downTime, SystemClock.uptimeMillis(), x, y),
                PRESS_DURATION_MS
        );
    }

    private void dispatchSyntheticTouch(int action, long downTime, long eventTime, float x, float y) {
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[1];
        props[0] = new MotionEvent.PointerProperties();
        props[0].id = SYNTHETIC_POINTER_ID;
        props[0].toolType = MotionEvent.TOOL_TYPE_FINGER;

        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[1];
        coords[0] = new MotionEvent.PointerCoords();
        coords[0].x = x;
        coords[0].y = y;
        coords[0].pressure = 1f;
        coords[0].size = 1f;

        MotionEvent event = MotionEvent.obtain(
                downTime, eventTime, action,
                1, props, coords,
                0, 0, 1f, 1f,
                0, 0,
                InputDevice.SOURCE_TOUCHSCREEN, 0
        );
        try {
            activity.dispatchTouchEvent(event);
        } catch (Exception ignored) {
        } finally {
            event.recycle();
        }
    }

    private void updateButtonState(boolean active) {
        if (overlayView != null) {
            ImageButton btn = overlayView.findViewById(R.id.mod_overlay_button);
            if (btn != null) {
                btn.setActivated(active);
                btn.setAlpha(getButtonAlpha() * (active ? 1.1f : 1.0f));
                btn.setBackgroundResource(
                        active ? R.drawable.bg_overlay_button_active
                               : R.drawable.bg_overlay_button
                );
            }
        }
    }

    @Override
    public void hide() {
        // Always stop clicking when the overlay is removed (mod disabled, activity paused/destroyed).
        isActive = false;
        clickHandler.removeCallbacksAndMessages(null);
        super.hide();
    }
}
