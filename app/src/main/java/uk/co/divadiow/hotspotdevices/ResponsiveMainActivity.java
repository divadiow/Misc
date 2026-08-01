package uk.co.divadiow.hotspotdevices;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Applies system-bar insets and responsive sizing around the original scanner UI.
 * The scanning implementation remains in MainActivity.
 */
public final class ResponsiveMainActivity extends MainActivity {
    private static final int BG = Color.rgb(11, 15, 20);

    private int lastUsableWidth = -1;
    private float lastFontScale = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        configureSystemBars();

        ScrollView scroll = findFirstView(findViewById(android.R.id.content), ScrollView.class);
        if (scroll == null) return;

        scroll.setClipToPadding(true);
        scroll.setFillViewport(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                view.setPadding(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom()
                );
                return insets;
            });
            scroll.requestApplyInsets();
        }

        scroll.addOnLayoutChangeListener((view, left, top, right, bottom,
                                           oldLeft, oldTop, oldRight, oldBottom) ->
                applyResponsiveLayout(scroll));
        scroll.post(() -> applyResponsiveLayout(scroll));
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);
        window.getDecorView().setBackgroundColor(BG);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        }
    }

    private void applyResponsiveLayout(ScrollView scroll) {
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;

        LinearLayout root = (LinearLayout) scroll.getChildAt(0);
        int usableWidth = Math.max(0,
                scroll.getWidth() - scroll.getPaddingLeft() - scroll.getPaddingRight());
        float density = getResources().getDisplayMetrics().density;
        float fontScale = getResources().getConfiguration().fontScale;

        if (usableWidth == lastUsableWidth && Math.abs(fontScale - lastFontScale) < 0.001f) return;
        lastUsableWidth = usableWidth;
        lastFontScale = fontScale;

        int widthDp = density == 0f ? 0 : Math.round(usableWidth / density);
        boolean narrow = widthDp > 0 && widthDp < 360;
        boolean veryNarrow = widthDp > 0 && widthDp < 330;
        boolean largeText = fontScale >= 1.25f;

        int sidePadding = dp(widthDp >= 600 ? 24 : narrow ? 12 : 18);
        int topPadding = dp(narrow ? 12 : 18);
        root.setPadding(sidePadding, topPadding, sidePadding, dp(28));

        ViewGroup.LayoutParams rawParams = root.getLayoutParams();
        rawParams.width = usableWidth > 0 ? Math.min(usableWidth, dp(720)) : ViewGroup.LayoutParams.MATCH_PARENT;
        rawParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        if (rawParams instanceof FrameLayout.LayoutParams) {
            ((FrameLayout.LayoutParams) rawParams).gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        }
        root.setLayoutParams(rawParams);

        TextView title = findTextView(root, "Hotspot Devices");
        if (title != null) {
            title.setTextSize(narrow ? 24 : 28);
            title.setMaxLines(2);
        }

        TextView subtitle = findTextView(root,
                "Find IP addresses on your phone's hotspot without root or Termux.");
        if (subtitle != null) subtitle.setTextSize(narrow ? 14 : 15);

        EditText subnet = findFirstView(root, EditText.class);
        if (subnet != null) {
            subnet.setMinWidth(0);
            subnet.setMinimumWidth(0);
            subnet.setTextSize(narrow ? 15 : 17);
        }

        CheckBox autoRefresh = findFirstView(root, CheckBox.class);
        if (autoRefresh != null) {
            autoRefresh.setMinWidth(0);
            autoRefresh.setMinimumWidth(0);
            autoRefresh.setTextSize(narrow ? 14 : 15);
        }

        Button scan = findButton(root, "Scan now");
        Button stop = findButton(root, "Stop");
        Button share = findButton(root, "Share");
        if (scan == null || stop == null || share == null) return;
        if (!(scan.getParent() instanceof LinearLayout)) return;

        LinearLayout actions = (LinearLayout) scan.getParent();
        for (Button button : new Button[]{scan, stop, share}) {
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setSingleLine(true);
            button.setTextSize(narrow ? 14 : 15);
        }

        if (veryNarrow || largeText) {
            actions.setOrientation(LinearLayout.VERTICAL);
            actions.setGravity(Gravity.FILL_HORIZONTAL);
            setButtonLayout(scan, ViewGroup.LayoutParams.MATCH_PARENT, dp(50), 0f, 0);
            setButtonLayout(stop, ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 8);
            setButtonLayout(share, ViewGroup.LayoutParams.MATCH_PARENT, dp(48), 0f, 8);
        } else {
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            setButtonLayout(scan, 0, dp(50), 1.65f, 0);
            setButtonLayout(stop, 0, dp(50), 0.82f, 8);
            setButtonLayout(share, 0, dp(50), 0.82f, 8);
        }
    }

    private void setButtonLayout(Button button, int width, int height, float weight, int startMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.setMarginStart(dp(startMarginDp));
        params.topMargin = startMarginDp > 0 &&
                ((LinearLayout) button.getParent()).getOrientation() == LinearLayout.VERTICAL
                ? dp(startMarginDp) : 0;
        if (((LinearLayout) button.getParent()).getOrientation() == LinearLayout.VERTICAL) {
            params.setMarginStart(0);
        }
        button.setLayoutParams(params);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button findButton(View root, String text) {
        TextView candidate = findTextView(root, text);
        return candidate instanceof Button ? (Button) candidate : null;
    }

    private TextView findTextView(View root, String exactText) {
        if (root instanceof TextView && exactText.contentEquals(((TextView) root).getText())) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView found = findTextView(group.getChildAt(i), exactText);
                if (found != null) return found;
            }
        }
        return null;
    }

    private <T extends View> T findFirstView(View root, Class<T> type) {
        if (root == null) return null;
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findFirstView(group.getChildAt(i), type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
