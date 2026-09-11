package dglabmc.core.rule;

import java.util.List;

/** Optional diagnostics emitted by the rule engine. Platform adapters may render them. */
public interface RuleFeedback {
    RuleFeedback NOOP = new RuleFeedback() {
        @Override
        public void reportRuleRowContinue(int rowIndex) {
        }

        @Override
        public void reportRuleRowMatched(int rowIndex, List<String> ruleNames) {
        }

        @Override
        public void reportRuleStop(int rowIndex) {
        }

        @Override
        public void reportRuleNoMatch() {
        }
    };

    void reportRuleRowContinue(int rowIndex);

    void reportRuleRowMatched(int rowIndex, List<String> ruleNames);

    void reportRuleStop(int rowIndex);

    void reportRuleNoMatch();
}
