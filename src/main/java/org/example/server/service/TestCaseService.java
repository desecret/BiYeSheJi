package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.TestCaseEntity;
import org.example.server.entity.TestCaseStepEntity;
import org.example.server.repository.TestCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.example.server.staticString.CASES_DIR;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;

    /**
     * 获取所有测试用例
     *
     * @return 测试用例列表
     */
    public List<Map<String, Object>> getAllTestCases() {
        return testCaseRepository.findAll().stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定测试用例
     *
     * @param id 测试用例ID
     * @return 测试用例信息
     */
    public Map<String, Object> getTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .map(this::convertToDetailMap)
                .orElse(null);
    }

    /**
     * 根据名称获取测试用例
     *
     * @param name 测试用例名称
     * @return 测试用例信息
     */
    public Map<String, Object> getTestCaseByName(String name) {
        return testCaseRepository.findByName(name)
                .map(this::convertToDetailMap)
                .orElse(null);
    }

    /**
     * 保存测试用例
     *
     * @param name 测试用例名称
     * @param steps 测试步骤列表
     * @return 保存结果
     */
    @Transactional
    public Map<String, Object> saveTestCase(Long id, String name, String originalName, List<Map<String, Object>> steps) {
    Map<String, Object> result = new HashMap<>();

    try {
        TestCaseEntity testCase;

        // 根据ID查找测试用例
        if (id != null) {
            Optional<TestCaseEntity> existingCaseById = testCaseRepository.findById(id);
            if (existingCaseById.isPresent()) {
                testCase = existingCaseById.get();

                // 如果名称变更，需要检查新名称是否已存在
                if (!testCase.getName().equals(name)) {
                    Optional<TestCaseEntity> existingCaseByName = testCaseRepository.findByName(name);
                    if (existingCaseByName.isPresent() && !existingCaseByName.get().getId().equals(id)) {
                        result.put("success", false);
                        result.put("error", "已存在相同名称的测试用例");
                        return result;
                    }
                    // 更新名称
                    testCase.setName(name);
                }
            } else {
                // ID不存在，作为新用例处理
                testCase = createNewTestCase(name, steps);
            }
        }
        // 根据原始名称查找测试用例(兼容旧版本)
        else if (originalName != null && !originalName.isEmpty()) {
            Optional<TestCaseEntity> existingCaseByName = testCaseRepository.findByName(originalName);
            if (existingCaseByName.isPresent()) {
                testCase = existingCaseByName.get();
                // 如果名称变更，需要检查新名称是否已存在
                if (!testCase.getName().equals(name)) {
                    Optional<TestCaseEntity> existingWithNewName = testCaseRepository.findByName(name);
                    if (existingWithNewName.isPresent() && !existingWithNewName.get().getId().equals(testCase.getId())) {
                        result.put("success", false);
                        result.put("error", "已存在相同名称的测试用例");
                        return result;
                    }
                    // 更新名称
                    testCase.setName(name);
                }
            } else {
                // 原始名称不存在，作为新用例处理
                testCase = createNewTestCase(name, steps);
            }
        }
        // 没有ID和原始名称，直接根据name查找
        else {
            Optional<TestCaseEntity> existingCase = testCaseRepository.findByName(name);
            if (existingCase.isPresent()) {
                testCase = existingCase.get();
            } else {
                // 不存在，创建新用例
                testCase = createNewTestCase(name, steps);
            }
        }

        // 更新测试步骤
        updateTestCaseSteps(testCase, steps);

        // 保存更新后的测试用例
        testCaseRepository.saveAndFlush(testCase);

        // 更新文件系统中的用例(用于向后兼容)
        if (originalName != null && !originalName.isEmpty() && !originalName.equals(name)) {
            // 如果名称变更，删除旧文件
            Path oldFilePath = Paths.get(CASES_DIR + originalName + ".txt");
            Files.deleteIfExists(oldFilePath);
        }
        saveToFile(name, steps);

        result.put("success", true);
        result.put("id", testCase.getId());
    } catch (Exception e) {
        e.printStackTrace();
        result.put("success", false);
        result.put("error", e.getMessage());
    }

    return result;
}

// 创建新测试用例的辅助方法
private TestCaseEntity createNewTestCase(String name, List<Map<String, Object>> steps) {
    TestCaseEntity testCase = new TestCaseEntity();
    testCase.setName(name);
    testCase.setSteps(new ArrayList<>());
    return testCaseRepository.saveAndFlush(testCase);
}

