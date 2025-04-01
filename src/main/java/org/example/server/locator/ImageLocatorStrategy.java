package org.example.server.locator;

import java.util.Map;

public class ImageLocatorStrategy implements ElementLocatorStrategy {
    // private final ImageMapper imageMapper;
    Map<String, String> imageMappings;

    public ImageLocatorStrategy(Map<String, String> imageMappings) {
        this.imageMappings = imageMappings;
    }

    @Override
    public String getName() {
        return "image";
    }

    @Override
    public String getLocator(String elementName) {
        return getLocator(elementName, null);
    }

    @Override
    public String getLocator(String elementName, String context) {
        String key = context != null ? context + "_" + elementName : elementName;
        String imagePath = imageMappings.get(key);
        if (imagePath == null || imagePath.trim().isEmpty()) {
            throw new RuntimeException("找不到元素的图片映射: " + key);
        }
        return imagePath;
    }

    @Override
    public String getLocatorType() {
        return "image";
    }
}