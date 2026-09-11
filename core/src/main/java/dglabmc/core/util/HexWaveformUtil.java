package dglabmc.core.util;

import dglabmc.core.wave.WaveformDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HexWaveformUtil {
    private HexWaveformUtil() {
    }

    public static List<String> toFrames(List<Integer> frequencies, List<Integer> strengths) {
        if (frequencies.size() != strengths.size()) {
            throw new IllegalArgumentException("频率采样数与强度采样数必须一致。");
        }

        List<String> frames = new ArrayList<String>();
        StringBuilder frequencyHex = new StringBuilder();
        StringBuilder strengthHex = new StringBuilder();
        for (int i = 0; i < frequencies.size(); i++) {
            frequencyHex.append(hexByte(getOutputValue(frequencies.get(i))));
            strengthHex.append(hexByte(clamp(strengths.get(i), 0, 100)));
            if ((i + 1) % 4 == 0) {
                frames.add(frequencyHex.toString() + strengthHex.toString());
                frequencyHex.setLength(0);
                strengthHex.setLength(0);
            }
        }
        if (frequencyHex.length() > 0 || strengthHex.length() > 0) {
            while (frequencyHex.length() < 8) {
                frequencyHex.append("0A");
            }
            while (strengthHex.length() < 8) {
                strengthHex.append("00");
            }
            frames.add(frequencyHex.toString() + strengthHex.toString());
        }
        return frames;
    }

    public static List<String> constantPulse(int frequencyMs, int strength, int samples) {
        List<Integer> frequencies = new ArrayList<Integer>();
        List<Integer> strengths = new ArrayList<Integer>();
        for (int i = 0; i < samples; i++) {
            frequencies.add(Integer.valueOf(frequencyMs));
            strengths.add(Integer.valueOf(strength));
        }
        return toFrames(frequencies, strengths);
    }

    public static List<String> swellPulse(int frequencyMs, int fromStrength, int toStrength, int samples) {
        List<Integer> frequencies = new ArrayList<Integer>();
        List<Integer> strengths = new ArrayList<Integer>();
        int safeSamples = Math.max(1, samples);
        for (int i = 0; i < safeSamples; i++) {
            double progress = safeSamples == 1 ? 1.0D : (double) i / (double) (safeSamples - 1);
            int strength = (int) Math.round(fromStrength + ((toStrength - fromStrength) * progress));
            frequencies.add(Integer.valueOf(frequencyMs));
            strengths.add(Integer.valueOf(strength));
        }
        return toFrames(frequencies, strengths);
    }

    public static List<String> warningPulse(int lowStrength, int highStrength, int repetitions) {
        List<Integer> frequencies = new ArrayList<Integer>();
        List<Integer> strengths = new ArrayList<Integer>();
        for (int i = 0; i < repetitions; i++) {
            addBurst(frequencies, strengths, 55, lowStrength, 2);
            addBurst(frequencies, strengths, 35, highStrength, 2);
            addBurst(frequencies, strengths, 80, 0, 2);
        }
        return toFrames(frequencies, strengths);
    }

    private static void addBurst(List<Integer> frequencies, List<Integer> strengths, int frequency, int strength, int count) {
        for (int i = 0; i < count; i++) {
            frequencies.add(Integer.valueOf(frequency));
            strengths.add(Integer.valueOf(strength));
        }
    }

    public static List<String> parseHexFrames(String rawInput) {
        String sanitized = rawInput.replace("[", " ")
            .replace("]", " ")
            .replace("\"", " ")
            .replace("'", " ")
            .replace("\r", "\n")
            .replace(",", "\n")
            .trim();
        String[] lines = sanitized.split("\n");
        List<String> frames = new ArrayList<String>();
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (!candidate.matches("(?i)^[0-9a-f]{16}$")) {
                throw new IllegalArgumentException("无效的 HEX 波形帧： " + candidate);
            }
            frames.add(candidate.toUpperCase(Locale.ROOT));
        }
        if (frames.isEmpty()) {
            throw new IllegalArgumentException("未找到任何 HEX 波形帧。");
        }
        return frames;
    }

    public static WaveformDefinition createWaveform(String name, String description, String sourceType, String sourceText, List<String> frames) {
        WaveformDefinition waveform = new WaveformDefinition();
        waveform.id = UUID.randomUUID().toString();
        waveform.name = name;
        waveform.description = description;
        waveform.sourceType = sourceType;
        waveform.sourceText = sourceText;
        waveform.frames = new ArrayList<String>(frames);
        waveform.estimatedDurationMs = frames.size() * 100L;
        return waveform;
    }

    public static int getOutputValue(int x) {
        if (x >= 10 && x <= 100) {
            return x;
        }
        if (x > 100 && x <= 600) {
            return Math.max(10, Math.min(240, Math.round(((x - 100) / 5.0F) + 100)));
        }
        if (x > 600 && x <= 1000) {
            return Math.max(10, Math.min(240, Math.round(((x - 600) / 10.0F) + 200)));
        }
        if (x < 10) {
            return 10;
        }
        return 240;
    }

    private static String hexByte(int value) {
        return String.format(Locale.ROOT, "%02X", Integer.valueOf(clamp(value, 0, 255)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

