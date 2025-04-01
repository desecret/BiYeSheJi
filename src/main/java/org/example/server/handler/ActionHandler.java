package org.example.server.handler;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理步骤的内容
 */
public interface ActionHandler {
    /**
     * 检查step是否与正则表达式匹配
     * @param step 检查的表达式
     * @return 是否匹配
     */
    boolean matches(String step);

    /**
     * 处理匹配的表达式
     * @param matcher 匹配的结果
     * @return 处理结果
     */
    Map<String, Object> handle(Matcher matcher);

    /**
     * 获取正则表达式
     * @return 正则表达式
     */
    Pattern getPattern();
}
