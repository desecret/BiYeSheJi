package org.example.server.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作处理器注册表
 */
public class ActionHandlerRegistry {
    public static final List<ActionHandler> handlers = new ArrayList<>();

    static {
        // 点击操作
        handlers.add(new GenericActionHandler(
                "点击(.*?)的(.*?)",
                "click",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("context", matcher.group(1));
                    result.put("element", matcher.group(2));
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "右键点击(.*?)的(.*?)",
                "rclick",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("context", matcher.group(1));
                    result.put("element", matcher.group(2));
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "双击(.*?)的(.*?)",
                "dclick",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("context", matcher.group(1));
                    result.put("element", matcher.group(2));
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "鼠标移动到(.*?)的(.*?)",
                "hover",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("context", matcher.group(1));
                    result.put("element", matcher.group(2));
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "在当前鼠标位置，按下鼠标左键",
                "mouse",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", "down");
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "在当前鼠标位置，弹起鼠标左键",
                "mouse",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", "up");
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "等待(.*?)秒",
                "wait",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", matcher.group(1));
                    return result;
                }));

        handlers.add(new GenericActionHandler(
                "设置自动等待(.*?)秒",
                "timeout",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", matcher.group(1));
                    return result;
                }));

        // 输入操作
        handlers.add(new GenericActionHandler(
                "在(.*?)的(.*?)输入\"(.*?)\"",
                "type",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("context", matcher.group(1));
                    result.put("element", matcher.group(2));
                    result.put("value", matcher.group(3));
                    return result;
                }));

        // 访问网站
        handlers.add((new GenericActionHandler(
                "访问(.*?)",
                "",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("element", matcher.group(1));
                    return result;
                })));

        handlers.add((new GenericActionHandler(
                "(.*?)出现",
                "exist",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    String front = "('";
                    result.put("element", front.concat(matcher.group(1)).concat("')"));
                    return result;
                })));

        handlers.add((new GenericActionHandler(
                "(.*?)存在",
                "present",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    String front = "('";
                    result.put("element", front.concat(matcher.group(1)).concat("')"));
                    return result;
                })));

        handlers.add((new GenericActionHandler(
                "输出\"(.*?)\"",
                "echo",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("value", matcher.group(1));
                    return result;
                })));

        // 条件语句处理器 - 通用型
        handlers.add(new GenericActionHandler(
                "如果(.*?)，(?:则|就|那么)?(.*)",
                "conditional",
                matcher -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("condition", matcher.group(1));
                    result.put("result", matcher.group(2));
                    return result;
                }));

    }
}