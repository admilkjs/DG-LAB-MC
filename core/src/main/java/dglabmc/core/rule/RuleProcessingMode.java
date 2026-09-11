package dglabmc.core.rule;

public enum RuleProcessingMode {
    PARALLEL("并行", "同一事件可触发多条"),
    SEQUENTIAL_FIRST_MATCH("按顺序", "从上到下只触发第一条");

    private final String label;
    private final String description;

    RuleProcessingMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return this.label;
    }

    public String description() {
        return this.description;
    }
}

