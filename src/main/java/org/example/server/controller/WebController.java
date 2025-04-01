package org.example.server.controller;

import org.example.server.Init;
import org.example.server.config.ElementConfig;
import org.example.server.config.ElementMappings;
import org.example.server.rules.CommandGenerator;
import org.example.server.rules.RulesRegistry;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Web控制器，提供自动化测试系统的API接口
 * <p>
 * 该控制器提供了测试用例管理、元素库访问和测试用例执行等功能的RESTful接口
 * </p>
 */
@RestController
@RequestMapping("/api")
public class WebController {

    /** YAML元素映射文件路径 */
    private static final String YAML_FILE = "src/main/resources/element-mappings.yaml";
    /** 测试用例存储目录路径 */
    private static final String CASES_DIR = "src/main/java/org/example/server/cases/";

    /**
     * 构造函数
     * 
     * @param init 初始化对象，用于系统启动
     */
    public WebController(Init init) {
    }

    /**
     * 获取所有测试用例列表
     * <p>
     * 该接口会扫描测试用例目录下的所有.txt文件，返回测试用例ID和名称
     * </p>
     * 
     * @return 测试用例列表，包含id和name属性
     */
    @GetMapping("/testcases")
    public List<Map<String, Object>> getTestCases() {
        List<Map<String, Object>> testCases = new ArrayList<>();
        File casesDir = new File(CASES_DIR);
        if (casesDir.exists() && casesDir.isDirectory()) {
            File[] files = casesDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File file : files) {
                    Map<String, Object> testCase = new HashMap<>();
                    testCase.put("id", file.getName());
                    testCase.put("name", file.getName().replace(".txt", ""));
                    testCases.add(testCase);
                }
            }
        }
        return testCases;
    }

    /**
     * 获取指定测试用例的详细内容
     * <p>
     * 读取指定测试用例文件并解析其中的命令步骤，返回结构化的步骤列表
     * </p>
     * 
     * @param id 测试用例ID（文件名）
     * @return 包含测试用例步骤和状态的Map，steps包含步骤列表，success表示是否成功
     */
    @GetMapping("/testcases/{id}")
    public Map<String, Object> getTestCaseContent(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String filePath = CASES_DIR + id;
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            List<Map<String, Object>> steps = new ArrayList<>();

            // 解析测试用例内容
            String[] lines = content.split("\n");
            for (String line : lines) {
                Map<String, Object> step = new HashMap<>();
                if (line.startsWith("点击")) {
                    step.put("type", "点击");
                    step.put("content", line.substring(2));
                } else if (line.startsWith("右键点击")) {
                    step.put("type", "右键点击");
                    step.put("content", line.substring(4));
                } else if (line.startsWith("双击")) {
                    step.put("type", "双击");
                    step.put("content", line.substring(2));
                } else if (line.startsWith("鼠标移动到")) {
                    step.put("type", "鼠标移动到");
                    step.put("content", line.substring(5));
                } else if (line.startsWith("在当前鼠标位置，按下鼠标左键")) {
                    step.put("type", "在当前鼠标位置，按下鼠标左键");
                    step.put("content", "");
                } else if (line.startsWith("在当前鼠标位置，弹起鼠标左键")) {
                    step.put("type", "在当前鼠标位置，弹起鼠标左键");
                    step.put("content", "");
                } else if (line.startsWith("等待")) {
                    step.put("type", "等待");
                    // 提取数字部分
                    String timeStr = line.substring(2).replaceAll("[^0-9]", "");
                    step.put("content", timeStr);
                } else if (line.startsWith("设置自动等待")) {
                    step.put("type", "设置自动等待");
                    // 提取数字部分
                    String timeStr = line.substring(6).replaceAll("[^0-9]", "");
                    step.put("content", timeStr);
                } else if (line.startsWith("在")) {
                    step.put("type", "在");
                    // 保留完整内容
                    step.put("content", line.substring(1));
                } else if (line.startsWith("访问")) {
                    step.put("type", "访问");
                    // 保留完整URL，包括第一个字母
                    step.put("content", line.substring(2));
                } else if (line.endsWith("出现")) {
                    step.put("type", "出现");
                    step.put("content", line.substring(0, line.length() - 2));
                } else if (line.endsWith("存在")) {
                    step.put("type", "存在");
                    step.put("content", line.substring(0, line.length() - 2));
                } else if (line.startsWith("输出")) {
                    step.put("type", "输出");
                    // 提取引号中的内容
                    String outputContent = line.substring(3);
                    if (outputContent.startsWith("\"") && outputContent.endsWith("\"")) {
                        outputContent = outputContent.substring(1, outputContent.length() - 1);
                    }
                    step.put("content", outputContent);
                } else if (line.startsWith("如果")) {
                    step.put("type", "如果");
                    step.put("content", line.substring(2));
                }
                if (!step.isEmpty()) {
                    steps.add(step);
                }
            }

            response.put("steps", steps);
            response.put("success", true);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", "无法读取测试用例内容：" + e.getMessage());
        }
        return response;
    }

    /**
     * 获取UI元素库
     * <p>
     * 从YAML配置文件中读取元素映射配置，返回所有UI元素的列表
     * </p>
     * 
     * @return UI元素配置列表
     */
    @GetMapping("/elements")
    public List<ElementConfig> getElements() {
        try {
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);

            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));
            if (mappings != null && mappings.getConfigs() != null) {
                return mappings.getConfigs();
            }
            return new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 获取所有可用的动作类型列表
     * 
     * @return 动作类型列表
     */
    @GetMapping("/actions")
    public List<String> getActions() {
        List<String> actions = new ArrayList<>();
        actions.add("点击");
        actions.add("右键点击");
        actions.add("双击");
        actions.add("鼠标移动到");
        actions.add("在当前鼠标位置，按下鼠标左键");
        actions.add("在当前鼠标位置，弹起鼠标左键");
        actions.add("等待");
        actions.add("设置自动等待");
        actions.add("在");
        actions.add("访问");
        actions.add("出现");
        actions.add("存在");
        actions.add("输出");
        actions.add("如果");
        return actions;
    }

    /**
     * 获取按context分组的元素列表
     * 
     * @return 按context分组的元素列表
     */
    @GetMapping("/elements/grouped")
    public Map<String, List<ElementConfig>> getGroupedElements() {
        try {
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);

            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));
            if (mappings != null && mappings.getConfigs() != null) {
                Map<String, List<ElementConfig>> groupedElements = new HashMap<>();
                for (ElementConfig element : mappings.getConfigs()) {
                    groupedElements.computeIfAbsent(element.getContext(), k -> new ArrayList<>()).add(element);
                }
                return groupedElements;
            }
            return new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * 保存测试用例
     * <p>
     * 将前端提交的测试步骤保存为标准格式的测试用例文件
     * </p>
     * 
     * @param request 包含测试用例名称和步骤的请求体
     */
    @PostMapping("/testcases")
    public void saveTestCase(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        List<Map<String, Object>> steps = (List<Map<String, Object>>) request.get("steps");

        StringBuilder content = new StringBuilder();
        for (Map<String, Object> step : steps) {
            String type = (String) step.get("type");
            String contentStr = (String) step.get("content");

            switch (type) {
                case "点击":
                    content.append("点击").append(contentStr).append("\n");
                    break;
                case "右键点击":
                    content.append("右键点击").append(contentStr).append("\n");
                    break;
                case "双击":
                    content.append("双击").append(contentStr).append("\n");
                    break;
                case "鼠标移动到":
                    content.append("鼠标移动到").append(contentStr).append("\n");
                    break;
                case "在当前鼠标位置，按下鼠标左键":
                    content.append("在当前鼠标位置，按下鼠标左键\n");
                    break;
                case "在当前鼠标位置，弹起鼠标左键":
                    content.append("在当前鼠标位置，弹起鼠标左键\n");
                    break;
                case "等待":
                    content.append("等待").append(contentStr).append("秒\n");
                    break;
                case "设置自动等待":
                    content.append("设置自动等待").append(contentStr).append("秒\n");
                    break;
                case "在":
                    content.append("在").append(contentStr).append("\n");
                    break;
                case "访问":
                    content.append("访问").append(contentStr).append("\n");
                    break;
                case "出现":
                    content.append(contentStr).append("出现\n");
                    break;
                case "存在":
                    content.append(contentStr).append("存在\n");
                    break;
                case "输出":
                    content.append("输出\"").append(contentStr).append("\"\n");
                    break;
                case "如果":
                    content.append("如果").append(contentStr).append("\n");
                    break;
            }
        }

        try {
            File file = new File(CASES_DIR + name + ".txt");
            java.nio.file.Files.write(file.toPath(), content.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 执行测试用例
     * <p>
     * 接收前端提交的测试步骤，将其转换为标准命令格式并执行，返回执行结果日志
     * </p>
     * 
     * @param request 包含测试步骤的请求体
     * @return 测试执行日志
     */
    @PostMapping("/run")
    public Map<String, Object> runTestCase(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> steps = (List<Map<String, Object>>) request.get("steps");
        List<String> logs = new ArrayList<>();
        StringBuilder testCases = new StringBuilder();

        for (Map<String, Object> step : steps) {
            String type = (String) step.get("type");
            String content = (String) step.get("content");

            testCases.append(type).append(content).append("\n");
        }
        try {
            List<String> commands = Init.runTestCase(testCases.toString());
            if (commands != null) {
                logs.add("执行命令: " + commands);
            }
        } catch (Exception e) {
            logs.add("错误: " + e.getMessage());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("logs", logs);
        return result;
    }
}