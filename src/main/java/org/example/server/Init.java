package org.example.server;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.api.RulesEngineParameters;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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


    public static List<String> runTestCase(String testCase) {
        if (testCase.trim().isEmpty()) {
            throw new RuleEngineException("测试用例为空",
                    RuleEngineException.ErrorLevel.ERROR);
        }

        // 2. 创建规则引擎
        RulesEngineParameters params = new RulesEngineParameters()
                .skipOnFirstAppliedRule(true);
        RulesEngine engine = new DefaultRulesEngine(params);

        // 3. 注册规则
        Rules rules = RulesRegistry.registerRules();

        // 4. 清除之前可能存在的命令
        RulesRegistry.clearCommands();

        // 5. 处理每个步骤
        String[] steps = testCase.split("\n");
        for (int i = 0; i < steps.length; i++) {
            String step = steps[i];
            try {
                String[] parts = step.split("\\. ", 2);
//                if (parts.length < 2) {
//                    ErrorHandler.logWarning("无法解析步骤 #" + (i + 1) + ": " + step);
//                    continue;
//                }

                String stepContent = parts[0];
                Map<String, Object> parsed = NlpParser.parseStep(stepContent);
                if (parsed.isEmpty()) {
                    ErrorHandler.logWarning("无法解析步骤内容 #" + (i + 1) + ": " + stepContent);
                    continue;
                }

                Facts facts = new Facts();
                facts.put("action", parsed.get("action"));
                // facts.put("locatorType", parsed.getOrDefault("locatorType", null)); //
                // 用户指定的定位方式
                putToFact(facts, parsed, Arrays.asList("element", "value", "context", "condition", "result"));

                engine.fire(rules, facts);
            } catch (Exception e) {
                ErrorHandler.logError("处理步骤 #" + (i + 1) + " 时发生错误: " + e.getMessage(), e);
            }
        }

        // 6. 保存生成的命令到文件
        RulesRegistry.saveCommandsToFile(outputPath);

        return RulesRegistry.getCommands();
    }

    private static void putToFact(Facts facts, Map<String, Object> parsed, List<String> keys) {
        for (String key : keys) {
            if (parsed.containsKey(key))
                facts.put(key, parsed.get(key));
        }

    }
}
