package dglabmc.client.ui;

import java.util.Locale;

public final class UiUtil {
    private UiUtil() {
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static String formatDouble(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    public static String trimChars(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    public static String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    public static String safeMessage(Throwable throwable, String fallback) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return fallback;
        }
        return throwable.getMessage();
    }

    public static int lerpColor(int from, int to, float progress) {
        float p = Math.max(0.0F, Math.min(1.0F, progress));
        int fa = (from >> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = (int) (fa + (ta - fa) * p);
        int r = (int) (fr + (tr - fr) * p);
        int g = (int) (fg + (tg - fg) * p);
        int b = (int) (fb + (tb - fb) * p);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
