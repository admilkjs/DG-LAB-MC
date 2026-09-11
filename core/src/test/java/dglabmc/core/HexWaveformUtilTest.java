package dglabmc.core;

import dglabmc.core.util.HexWaveformUtil;
import dglabmc.core.wave.WaveformDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HexWaveformUtilTest {
    @Test
    void parsesAndNormalizesHexFrames() {
        List<String> frames = HexWaveformUtil.parseHexFrames("[\"0a0b0c0d0e0f1011\",\n 1213141516171819]");
        assertEquals(Arrays.asList("0A0B0C0D0E0F1011", "1213141516171819"), frames);
    }

    @Test
    void createsIndependentWaveformCopy() {
        WaveformDefinition original = HexWaveformUtil.createWaveform("x", "d", "hex", "", Arrays.asList("0011223344556677"));
        WaveformDefinition copy = original.copy();
        copy.frames.add("8899AABBCCDDEEFF");
        assertEquals(1, original.frames.size());
        assertEquals(2, copy.frames.size());
    }
}
