package org.example.server.model;

import java.util.List;
import java.util.Map;

public class TestStep {
    private String action;
    private String content;
    private String status; // success, warning, error
    private String duration;
    private String screenshot;
    private List<Map<String, String>> logs;

    // 添加getter和setter方法
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getScreenshot() { return screenshot; }
    public void setScreenshot(String screenshot) { this.screenshot = screenshot; }

    public List<Map<String, String>> getLogs() { return logs; }
    public void setLogs(List<Map<String, String>> logs) { this.logs = logs; }
}