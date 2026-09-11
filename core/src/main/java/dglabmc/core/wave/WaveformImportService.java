package dglabmc.core.wave;

import dglabmc.core.util.HexWaveformUtil;

import java.util.List;

public class WaveformImportService {
    private final DungeonlabWaveformParser parser = new DungeonlabWaveformParser();

    public WaveformDefinition importPulse(String name, String description, String sourceText) {
        DungeonlabWaveformParser.ParsedWaveform parsed = parser.parse(sourceText, name);
        return HexWaveformUtil.createWaveform(name, description, "pulse_text", sourceText, parsed.hexWaveforms);
    }

    public WaveformDefinition importHex(String name, String description, String sourceText) {
        List<String> frames = HexWaveformUtil.parseHexFrames(sourceText);
        return HexWaveformUtil.createWaveform(name, description, "hex_frames", sourceText, frames);
    }
}

