package org.example.server.util;

import org.example.server.config.ElementConfig;
import org.example.server.handler.ActionHandler;
import org.example.server.handler.ActionHandlerRegistry;
import org.example.server.handler.GenericActionHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.example.server.staticString.IMAGES_DIR;
import static org.example.server.staticString.YAML_FILE;

public class util {

    /**
     * 列出所有支持的命令格式及对应的执行语句
     */
    public static void printSupportedCommands() {
        System.out.println("========== 支持的命令格式 ==========");
        System.out.println("| 自然语言命令格式              | 转换后的执行语句          |");
        System.out.println("|------------------------------|--------------------------|");

        for (ActionHandler handler : ActionHandlerRegistry.handlers) {
            if (handler instanceof GenericActionHandler) {
                GenericActionHandler genericHandler = (GenericActionHandler) handler;
                String patternStr = genericHandler.getPattern().pattern()
                        .replace("(.*?)", "<元素>")
                        .replace("\"(.*?)\"", "\"<值>\"");

                String actionType = "";
                try {
                    // 通过反射获取action字段
                    java.lang.reflect.Field field = GenericActionHandler.class.getDeclaredField("action");
                    field.setAccessible(true);
                    actionType = (String) field.get(genericHandler);
                } catch (Exception e) {
                    // 忽略异常
                }

                String resultFormat = getResultFormat(actionType, patternStr);

                System.out.printf("| %-28s | %-24s |\n", patternStr, resultFormat);
            }
        }

        System.out.println("==================================");
    }

    /**
     * 获取结果格式
     */
    private static String getResultFormat(String actionType, String pattern) {
        switch (actionType) {
            case "click":
            case "rclick":
            case "dclick":
            case "hover":
                return actionType + " images/元素.png";
            case "type":
                return "type images/元素.png as 值";
            case "wait":
                return "wait <秒数>";
            case "timeout":
                return "timeout <秒数>";
            case "echo":
                return "echo <值>";
            case "exist":
                return "exist('images/元素.png')";
            case "present":
                return "present('images/元素.png')";
            case "mouse":
                if (pattern.contains("按下"))
                    return "mouse down";
                else if (pattern.contains("弹起"))
                    return "mouse up";
                return "mouse <操作>";
            case "conditional":
                return "if <条件> then <动作>";
            case "":
                if (pattern.startsWith("访问"))
                    return "https://<网址>";
                return "<特殊命令>";
            default:
                return "未知命令类型";
        }
    }

    /**
     * 将图片名称转换为YAML格式
     *
     * @throws IOException IO异常
     */
    public static void writeToYaml() throws IOException {
        List<ElementConfig> configs = new ArrayList<>();
        File imagesDir = new File(IMAGES_DIR);

        if (!imagesDir.exists() || !imagesDir.isDirectory()) {
            throw new IOException("Images directory not found: " + IMAGES_DIR);
        }

        // 递归扫描所有文件夹
        scanDirectory(imagesDir, configs);

        StringBuilder yamlContent = new StringBuilder();
        yamlContent.append("defaultLocatorType: \"image\"\n\n");
        yamlContent.append("configs:\n");

        for (ElementConfig config : configs) {
            yamlContent.append("  - name: \"").append(config.getName()).append("\"\n");
            yamlContent.append("    context: \"").append(config.getContext()).append("\"\n");
            yamlContent.append("    imagePath: \"").append(config.getImagePath()).append("\"\n");
            yamlContent.append("    xpath: \"\"\n");
            yamlContent.append("    cssSelector: \"\"\n");
            yamlContent.append("    locatorType: \"image\"\n");
            yamlContent.append("\n");
        }

        Files.write(Paths.get(YAML_FILE), yamlContent.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 递归扫描目录并收集图片信息
     *
     * @param directory 要扫描的目录
     * @param configs   收集的元素配置列表
     */
    private static void scanDirectory(File directory, List<ElementConfig> configs) {
        File[] files = directory.listFiles();
        if (files == null)
            return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanDirectory(file, configs);
            } else {
                String filename = file.getName().toLowerCase();
                if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                    // 获取相对于IMAGES_DIR的路径
                    String relativePath = file.getAbsolutePath()
                            .substring(new File(IMAGES_DIR).getAbsolutePath().length() + 1);
                    String imagePath = "images/" + relativePath.replace('\\', '/'); // 确保路径分隔符为正斜杠

                    // 从路径中提取context（目录名）
                    String context = "";
                    String[] pathParts = relativePath.split("[\\\\/]");
                    if (pathParts.length > 1) {
                        context = pathParts[0]; // 使用第一级目录名作为context
                    }

                    // 去除filename中的扩展名
                    filename = filename.substring(0, filename.lastIndexOf('.'));
                    String[] parts = filename.split("_");
                    if (parts.length >= 2) {
                        // 从文件名提取显示名称
                        String displayName = parts[0] + parts[1];
                        ElementConfig config = new ElementConfig(displayName, imagePath);
                        config.setContext(context); // 设置context
                        configs.add(config);
                    }
                }
            }
        }
    }
}
