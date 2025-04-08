package org.example.server.rules;

import org.example.server.exception.RuleEngineException;
import org.example.server.handler.NlpParser;
import org.example.server.locator.ElementLocatorFactory;
import org.example.server.util.ErrorHandler;
import org.jeasy.rules.api.Rules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RulesRegistry {
    private static final List<String> taguiCommands = new ArrayList<>();

    public static Rules registerRules() throws RuleEngineException {
        Rules rules = new Rules();

        try{// 注册点击规则
            rules.register(RuleFactory.createRule("click", facts -> {
                String command = getCommand("click", facts.get("element"), null, facts.get("context"));

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("rclick", facts -> {
                String command = getCommand("rclick", facts.get("element"), null, facts.get("context"));

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("dclick", facts -> {
                String command = getCommand("dclick", facts.get("element"), null, facts.get("context"));

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("hover", facts -> {
                String command = getCommand("hover", facts.get("element"), null, facts.get("context"));

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("mouse", facts -> {
                String command = getCommand("mouse", null, facts.get("value"), null);

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("wait", facts -> {
                String command = getCommand("wait", null, facts.get("value"), null);

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            rules.register(RuleFactory.createRule("timeout", facts -> {
                String command = getCommand("timeout", null, facts.get("value"), null);

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            // 注册输入规则
            rules.register(RuleFactory.createRule("type", facts -> {
                String command = getCommand("type", facts.get("element"), facts.get("value"), facts.get("context"));

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            // 访问网站
            rules.register((RuleFactory.createRule("", facts -> {
                Map<String, Object> params = new HashMap<>();
                String element = facts.get("element");
                params.put("element", element);

                if (element != null) {
                    String command = CommandGenerator.generateCommand(params);
                    System.out.println(command); // 保留控制台输出
                    taguiCommands.add(command); // 添加到命令列表
                }
            })));

            rules.register(RuleFactory.createRule("echo", facts -> {
                String command = getCommand("echo", null, facts.get("value"), null);

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));

            // 注册通用条件规则
            rules.register(RuleFactory.createRule("conditional", facts -> {
                String condition = facts.get("condition");
                String result = facts.get("result");

                // 这里需要进一步解析条件和动作，递归处理
                // 简化实现，实际可能需要更复杂的解析逻辑
                String command = "if " + parseCondition(condition) + "\n    " + parseAction(result);

                System.out.println(command);
                taguiCommands.add(command);
            }));

            rules.register(RuleFactory.createRule("fullscreen", facts -> {
                String command = "keyboard [alt][space]\n" +
                        "keyboard x\n";

                System.out.println(command); // 保留控制台输出
                taguiCommands.add(command); // 添加到命令列表
            }));
        }
        catch (Exception e) {
            ErrorHandler.logError("注册规则时发生错误: " + e.getMessage(), e);
            throw new RuleEngineException("注册规则时发生错误: " + e.getMessage());
        }

        return rules;
    }

    /**
     * 将收集的TagUI命令保存到文件
     * 
     * @param filePath 文件路径
     */
    public static List<String> saveCommandsToFile(String filePath) {
        try {
            // 确保目录存在
            File file = new File(filePath);
            File directory = file.getParentFile();

            if (directory != null && !directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    System.err.println("无法创建目录: " + directory.getAbsolutePath());
                    return new ArrayList<>();
                }
            }

            // 写入文件,false表示写入文件时不追加内容，即先清空再写入
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                //把CMD 设置成utf-8
                writer.write("run cmd /c chcp 65001\n");
                writer.write("keyboard [alt][space]\n" +
                        "keyboard x\n");
                for (String command : taguiCommands) {
                    writer.write(command);
                    writer.newLine();
                }
                System.out.println("TagUI命令已保存到: " + filePath);
                return taguiCommands;
            }
        } catch (IOException e) {
            System.err.println("保存TagUI命令时出错: " + e.getMessage());
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * 清除已收集的命令
     */
    public static void clearCommands() {
        taguiCommands.clear();
    }

    public static String getCommand(String action, String element, String value, String context) {
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);
        if (element != null) {
            params.put("element", element);
        }
        if (value != null) {
            params.put("value", value);
        }
        if (context != null) {
            params.put("context", context);
        }

        return CommandGenerator.generateCommand(params);
    }

    public static List<String> getCommands() {
        return taguiCommands;
    }

    /**
     * 解析条件表达式
     */
    private static String parseCondition(String condition) throws RuleEngineException {
        // 处理常见条件模式
        if (condition.matches(".*存在.*")) {
            String[] parts = condition.replaceAll("(.*?)的(.*?)存在.*", "$1,$2").split(",", 2);
            String context = parts.length > 0 ? parts[0] : "";
            String element = parts.length > 1 ? parts[1] : "";
            String result;
            try {
                result = "present(\"" + ElementLocatorFactory.getLocator(element, context) + "\")";
            } catch (Exception e) {
                ErrorHandler.logError("解析条件时发生错误: " + e.getMessage(), e);
                throw new RuleEngineException("解析条件时发生错误: " + e.getMessage());
            }
            return result;

        } else if (condition.matches(".*出现.*")) {
            String[] parts = condition.replaceAll("(.*?)的(.*?)出现", "$1,$2").split(",", 2);
            String context = parts.length > 0 ? parts[0] : "";
            String element = parts.length > 1 ? parts[1] : "";
            String result;
            try {
                result = "exist(\"" + ElementLocatorFactory.getLocator(element, context) + "\")";
            } catch (Exception e) {
                ErrorHandler.logError("解析条件时发生错误: " + e.getMessage(), e);
                throw new RuleEngineException("解析条件时发生错误: " + e.getMessage());
            }
            return result;
        }
        // 可以添加更多条件类型处理
        return condition; // 简单处理，实际应用需更复杂的解析
    }

    /**
     * 解析动作表达式
     */
    private static String parseAction(String action) {
        if (action == null || action.trim().isEmpty()) {
            ErrorHandler.logWarning("动作表达式为空");
            return "";
        }

        try {
            // 解析步骤
            Map<String, Object> parsed = NlpParser.parseStep(action);
            if (parsed.isEmpty()) {
                ErrorHandler.logWarning("无法解析动作: " + action);
                return action; // 保留原始文本，以便调试
            }

            // 使用通用命令生成器生成命令
            String command = CommandGenerator.generateCommand(parsed);
            if (command == null) {
                ErrorHandler.logWarning("无法生成命令，使用原始动作文本: " + action);
                return action;
            }

            return command;
        } catch (Exception e) {
            ErrorHandler.logError("解析动作时发生错误: " + e.getMessage(), e);
            return action; // 出错时返回原始文本
        }
    }
}