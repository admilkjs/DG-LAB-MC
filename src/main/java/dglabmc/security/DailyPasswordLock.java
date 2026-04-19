package dglabmc.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DailyPasswordLock {
    private static final String SALT = "admilk-dglab-lock-v1";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private static String unlockedDateToken = "";

    private DailyPasswordLock() {
    }

    public static synchronized boolean isUnlocked() {
        return currentDateToken().equals(unlockedDateToken);
    }

    public static synchronized boolean unlock(String password) {
        if (!matchesToday(password)) {
            return false;
        }
        unlockedDateToken = currentDateToken();
        return true;
    }

    public static synchronized void clearExpiredLock() {
        if (!currentDateToken().equals(unlockedDateToken)) {
            unlockedDateToken = "";
        }
    }

    public static boolean matchesToday(String password) {
        String candidate = password == null ? "" : password.trim();
        return !candidate.isEmpty() && todayPassword().equalsIgnoreCase(candidate);
    }

    public static String todayPassword() {
        return generatePassword(currentDateToken());
    }

    public static String currentDateToken() {
        return LocalDate.now(ZoneId.systemDefault()).format(DATE_FORMAT);
    }

    public static String generatePassword(String dateToken) {
        if (dateToken == null || dateToken.trim().isEmpty()) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return sha256Hex(SALT + ":" + dateToken.trim()).substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(encoded.length * 2);
            for (byte current : encoded) {
                builder.append(String.format(Locale.ROOT, "%02x", current & 0xFF));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("缺少 SHA-256 算法", exception);
        }
    }
}
