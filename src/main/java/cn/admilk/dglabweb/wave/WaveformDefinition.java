package cn.admilk.dglabweb.wave;

import java.util.ArrayList;
import java.util.List;

public class WaveformDefinition {
    public String id;
    public String name;
    public String description;
    public String sourceType;
    public String sourceText;
    public List<String> frames = new ArrayList<String>();
    public long estimatedDurationMs;

    public WaveformDefinition copy() {
        WaveformDefinition copy = new WaveformDefinition();
        copy.id = this.id;
        copy.name = this.name;
        copy.description = this.description;
        copy.sourceType = this.sourceType;
        copy.sourceText = this.sourceText;
        copy.frames = new ArrayList<String>(this.frames);
        copy.estimatedDurationMs = this.estimatedDurationMs;
        return copy;
    }
}
