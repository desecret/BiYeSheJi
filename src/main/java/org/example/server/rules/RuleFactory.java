package org.example.server.rules;

import org.example.server.exception.RuleEngineException;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.core.BasicRule;

/**
 * 规则工厂
 */
public class RuleFactory {

    /**
     * 创建规则
     *
     * @param action     操作名称
     * @param ruleAction 规则动作
     * @return 规则
     */
    public static Rule createRule(String action, RuleAction ruleAction) {
        return new BasicRule(action + "Rule", "处理" + action + "操作") {
            @Override
            public boolean evaluate(org.jeasy.rules.api.Facts facts) {
                return action.equals(facts.get("action"));
            }

            @Override
            public void execute(org.jeasy.rules.api.Facts facts) throws RuleEngineException {
                try {
                    ruleAction.execute(facts);
                } catch (Exception e) {
                    throw new RuleEngineException("执行规则失败", e);
                }
            }
        };
    }

    /**
     * 规则动作接口，函数式接口，createRule的第二个参数接受到lambda表达式时，会被转换为该接口的实现
     */
    public interface RuleAction {
        void execute(org.jeasy.rules.api.Facts facts) throws Exception;
    }
}