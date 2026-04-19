package dglabmc.wave;

import dglabmc.util.HexWaveformUtil;

import java.util.ArrayList;
import java.util.List;

public class DungeonlabWaveformParser {
    private static final int[] FREQUENCY_DATASET = new int[] {
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
        52, 54, 56, 58, 60, 62, 64, 66, 68, 70,
        72, 74, 76, 78, 80, 85, 90, 95, 100, 110,
        120, 130, 140, 150, 160, 170, 180, 190, 200, 233,
        266, 300, 333, 366, 400, 450, 500, 550, 600, 700,
        800, 900, 1000
    };

    public ParsedWaveform parse(String rawData, String name) {
        if (rawData == null || !rawData.startsWith("Dungeonlab+pulse:")) {
            throw new IllegalArgumentException("波形文本必须以 Dungeonlab+pulse: 开头。");
        }

        String cleanData = rawData.replaceFirst("(?i)^Dungeonlab\\+pulse:", "");
        String[] sectionParts = cleanData.split("\\+section\\+");
        if (sectionParts.length == 0 || sectionParts[0].trim().isEmpty()) {
            throw new IllegalArgumentException("未找到可解析的波形分段。");
        }

        String firstPart = sectionParts[0];
        int equalIndex = firstPart.indexOf('=');
        if (equalIndex < 0) {
            throw new IllegalArgumentException("波形文本缺少全局设置分隔符 '='。");
        }

        ParsedWaveform waveform = new ParsedWaveform();
        waveform.name = name;
        String[] settings = firstPart.substring(0, equalIndex).split(",");
        waveform.globalSettings = new GlobalSettings(
            numberAt(settings, 0, 0),
            numberAt(settings, 1, 1),
            numberAt(settings, 2, 8)
        );

        List<String> allSections = new ArrayList<String>();
        allSections.add(firstPart.substring(equalIndex + 1));
        for (int i = 1; i < sectionParts.length; i++) {
            allSections.add(sectionParts[i]);
        }

        for (int i = 0; i < allSections.size() && i < 10; i++) {
            Section parsedSection = parseSection(i, allSections.get(i));
            waveform.sections.add(parsedSection);
        }
        if (waveform.sections.isEmpty()) {
            throw new IllegalArgumentException("波形中没有启用的分段。");
        }

        waveform.hexWaveforms.addAll(convertSections(waveform.sections));
        return waveform;
    }

    private Section parseSection(int index, String rawSection) {
        int slashIndex = rawSection.indexOf('/');
        if (slashIndex < 0) {
            throw new IllegalArgumentException("第 " + (index + 1) + " 段缺少 '/' 分隔符。");
        }

        String[] header = rawSection.substring(0, slashIndex).split(",");
        boolean enabled = numberAt(header, 4, 1) != 0;
        if (!enabled) {
            return null;
        }

        Section section = new Section();
        section.index = index;
        section.enabled = true;
        section.startFrequencyIndex = numberAt(header, 0, 0);
        section.endFrequencyIndex = numberAt(header, 1, 0);
        section.durationIndex = numberAt(header, 2, 0);
        section.frequencyMode = numberAt(header, 3, 1);
        section.startFrequency = getFrequencyFromIndex(section.startFrequencyIndex);
        section.endFrequency = getFrequencyFromIndex(section.endFrequencyIndex);
        section.duration = getDurationFromIndex(section.durationIndex);

        String[] shapeItems = rawSection.substring(slashIndex + 1).split(",");
        for (String item : shapeItems) {
            String candidate = item.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            String[] parts = candidate.split("-");
            ShapePoint point = new ShapePoint();
            point.strength = clamp(parseShapeStrength(parts, 0, 0.0D), 0, 100);
            point.anchor = numberAt(parts, 1, 0) == 1;
            section.shape.add(point);
        }

        if (section.shape.size() < 2) {
            throw new IllegalArgumentException("第 " + (index + 1) + " 段至少需要两个波形点。");
        }
        return section;
    }

