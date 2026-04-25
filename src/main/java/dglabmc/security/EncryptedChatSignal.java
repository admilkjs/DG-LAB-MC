package dglabmc.security;

import dglabmc.rule.TriggerRegistry;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.Pattern;

public final class EncryptedChatSignal {
    private EncryptedChatSignal() {
    }

    public static String createSignal(String playerName, String triggerId) {
        return createSignal(playerName, triggerId, DailyPasswordLock.currentDateToken());
    }

    public static String createSignal(String playerName, String triggerId, String dateToken) {
        String target = normalizePlayerName(playerName);
        String trigger = normalizeTrigger(triggerId);
        String date = normalizeDate(dateToken);
        String payload = (String) SecurityVm.v(1, trigger, target, date);
        return ((String) SecurityVm.v(3)) + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypt(payload, date));
    }

    public static String createDeathSignal(String playerName) {
        return createSignal(playerName, TriggerRegistry.PLAYER_DEATH, DailyPasswordLock.currentDateToken());
    }

    public static String createDeathSignal(String playerName, String dateToken) {
        return createSignal(playerName, TriggerRegistry.PLAYER_DEATH, dateToken);
    }

    public static boolean containsDeathSignal(String message, String playerName) {
        return TriggerRegistry.PLAYER_DEATH.equals(extractSignalTrigger(message, playerName));
    }

    public static String extractSignalTrigger(String message, String playerName) {
        String encodedPayload = extractEncodedPayload(message);
        if (encodedPayload.isEmpty() || playerName == null || playerName.trim().isEmpty()) {
            return "";
        }
        String triggerId = decodeTriggerForPlayer(encodedPayload, playerName);
        return triggerId.isEmpty() ? "" : triggerId;
    }

    public static String extractEncodedPayload(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        Matcher matcher = ((Pattern) SecurityVm.v(4)).matcher(message);
        if (matcher.find()) {
            String payload = matcher.group(1);
            return payload == null ? "" : payload;
        }
        return "";
    }

    private static String decodeTriggerForPlayer(String encodedPayload, String playerName) {
        String dateToken = DailyPasswordLock.currentDateToken();
        String decrypted = decrypt(encodedPayload, dateToken);
        if (decrypted.isEmpty()) {
            return "";
        }
        String[] parts = (String[]) SecurityVm.v(5, decrypted);
        if (parts.length != 4) {
            return "";
        }
        if (!((String) SecurityVm.v(6)).equals(parts[0])) {
            return "";
        }
        if (!dateToken.equals(parts[3])) {
            return "";
        }
        if (!normalizePlayerName(playerName).equals(parts[2])) {
            return "";
        }
        String triggerId = parseTrigger(parts[1]);
        return triggerId == null ? "" : triggerId;
    }

    private static byte[] encrypt(String payload, String dateToken) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(resolveKey(dateToken), "AES"));
            return cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("生成密文失败", exception);
        }
    }

    private static String decrypt(String encodedPayload, String dateToken) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(resolveKey(dateToken), "AES"));
            byte[] encrypted = Base64.getUrlDecoder().decode(encodedPayload);
            byte[] plain = cipher.doFinal(encrypted);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static byte[] resolveKey(String dateToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(((String) SecurityVm.v(2, normalizeDate(dateToken))).getBytes(StandardCharsets.UTF_8));
            byte[] key = new byte[16];
            System.arraycopy(hashed, 0, key, 0, key.length);
            return key;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("缺少 SHA-256 算法", exception);
        }
    }

    private static String normalizePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("玩家名不能为空");
        }
        return playerName.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeDate(String dateToken) {
        if (dateToken == null || dateToken.trim().isEmpty()) {
            throw new IllegalArgumentException("日期不能为空");
        }
        return dateToken.trim();
    }

    private static String normalizeTrigger(String triggerId) {
        String parsed = parseTrigger(triggerId);
        if (parsed == null) {
            throw new IllegalArgumentException("不支持的触发事件");
        }
        return parsed;
    }

    private static String parseTrigger(String triggerId) {
        if (triggerId == null || triggerId.trim().isEmpty()) {
            return null;
        }
        String normalized = triggerId.trim().toLowerCase(Locale.ROOT);
        if ("death".equals(normalized)) {
            return TriggerRegistry.PLAYER_DEATH;
        }
        return TriggerRegistry.isKnown(normalized) ? normalized : null;
    }
}
