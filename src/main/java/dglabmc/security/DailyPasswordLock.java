package dglabmc.security;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DailyPasswordLock {
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
        return (String) SecurityVm.v(0, dateToken);
    }
}
