package org.example.server.locator;

import org.example.server.exception.RuleEngineException;

import java.util.Map;

public class CssLocatorStrategy implements ElementLocatorStrategy {
    private final Map<String, String> CSSMappings;

    public CssLocatorStrategy(Map<String, String> xpathMappings) {
        this.CSSMappings = xpathMappings;
    }

    @Override
    public String getName() {
        return "Css";
    }

    @Override
    public String getLocator(String elementName) {
        return getLocator(elementName, null);
    }

    @Override
    public String getLocator(String elementName, String context) {
        String key = context != null ? context + "_" + elementName : elementName;
        String xpath = CSSMappings.get(key);
        if (xpath == null || xpath.trim().isEmpty()) {
            throw new RuleEngineException("找不到元素的Css映射: " + elementName,
                    RuleEngineException.ErrorLevel.ERROR);
        }
        return xpath;
    }

    @Override
    public String getLocatorType() {
        return "Css";
    }
}
