package dglabmc.rule;

import java.util.LinkedHashMap;
import java.util.Map;

public class RuleDefinition {
    public String id;
    public String name = "";
    public boolean enabled = true;
    public int orderGroup = 0;
    public String trigger;
    public ChannelTarget channel = ChannelTarget.A;
    public String waveformId;
    public StrengthAction strengthAction = StrengthAction.INCREASE;
    public double strengthScale = 1.0D;
    public IntensityMode intensityMode = IntensityMode.FIXED;
    public boolean useGlobalStrength = true;
    public int baseStrength = 20;
    public int maxStrength = 60;
    public boolean clearBeforeSend = true;
    public long cooldownMs = 1000L;
    public Map<String, Double> conditions = new LinkedHashMap<String, Double>();
}
