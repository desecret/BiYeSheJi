package org.example.server.config;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 元素配置类
 * - name: 元素名称
 * - context: 上下文信息
 * - imagePath: 图片路径
 * - xpath: xpath路径
 * - cssSelector: css选择器
 * - locatorType: 定位方式
 */
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