    private List<String> convertSections(List<Section> sections) {
        List<String> output = new ArrayList<String>();
        for (Section section : sections) {
            if (section == null || section.shape.isEmpty()) {
                continue;
            }
            int shapeCount = section.shape.size();
            int pulseElementCount = Math.max(1, (int) Math.ceil((double) section.duration / (double) shapeCount));
            int actualDuration = pulseElementCount * shapeCount;
            List<Integer> frequencySamples = new ArrayList<Integer>();
            List<Integer> strengthSamples = new ArrayList<Integer>();

            for (int elementIndex = 0; elementIndex < pulseElementCount; elementIndex++) {
                for (int shapeIndex = 0; shapeIndex < shapeCount; shapeIndex++) {
                    ShapePoint point = section.shape.get(shapeIndex);
                    double sectionProgress = (double) (elementIndex * shapeCount + shapeIndex) / (double) actualDuration;
                    double elementProgress = (double) shapeIndex / (double) shapeCount;
                    int outputFrequency = resolveOutputFrequency(section, elementIndex, pulseElementCount, sectionProgress, elementProgress);

                    for (int n = 0; n < 4; n++) {
                        frequencySamples.add(Integer.valueOf(outputFrequency));
                        strengthSamples.add(Integer.valueOf(point.strength));
                    }
                }
            }
            output.addAll(HexWaveformUtil.toFrames(frequencySamples, strengthSamples));
        }
        return output;
    }

    private int resolveOutputFrequency(Section section, int elementIndex, int pulseElementCount, double sectionProgress, double elementProgress) {
        switch (section.frequencyMode) {
            case 2:
                return HexWaveformUtil.getOutputValue((int) Math.round(section.startFrequency + ((section.endFrequency - section.startFrequency) * sectionProgress)));
            case 3:
                return HexWaveformUtil.getOutputValue((int) Math.round(section.startFrequency + ((section.endFrequency - section.startFrequency) * elementProgress)));
            case 4:
                double stepProgress = pulseElementCount <= 1 ? 0.0D : (double) elementIndex / (double) (pulseElementCount - 1);
                return HexWaveformUtil.getOutputValue((int) Math.round(section.startFrequency + ((section.endFrequency - section.startFrequency) * stepProgress)));
            case 1:
            default:
                return HexWaveformUtil.getOutputValue(section.startFrequency);
        }
    }

    private int getFrequencyFromIndex(int index) {
        int safeIndex = clamp(index, 0, FREQUENCY_DATASET.length - 1);
        return FREQUENCY_DATASET[safeIndex];
    }

    private int getDurationFromIndex(int index) {
        return clamp(index, 0, 99) + 1;
    }

    private int numberAt(String[] values, int index, int fallback) {
        if (index < 0 || index >= values.length) {
            return fallback;
        }
        try {
            return Integer.parseInt(values[index].trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseShapeStrength(String[] values, int index, double fallback) {
        if (index < 0 || index >= values.length) {
            return (int) Math.round(fallback);
        }
        try {
            return (int) Math.round(Double.parseDouble(values[index].trim()));
        } catch (NumberFormatException ignored) {
            return (int) Math.round(fallback);
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class ParsedWaveform {
        public String name;
        public GlobalSettings globalSettings;
        public final List<Section> sections = new ArrayList<Section>();
        public final List<String> hexWaveforms = new ArrayList<String>();
    }

    public static class GlobalSettings {
        public final int sectionRestTime;
        public final int playbackSpeed;
        public final int frequencyBalance;

        public GlobalSettings(int sectionRestTime, int playbackSpeed, int frequencyBalance) {
            this.sectionRestTime = sectionRestTime;
            this.playbackSpeed = playbackSpeed;
            this.frequencyBalance = frequencyBalance;
        }
    }

    public static class Section {
        public int index;
        public boolean enabled;
        public int startFrequencyIndex;
        public int endFrequencyIndex;
        public int durationIndex;
        public int frequencyMode;
        public int startFrequency;
        public int endFrequency;
        public int duration;
        public final List<ShapePoint> shape = new ArrayList<ShapePoint>();
    }

    public static class ShapePoint {
        public int strength;
        public boolean anchor;
    }
}
