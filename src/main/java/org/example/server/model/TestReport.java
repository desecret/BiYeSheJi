package org.example.server.model;

import java.util.List;
import java.util.Map;

public class TestReport {
    private String reportId;
    private String reportDate;
    private int totalSteps;
    private int successSteps;
    private int warningSteps;
    private int errorSteps;
    private String totalTime;
    private Map<String, String> environment;
    private List<TestStep> steps;

    // 添加getter和setter方法
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReportDate() { return reportDate; }
    public void setReportDate(String reportDate) { this.reportDate = reportDate; }

    public int getTotalSteps() { return totalSteps; }
    public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }

    public int getSuccessSteps() { return successSteps; }
    public void setSuccessSteps(int successSteps) { this.successSteps = successSteps; }

    public int getWarningSteps() { return warningSteps; }
    public void setWarningSteps(int warningSteps) { this.warningSteps = warningSteps; }

    public int getErrorSteps() { return errorSteps; }
    public void setErrorSteps(int errorSteps) { this.errorSteps = errorSteps; }

    public String getTotalTime() { return totalTime; }
    public void setTotalTime(String totalTime) { this.totalTime = totalTime; }

    public Map<String, String> getEnvironment() { return environment; }
    public void setEnvironment(Map<String, String> environment) { this.environment = environment; }

    public List<TestStep> getSteps() { return steps; }
    public void setSteps(List<TestStep> steps) { this.steps = steps; }
}