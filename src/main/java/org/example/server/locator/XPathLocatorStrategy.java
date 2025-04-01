package org.example.server.locator;

import org.example.server.exception.RuleEngineException;

import java.util.Map;

public class XPathLocatorStrategy implements ElementLocatorStrategy {
    private final Map<String, String> xpathMappings;

    public XPathLocatorStrategy(Map<String, String> xpathMappings) {
        this.xpathMappings = xpathMappings;
    }

    @Override
    public String getName() {
        return "xpath";
    }

    @Override
    public String getLocator(String elementName) {
        return getLocator(elementName, null);
    }

    @Override
    public String getLocator(String elementName, String context) {
        String key = context != null ? context + "_" + elementName : elementName;
        String xpath = xpathMappings.get(key);
        if (xpath == null || xpath.trim().isEmpty()) {
            throw new RuleEngineException("找不到元素的XPath映射: " + elementName,
                    RuleEngineException.ErrorLevel.ERROR);
        }
        return xpath;
    }

    @Override
    public String getLocatorType() {
        return "xpath";
    }
}
