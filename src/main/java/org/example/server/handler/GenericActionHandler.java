package org.example.server.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用操作处理器
 */
public class GenericActionHandler implements ActionHandler {
    private final Pattern pattern;
    private final String action;
    private final Function<Matcher, Map<String, Object>> extractor;

    /**
     * 构造函数
     *
     * @param regex     匹配的正则表达式
     * @param action    操作名称
     * @param extractor 参数提取函数
     */
    public GenericActionHandler(String regex, String action, Function<Matcher, Map<String, Object>> extractor) {
        this.pattern = Pattern.compile(regex);
        this.action = action;
        this.extractor = extractor;
    }

    /**
     * 判断一个测试步骤描述（如"点击登录按钮"）是否应由此处理器处理
     *
     * @param step 步骤
     * @return 是否匹配
     */
    @Override
    public boolean matches(String step) {
        return pattern.matcher(step).matches();
    }

    /**
     * 处理一个测试步骤描述，提取其中的参数
     *
     * @param matcher 匹配器
     * @return 参数
     */
    @Override
    public Map<String, Object> handle(Matcher matcher) {
        Map<String, Object> result = extractor.apply(matcher);  // 调用提取函数获取参数

        // 过滤掉所有 null 值
        result.entrySet().removeIf(entry -> entry.getValue() == null);

        // 添加非空的 action
        if (action != null) {
            result.put("action", action);
        }

        return result;
    }

    /**
     * 获取处理器的正则表达式
     *
     * @return 正则表达式
     */
    @Override
    public Pattern getPattern() {
        return pattern;
    }
}