// 更新测试步骤的辅助方法
private void updateTestCaseSteps(TestCaseEntity testCase, List<Map<String, Object>> steps) {
    // 先移除所有现有步骤
    List<TestCaseStepEntity> currentSteps = new ArrayList<>(testCase.getSteps());
    for (TestCaseStepEntity step : currentSteps) {
        testCase.getSteps().remove(step);
    }
    testCaseRepository.saveAndFlush(testCase);

    // 添加新步骤
    int stepOrder = 0;
    for (Map<String, Object> step : steps) {
        TestCaseStepEntity stepEntity = new TestCaseStepEntity();
        stepEntity.setTestCase(testCase);
        stepEntity.setStepOrder(stepOrder++);
        stepEntity.setStepType((String) step.get("type"));
        stepEntity.setContent((String) step.get("content"));
        testCase.getSteps().add(stepEntity);
    }
}
    /**
     * 删除测试用例
     *
     * @param id 测试用例ID
     * @return 删除结果
     */
    @Transactional
    public Map<String, Object> deleteTestCase(Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<TestCaseEntity> testCase = testCaseRepository.findById(id);
            if (testCase.isPresent()) {
                String name = testCase.get().getName();

                // 从数据库删除
                testCaseRepository.deleteById(id);

                // 从文件系统删除(兼容旧版本)
                Path filePath = Paths.get(CASES_DIR + name + ".txt");
                Files.deleteIfExists(filePath);

                result.put("success", true);
            } else {
                result.put("success", false);
                result.put("error", "测试用例不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

//    /**
//     * 从文件系统导入测试用例到数据库
//     *
//     * @return 导入结果
//     */
//    @Transactional
//    public Map<String, Object> importFromFiles() {
//        Map<String, Object> result = new HashMap<>();
//        List<String> imported = new ArrayList<>();
//        List<String> failed = new ArrayList<>();
//
//        try {
//            Path casesPath = Paths.get(CASES_DIR);
//            if (Files.exists(casesPath) && Files.isDirectory(casesPath)) {
//                Files.list(casesPath)
//                    .filter(path -> path.toString().endsWith(".txt"))
//                    .forEach(path -> {
//                        try {
//                            String name = path.getFileName().toString().replace(".txt", "");
//                            String content = new String(Files.readAllBytes(path));
//                            List<Map<String, Object>> steps = parseStepsFromContent(content);
//
//                            // 保存到数据库
//                            Map<String, Object> saveResult = saveTestCase(name, steps);
//                            if ((Boolean) saveResult.get("success")) {
//                                imported.add(name);
//                            } else {
//                                failed.add(name);
//                            }
//                        } catch (Exception e) {
//                            failed.add(path.getFileName().toString());
//                        }
//                    });
//            }
//
//            result.put("success", true);
//            result.put("imported", imported);
//            result.put("failed", failed);
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.put("success", false);
//            result.put("error", e.getMessage());
//        }
//
//        return result;
//    }

    /**
     * 将测试用例保存到文件(向后兼容)
     */
    private void saveToFile(String name, List<Map<String, Object>> steps) throws IOException {
        StringBuilder content = new StringBuilder();

        for (Map<String, Object> step : steps) {
            String type = (String) step.get("type");
            String contentStr = (String) step.get("content");

            switch (type) {
                case "全屏":
                    content.append("全屏\n");
                    break;
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
                    content.append("等待").append(contentStr).append("\n");
                    break;
                case "设置自动等待":
                    content.append("设置自动等待").append(contentStr).append("\n");
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
                    content.append("输出").append(contentStr).append("\n");
                    break;
                case "如果":
                    content.append("如果").append(contentStr).append("\n");
                    break;
            }
        }

        // 确保目录存在
        Path directory = Paths.get(CASES_DIR);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        // 写入文件
        Files.write(Paths.get(CASES_DIR + name + ".txt"), content.toString().getBytes());
    }

    /**
     * 从文本内容解析测试步骤
     */
    private List<Map<String, Object>> parseStepsFromContent(String content) {
        List<Map<String, Object>> steps = new ArrayList<>();
        String[] lines = content.split("\n");

        for (String line : lines) {
            Map<String, Object> step = new HashMap<>();

            if (line.startsWith("全屏")) {
                step.put("type", "全屏");
                step.put("content", "");
            } else if (line.startsWith("点击")) {
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
                step.put("content", line.substring(2));
            } else if (line.startsWith("如果")) {
                step.put("type", "如果");
                step.put("content", line.substring(2));
            }

            if (!step.isEmpty()) {
                steps.add(step);
            }
        }

        return steps;
    }

    /**
     * 转换测试用例实体到简单Map
     */
    private Map<String, Object> convertToMap(TestCaseEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("name", entity.getName());
        return map;
    }

    /**
     * 转换测试用例实体到详细Map(包含步骤)
     */
    private Map<String, Object> convertToDetailMap(TestCaseEntity entity) {
        Map<String, Object> map = convertToMap(entity);

        List<Map<String, Object>> steps = entity.getSteps().stream()
                .sorted(Comparator.comparing(TestCaseStepEntity::getStepOrder))
                .map(step -> {
                    Map<String, Object> stepMap = new HashMap<>();
                    stepMap.put("type", step.getStepType());
                    stepMap.put("content", step.getContent());
                    return stepMap;
                })
                .collect(Collectors.toList());

        map.put("steps", steps);
        return map;
    }
}