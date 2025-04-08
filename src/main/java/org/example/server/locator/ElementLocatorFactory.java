package org.example.server.locator;

import org.example.server.config.*;
import org.example.server.exception.RuleEngineException;
import org.example.server.util.ErrorHandler;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.server.staticString.CONFIG_FILE;

public class ElementLocatorFactory {
    private static ElementMappings elementMappings;
    private static final Map<String, ElementConfig> elementConfigs = new ConcurrentHashMap<>();
    private static final Map<String, String> ImageMappings = new ConcurrentHashMap<>();
    private static final Map<String, String> xpathMappings = new ConcurrentHashMap<>();
    private static final Map<String, String> cssMappings = new ConcurrentHashMap<>();
    private static final Map<String, ElementLocatorStrategy> strategies = new HashMap<>();

    public static void init() {
        loadConfig();
        registerStrategies();
    }

    private static void loadConfig() {
        try {
            // 创建 LoaderOptions
            LoaderOptions options = new LoaderOptions();
            // 使用 LoaderOptions 创建 Constructor
            Yaml yaml = new Yaml(new Constructor(ElementMappings.class, options));
            String content = new String(Files.readAllBytes(Paths.get(CONFIG_FILE)));
            elementMappings = yaml.load(content);

            if (elementMappings == null || elementMappings.getConfigs() == null) {
                ErrorHandler.logError("配置文件格式错误或为空");
                return;
            }

            // 缓存所有配置
            elementMappings.getConfigs().forEach(config -> {
                elementConfigs.put(config.getName(), config);

                if (config.getImagePath() != null && !config.getImagePath().isEmpty()) {
                    ImageMappings.put(config.getContext() + "_" + config.getName(), config.getImagePath());
                }

                if (config.getXpath() != null && !config.getXpath().isEmpty()) {
                    xpathMappings.put(config.getContext() + "_" + config.getName(), config.getXpath());
                }

                if (config.getCssSelector() != null && !config.getCssSelector().isEmpty()) {
                    cssMappings.put(config.getContext() + "_" + config.getName(), config.getCssSelector());
                }
            });
        } catch (IOException e) {
            ErrorHandler.logError("读取配置文件出错: " + e.getMessage(), e);
        }
    }

    private static void registerStrategies() {
        strategies.put("image", new ImageLocatorStrategy(ImageMappings));
        strategies.put("xpath", new XPathLocatorStrategy(xpathMappings));
        strategies.put("css", new CssLocatorStrategy(cssMappings));
    }

    public static String getLocator(String elementName) {
        return getLocator(elementName, null);
    }

    public static String getLocator(String elementName, String context) throws RuleEngineException {
        String locatorType;
        try {
            locatorType = getLocatorType(elementName, null);
        } catch (RuleEngineException e) {
            ErrorHandler.handle(e);
            throw new RuleEngineException("获取元素定位类型失败: " + e.getMessage(),
                    RuleEngineException.ErrorLevel.ERROR);
        }

        ElementLocatorStrategy strategy = strategies.get(locatorType);
        if (strategy == null) {
            throw new RuleEngineException("不支持的元素定位策略: " + locatorType,
                    RuleEngineException.ErrorLevel.ERROR);
        }

        return strategy.getLocator(elementName, context);
    }

    public static String getLocatorType(String elementName, String preferredType) {
        if (elementName == null || elementName.trim().isEmpty()) {
            throw new RuleEngineException("元素名称为空",
                    RuleEngineException.ErrorLevel.ERROR);
        }

        ElementConfig config = elementConfigs.get(elementName);
        if (config == null) {
            throw new RuleEngineException("找不到元素的映射配置: " + elementName,
                    RuleEngineException.ErrorLevel.ERROR);
        }

        String locatorType = preferredType;
        if (locatorType == null || locatorType.isEmpty()) {
            locatorType = config.getLocatorType();
            if (locatorType == null || locatorType.isEmpty()) {
                locatorType = elementMappings.getDefaultLocatorType();
            }
        }

        return locatorType;
    }
}