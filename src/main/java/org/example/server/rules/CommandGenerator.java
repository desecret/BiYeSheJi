package org.example.server.rules;

import org.example.server.exception.RuleEngineException;
import org.example.server.locator.ElementLocatorFactory;
import org.example.server.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 创建一个通用的命令生成器，供规则和条件解析器共用
 */
public class CommandGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CommandGenerator.class);

    public static String generateCommand(Map<String, Object> parsed) {
        if (parsed == null || parsed.isEmpty()) {
            throw new RuleEngineException("无法生成命令：参数映射为空",
                    RuleEngineException.ErrorLevel.WARNING);
        }

        try {
            // 处理没有actionType的特殊情况（如访问网站）
            if (!parsed.containsKey("action")) {
                if (parsed.containsKey("element")) {
                    return "https://" + parsed.get("element");
                }
                throw new RuleEngineException("无法生成命令：参数中既没有action也没有element",
                        RuleEngineException.ErrorLevel.WARNING);
            }

            String actionType = (String) parsed.get("action");
            if (actionType == null) {
                throw new RuleEngineException("无法生成命令：action值为null",
                        RuleEngineException.ErrorLevel.WARNING);
            }

            switch (actionType) {
                case "click":
                case "rclick":
                case "dclick":
                case "hover":
                    String element = getRequiredElement(parsed, actionType);
                    return actionType + " " + element;

                case "type":
                    String typeElement = getRequiredElement(parsed, actionType);
                    Object valueObj = getRequiredValue(parsed, actionType);
                    return "type " + typeElement + " as " + valueObj;

                case "wait":
                case "timeout":
                case "echo":
                case "mouse":
                    Object value = getRequiredValue(parsed, actionType);
                    return actionType + " " + value;

                default:
                    throw new RuleEngineException("不支持的操作类型: " + actionType,
                            RuleEngineException.ErrorLevel.WARNING);
            }
        } catch (RuleEngineException e) {
            ErrorHandler.handle(e);
            return null;
        } catch (Exception e) {
            ErrorHandler.handle(new RuleEngineException("生成命令时发生错误: " + e.getMessage(), e));
            return null;
        }
    }

    /**
     * 获取并验证必需的元素
     */
    private static String getRequiredElement(Map<String, Object> parsed, String actionType) {
        Object elementObj = parsed.get("element");
        if (elementObj == null) {
            throw new RuleEngineException(actionType + "操作缺少必需的元素参数",
                    RuleEngineException.ErrorLevel.WARNING);
        }

        String elementName = elementObj.toString();
        String context = (String) parsed.get("context"); // 获取上下文信息
        try {
            // return ImageMapper.getImagePath(elementName);
            return ElementLocatorFactory.getLocator(elementName, context);
        } catch (Exception e) {
            throw new RuleEngineException("无法获取元素'" + elementName + "'的图像路径: " + e.getMessage(),
                    e, RuleEngineException.ErrorLevel.ERROR);
        }
    }

    /**
     * 获取并验证必需的值
     */
    private static Object getRequiredValue(Map<String, Object> parsed, String actionType) {
        Object valueObj = parsed.get("value");
        if (valueObj == null) {
            throw new RuleEngineException(actionType + "操作缺少必需的值参数",
                    RuleEngineException.ErrorLevel.WARNING);
        }
        return valueObj;
    }
}