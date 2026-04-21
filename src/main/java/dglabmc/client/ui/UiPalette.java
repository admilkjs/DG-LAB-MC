package dglabmc.client.ui;

public final class UiPalette {
    // ── Background ──
    public static final int BACKGROUND_TOP = 0xFF08101C;
    public static final int BACKGROUND_BOTTOM = 0xFF0F172A;

    // ── Panels ──
    public static final int SIDEBAR = 0xD90B1220;
    public static final int PANEL = 0xD9172233;
    public static final int PANEL_ELEVATED = 0xD9222E42;
    public static final int PANEL_MUTED = 0xB3182230;
    public static final int PANEL_INFO = 0x66172233;
    public static final int PANEL_INFO_DIM = 0x44172233;

    // ── Accent ──
    public static final int ACCENT = 0xFFF97316;
    public static final int ACCENT_SOFT = 0xE6EA580C;

    // ── Borders ──
    public static final int BORDER = 0xFF334155;
    public static final int BORDER_STRONG = 0xFF64748B;

    // ── Text ──
    public static final int TEXT_PRIMARY = 0xFFF8FAFC;
    public static final int TEXT_MUTED = 0xFF94A3B8;
    public static final int TEXT_DIM = 0xFF64748B;

    // ── Status ──
    public static final int SUCCESS = 0xFF22C55E;
    public static final int DANGER = 0xFFEF4444;
    public static final int WARNING = 0xFFF59E0B;
    public static final int INFO = 0xFF38BDF8;

    // ── Button: disabled ──
    public static final int BTN_DISABLED_BG = 0x99202838;

    // ── Button: primary ──
    public static final int BTN_PRIMARY_HOVER = 0xFFF97316;
    public static final int BTN_PRIMARY_IDLE = 0xFFE85D04;
    public static final int BTN_PRIMARY_BORDER = 0xFFFFEDD5;

    // ── Button: secondary ──
    public static final int BTN_SECONDARY_HOVER = 0xCC243041;
    public static final int BTN_SECONDARY_IDLE = 0xB31B2635;

    // ── Button: ghost ──
    public static final int BTN_GHOST_HOVER = 0xCC1E293B;
    public static final int BTN_GHOST_IDLE = 0x88202B39;

    // ── Button: danger ──
    public static final int BTN_DANGER_HOVER = 0xFFDC2626;
    public static final int BTN_DANGER_IDLE = 0xFF991B1B;
    public static final int BTN_DANGER_BORDER = 0xFFFCA5A5;

    // ── Button: tab ──
    public static final int BTN_TAB_ACTIVE_HOVER = 0xFF2A374B;
    public static final int BTN_TAB_ACTIVE_IDLE = 0xFF1E293B;
    public static final int BTN_TAB_IDLE_HOVER = 0xD9233043;
    public static final int BTN_TAB_IDLE_IDLE = 0x99172233;

    // ── Cards / rows ──
    public static final int CARD_BG = 0x66172233;
    public static final int CARD_HOVER = 0xCC243041;
    public static final int CARD_SELECTED = 0xB3233043;
    public static final int CARD_IDLE = 0x99172233;
    public static final int CARD_MUTED = 0x77202A38;
    public static final int CARD_FLOATING = 0xD9243041;

    // ── Status badge ──
    public static final int BADGE_BG = 0x77202838;
    public static final int BADGE_SUCCESS_BG = 0x6630522A;
    public static final int BADGE_DANGER_BG = 0x66402222;

    // ── HUD ──
    public static final int HUD_BG = 0xAA101721;

    // ── QR fallback ──
    public static final int QR_BG = 0xFFF8FAFC;
    public static final int QR_BORDER = 0xFFCBD5E1;
    public static final int QR_TEXT = 0xFF0F172A;

    private UiPalette() {
    }
}
