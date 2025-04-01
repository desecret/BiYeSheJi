package org.example.server.config;

import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
@Data
public class ElementMappings {
    private List<ElementConfig> configs; // 对应YAML中的configs列表
    private String defaultLocatorType = "image"; // 默认定位方式

    public ElementMappings() {
    }

}
