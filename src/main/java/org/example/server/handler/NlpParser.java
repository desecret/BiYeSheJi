package org.example.server.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

import static org.example.server.handler.ActionHandlerRegistry.handlers;


public class NlpParser {
    /**
     * 解析step
     * @param step 步骤的内容
     * @return 解析结果
     */
    public static Map<String, Object> parseStep(String step) {
        for (ActionHandler handler : handlers) {
            // 如果匹配到了对应的handler
            if (handler.matches(step)) {
                // 处理匹配的表达式
                Matcher matcher = handler.getPattern().matcher(step);
                // 如果匹配到了
                if (matcher.matches()) {
                    // 返回处理结果
                    return handler.handle(matcher);
                }
            }
        }
        return new HashMap<>();
    }
}