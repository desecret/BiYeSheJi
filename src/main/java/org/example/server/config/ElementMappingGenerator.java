package org.example.server.config;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class ElementMappingGenerator {

    public static Map<String, Map<String, String>> generateMappings(String htmlSource) {
        Document doc = Jsoup.parse(htmlSource);
        Map<String, Map<String, String>> mappings = new HashMap<>();

        // 查找输入框
        Elements inputs = doc.select("input[type=text], input[type=password], input[type=email]");
        for (Element input : inputs) {
            String name = guessBestElementName(input);
            if (name != null) {
                Map<String, String> elementMappings = new HashMap<>();
                elementMappings.put("xpath", getXPath(input));
                elementMappings.put("cssSelector", getCssSelector(input));
                mappings.put(name, elementMappings);
            }
        }

        // 查找按钮
        Elements buttons = doc.select("button, input[type=button], input[type=submit]");
        for (Element button : buttons) {
            String name = guessBestElementName(button);
            if (name != null) {
                Map<String, String> elementMappings = new HashMap<>();
                elementMappings.put("xpath", getXPath(button));
                elementMappings.put("cssSelector", getCssSelector(button));
                mappings.put(name, elementMappings);
            }
        }

        return mappings;
    }

    private static String guessBestElementName(Element element) {
        // 按优先级尝试不同属性来推断元素名称
        String name = element.attr("name");
        if (!name.isEmpty()) return name + "输入框";

        name = element.attr("id");
        if (!name.isEmpty()) return name + "输入框";

        name = element.attr("placeholder");
        if (!name.isEmpty()) return name + "输入框";

        name = element.text();
        if (!name.isEmpty()) return name + "按钮";

        name = element.attr("value");
        if (!name.isEmpty()) return name + "按钮";

        return null;
    }

    private static String getXPath(Element element) {
        // 生成相对简单的XPath
        StringBuilder xpath = new StringBuilder();

        // 优先使用id
        String id = element.attr("id");
        if (!id.isEmpty()) {
            return "//*[@id='" + id + "']";
        }

        // 其次使用name
        String name = element.attr("name");
        if (!name.isEmpty()) {
            return "//" + element.tagName() + "[@name='" + name + "']";
        }

        // 使用class和内容组合
        String className = element.attr("class");
        if (!className.isEmpty()) {
            xpath.append("//").append(element.tagName()).append("[@class='").append(className).append("']");

            // 如果有文本内容，添加文本条件
            if (!element.text().isEmpty()) {
                xpath.append("[text()='").append(element.text()).append("']");
            }

            return xpath.toString();
        }

        // 最后使用相对位置
        return getRelativeXPath(element);
    }

    private static String getRelativeXPath(Element element) {
        // 生成相对XPath (简化实现)
        return "//" + element.tagName() + "[contains(text(),'" +
               element.text().substring(0, Math.min(10, element.text().length())) + "')]";
    }

    private static String getCssSelector(Element element) {
        // 生成CSS选择器
        String id = element.attr("id");
        if (!id.isEmpty()) {
            return "#" + id;
        }

        String className = element.attr("class");
        if (!className.isEmpty()) {
            // 取第一个class
            String firstClass = className.split("\\s+")[0];
            return element.tagName() + "." + firstClass;
        }

        String name = element.attr("name");
        if (!name.isEmpty()) {
            return element.tagName() + "[name='" + name + "']";
        }

        // 基于属性生成
        return element.tagName() + "[type='" + element.attr("type") + "']";
    }

    public static void updateYamlFile(String yamlPath, Map<String, Map<String, String>> mappings) throws IOException {
        // 读取现有YAML
        Yaml yaml = new Yaml();
        Map<String, Object> data = new HashMap<>();

        try {
            File yamlFile = new File(yamlPath);
            if (yamlFile.exists()) {
                data = yaml.load(Files.newInputStream(yamlFile.toPath()));
            }
        } catch (Exception e) {
            data.put("defaultLocatorType", "xpath");
            data.put("configs", new ArrayList<>());
        }

        // 获取配置列表
        List<Map<String, Object>> configs = (List<Map<String, Object>>) data.get("configs");
        if (configs == null) {
            configs = new ArrayList<>();
            data.put("configs", configs);
        }

        // 更新配置
        for (Map.Entry<String, Map<String, String>> entry : mappings.entrySet()) {
            String elementName = entry.getKey();
            Map<String, String> elementMappings = entry.getValue();

            // 查找是否已存在
            boolean found = false;
            for (Map<String, Object> config : configs) {
                if (elementName.equals(config.get("name"))) {
                    config.put("xpath", elementMappings.get("xpath"));
                    config.put("cssSelector", elementMappings.get("cssSelector"));
                    found = true;
                    break;
                }
            }

            // 不存在则添加新配置
            if (!found) {
                Map<String, Object> newConfig = new HashMap<>();
                newConfig.put("name", elementName);
                newConfig.put("imagePath", "");
                newConfig.put("xpath", elementMappings.get("xpath"));
                newConfig.put("cssSelector", elementMappings.get("cssSelector"));
                newConfig.put("locatorType", "xpath");
                configs.add(newConfig);
            }
        }

        // 写回到文件
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml outYaml = new Yaml(options);

        try (FileWriter writer = new FileWriter(yamlPath)) {
            outYaml.dump(data, writer);
        }
    }
}
