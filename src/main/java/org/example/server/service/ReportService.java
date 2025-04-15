package org.example.server.service;

import org.example.server.model.TestReport;
import org.example.server.model.TestStep;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ReportService {

    private Map<String, TestReport> reports = new HashMap<>();
    private final String REPORTS_DIR = "reports";

    public ReportService() {
        // 确保报告目录存在
        new File(REPORTS_DIR).mkdirs();
    }

    public String generateReport(Map<String, Object> taguiResult) {
        // 生成唯一报告ID
        String reportId = UUID.randomUUID().toString();

        // 创建报告对象
        TestReport report = new TestReport();
        report.setReportId(reportId);
        report.setReportDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // 解析TagUI执行结果
        boolean success = (boolean) taguiResult.getOrDefault("success", false);
        List<String> logs = (List<String>) taguiResult.getOrDefault("logs", new ArrayList<>());

        // 设置环境信息
        Map<String, String> environment = new HashMap<>();
        environment.put("os", System.getProperty("os.name"));
        environment.put("browser", "Chrome"); // 可以从TagUI结果中提取
        environment.put("executionTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        report.setEnvironment(environment);

        // 从日志中解析步骤信息
        List<TestStep> steps = parseStepsFromLogs(logs);
        report.setSteps(steps);

        // 统计步骤状态
        int totalSteps = steps.size();
        int successSteps = 0;
        int warningSteps = 0;
        int errorSteps = 0;

        for (TestStep step : steps) {
            switch (step.getStatus()) {
                case "success":
                    successSteps++;
                    break;
                case "warning":
                    warningSteps++;
                    break;
                case "error":
                    errorSteps++;
                    break;
            }
        }

        report.setTotalSteps(totalSteps);
        report.setSuccessSteps(successSteps);
        report.setWarningSteps(warningSteps);
        report.setErrorSteps(errorSteps);

        // 计算总执行时间
        long totalTimeMillis = 0;
        for (TestStep step : steps) {
            String duration = step.getDuration();
            if (duration != null && duration.endsWith("ms")) {
                try {
                    totalTimeMillis += Long.parseLong(duration.replace("ms", "").trim());
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        report.setTotalTime(totalTimeMillis / 1000.0 + "s");

        // 保存报告到内存
        reports.put(reportId, report);

        // 创建报告目录
        Path reportDir = Paths.get(REPORTS_DIR, reportId);
        Path screenshotsDir = Paths.get(REPORTS_DIR, reportId, "screenshots");

        try {
            Files.createDirectories(screenshotsDir);

            // 如果有截图，可以将截图复制到报告目录
            // 例如：Files.copy(sourcePath, screenshotsDir.resolve(filename));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return reportId;
    }

    private List<TestStep> parseStepsFromLogs(List<String> logs) {
        List<TestStep> steps = new ArrayList<>();
        TestStep currentStep = null;

        for (String log : logs) {
            // 检测新步骤开始
            if (log.matches("^INFO \\| 执行步骤: .*")) {
                // 如果有前一个步骤，添加到列表
                if (currentStep != null) {
                    steps.add(currentStep);
                }

                // 创建新步骤
                currentStep = new TestStep();
                String actionContent = log.replaceFirst("INFO \\| 执行步骤: ", "");

                // 尝试解析动作和内容
                int splitIndex = actionContent.indexOf(" ");
                if (splitIndex > 0) {
                    currentStep.setAction(actionContent.substring(0, splitIndex).trim());
                    currentStep.setContent(actionContent.substring(splitIndex).trim());
                } else {
                    currentStep.setAction(actionContent);
                    currentStep.setContent("");
                }

                // 默认设置为成功状态
                currentStep.setStatus("success");

                // 添加日志
                Map<String, String> stepLog = new HashMap<>();
                stepLog.put("type", "info");
                stepLog.put("message", log);
                List<Map<String, String>> stepLogs = new ArrayList<>();
                stepLogs.add(stepLog);
                currentStep.setLogs(stepLogs);
            }
            // 如果有当前步骤，添加日志
            else if (currentStep != null) {
                Map<String, String> stepLog = new HashMap<>();

                // 根据日志内容确定类型
                if (log.contains("警告") || log.contains("WARNING")) {
                    stepLog.put("type", "warning");
                    // 如果是第一个警告，将步骤状态设置为警告
                    if ("success".equals(currentStep.getStatus())) {
                        currentStep.setStatus("warning");
                    }
                } else if (log.contains("错误") || log.contains("ERROR") || log.contains("FAIL")) {
                    stepLog.put("type", "error");
                    // 错误始终覆盖其他状态
                    currentStep.setStatus("error");
                } else {
                    stepLog.put("type", "info");
                }

                stepLog.put("message", log);
                currentStep.getLogs().add(stepLog);

                // 检查是否包含执行时间信息
                if (log.matches(".*耗时: \\d+ms.*")) {
                    String duration = log.replaceFirst(".*耗时: (\\d+)ms.*", "$1ms");
                    currentStep.setDuration(duration);
                }

                // 检查是否有截图信息
                if (log.contains("截图已保存") || log.contains("saved screenshot")) {
                    // 从日志中提取截图路径，这需要根据实际日志格式调整
                    String screenshotPath = log.replaceFirst(".*截图已保存到: (.*)", "$1");
                    // 提取文件名
                    String filename = new File(screenshotPath).getName();
                    currentStep.setScreenshot(filename);

                    // 可以在这里复制截图到报告目录
                }
            }
        }

        // 添加最后一个步骤
        if (currentStep != null) {
            steps.add(currentStep);
        }

        return steps;
    }

    public TestReport getReportById(String reportId) {
        return reports.get(reportId);
    }
}