package org.example.server.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ElementConfig {
    private String name;
    private String context; // 添加上下文信息
    private String imagePath;
    private String xpath;
    private String cssSelector;
    private String locatorType; // 默认使用的定位方式: image, xpath, css

    public ElementConfig(String name, String imagePath) {
        this.name = name;
        this.imagePath = imagePath;
        this.locatorType = "image"; // 向后兼容
    }

    public ElementConfig() {
    }
}