package org.example.server;

import org.example.server.util.util;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.example.server.exception.RuleEngineException;
import org.example.server.handler.NlpParser;
import org.example.server.locator.ElementLocatorFactory;
import org.example.server.rules.RulesRegistry;
import org.example.server.util.ErrorHandler;
import org.example.server.util.logConfig;

import static org.example.server.util.httpRequest.uploadImagesAndProcessResponse;
import static org.example.server.util.util.printSupportedCommands;
import static org.example.server.util.util.writeToYaml;
import static org.example.server.staticString.*;

/**
 * 初始化类，用于启动时加载配置
 */
@Component
public class Init implements ApplicationRunner {

//    @Override
//    public void run(ApplicationArguments args) throws Exception {
//        writeToYaml();
//    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        init();
        printSupportedCommands();

        writeToYaml();
        ElementLocatorFactory.init();
//        System.out.println(util.runTaguiCommand());
    }

    private void init() {
        // 配置日志级别
        logConfig.configureLogging();

        printSupportedCommands();

        try {
            // uploadImagesAndProcessResponse(url, uploadImages);

//            writeToYaml();
//            ElementLocatorFactory.init();

            // 1. 从文件加载测试用例
            String testCase;
            try {
                testCase = String.join("\n", Files.readAllLines(Paths.get(casePath)));
            } catch (IOException e) {
                throw new RuleEngineException("读取测试用例文件出错: " + e.getMessage(), e,
                        RuleEngineException.ErrorLevel.FATAL);
            }

            runTestCase(testCase);

        } catch (RuleEngineException e) {
            ErrorHandler.handle(e);
            System.exit(1);
        } catch (Exception e) {
            ErrorHandler.handle(e);
            System.exit(2);
        }
    }


    public static List<String> runTestCase(String testCase) throws RuleEngineException {
        List<String> logs = new ArrayList<>();

        if (testCase.trim().isEmpty()) {
            throw new RuleEngineException("测试用例为空", RuleEngineException.ErrorLevel.ERROR);
        }

        try {
            // 2. 创建规则引擎
            RulesEngineParameters params = new RulesEngineParameters()
                    .skipOnFirstAppliedRule(true);
            RulesEngine engine = new DefaultRulesEngine(params);

            // 3. 注册规则
            Rules rules = RulesRegistry.registerRules();

            // 4. 清除之前可能存在的命令
            RulesRegistry.clearCommands();
            logs.add("初始化规则引擎完成");

            // 5. 处理每个步骤
            String[] steps = testCase.split("\n");
            logs.add("开始处理测试步骤，共 " + steps.length + " 个步骤");

            for (int i = 0; i < steps.length; i++) {
                String step = steps[i];
                try {
                    logs.add("处理步骤 #" + (i + 1) + ": " + step);

                    String[] parts = step.split("\\. ", 2);
                    String stepContent = parts.length > 1 ? parts[1] : step;

                    Map<String, Object> parsed = NlpParser.parseStep(stepContent);
                    if (parsed.isEmpty()) {
                        String warning = "无法解析步骤内容 #" + (i + 1) + ": " + stepContent;
                        logs.add("警告: " + warning);
                        ErrorHandler.logWarning(warning);
                        continue;
                    }

                    Facts facts = new Facts();
                    facts.put("action", parsed.get("action"));
                    putToFact(facts, parsed, Arrays.asList("element", "value", "context", "condition", "result"));

                    engine.fire(rules, facts);
                    logs.add("步骤 #" + (i + 1) + " 执行完成");
                } catch (Exception e) {
                    String errorMsg = "处理步骤 #" + (i + 1) + " 时发生错误: " + e.getMessage();
                    logs.add("错误: " + errorMsg);
                    ErrorHandler.logError(errorMsg, e);

                    // 捕获内部异常但不立即终止，继续处理其他步骤
                    // 但记录下详细错误信息
                    logs.add("详细错误信息: " + e.toString());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logs.add("堆栈: " + sw.toString());
                }
            }

            // 6. 保存生成的命令到文件
            RulesRegistry.saveCommandsToFile(outputPath);
            logs.add("测试执行完成，命令已保存");

            // 添加生成的命令到日志
            logs.add("执行命令: " + RulesRegistry.getCommands());

            return logs;
        } catch (Exception e) {
            logs.add("执行错误: " + e.getMessage());
            // 记录详细的错误堆栈
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logs.add("堆栈: " + sw.toString());

            // 将捕获的异常包装为 RuleEngineException 重新抛出
            throw new RuleEngineException("执行测试用例时发生错误: " + e.getMessage(),
                    e,
                    RuleEngineException.ErrorLevel.ERROR);
        }
    }

    private static void putToFact(Facts facts, Map<String, Object> parsed, List<String> keys) {
        for (String key : keys) {
            if (parsed.containsKey(key))
                facts.put(key, parsed.get(key));
        }

    }
}
