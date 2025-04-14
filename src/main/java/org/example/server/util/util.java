package org.example.server.util;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.example.server.config.ElementConfig;
import org.example.server.config.ElementMappings;
import org.example.server.handler.ActionHandler;
import org.example.server.handler.ActionHandlerRegistry;
import org.example.server.handler.GenericActionHandler;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.example.server.staticString.*;

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
                        String displayName = parts[0] + " " + parts[1];
                        ElementConfig config = new ElementConfig(displayName, imagePath);
                        config.setContext(context); // 设置context
                        configs.add(config);
                    }
                }
            }
        }
    }

    /**
     * 查找元素对应的图片路径
     * <p>
     * 根据元素信息，查找对应的预览图片
     * </p>
     *
     * @param element 元素配置
     * @return 图片相对路径，如果没有找到返回null
     */
    public static String findElementImage(ElementConfig element) {
        return element.getImagePath();
    }

    /**
     * 保存元素映射到YAML文件
     * <p>
     * 将元素配置写回YAML文件
     * </p>
     *
     * @param mappings 元素映射配置
     * @throws IOException 如果写入文件失败
     */
    public static void saveElementMappings(ElementMappings mappings) throws IOException {
        try {
            // 设置YAML输出选项
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            options.setIndent(2);

            // 创建一个干净的数据结构，不包含类型信息
            Map<String, Object> yamlData = new LinkedHashMap<>();
            yamlData.put("defaultLocatorType", "image");

            List<Map<String, String>> cleanConfigs = new ArrayList<>();
            for (ElementConfig config : mappings.getConfigs()) {
                Map<String, String> cleanConfig = new LinkedHashMap<>();
                cleanConfig.put("name", config.getName());
                cleanConfig.put("context", config.getContext());
                cleanConfig.put("imagePath", config.getImagePath());
                cleanConfig.put("xpath", config.getXpath() != null ? config.getXpath() : "");
                cleanConfig.put("cssSelector", config.getCssSelector() != null ? config.getCssSelector() : "");
                cleanConfig.put("locatorType", config.getLocatorType());
                cleanConfigs.add(cleanConfig);
            }

            yamlData.put("configs", cleanConfigs);

            // 使用手动序列化避免类型标记
            StringBuilder yaml = new StringBuilder();
            yaml.append("defaultLocatorType: \"").append(yamlData.get("defaultLocatorType")).append("\"\n");
            yaml.append("configs:\n");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> configs = (List<Map<String, String>>) yamlData.get("configs");

            for (Map<String, String> config : configs) {
                yaml.append("  - name: \"").append(config.get("name")).append("\"\n");
                yaml.append("    context: \"").append(config.get("context")).append("\"\n");
                yaml.append("    imagePath: \"").append(config.get("imagePath")).append("\"\n");
                yaml.append("    xpath: \"").append(config.get("xpath")).append("\"\n");
                yaml.append("    cssSelector: \"").append(config.get("cssSelector")).append("\"\n");
                yaml.append("    locatorType: \"").append(config.get("locatorType")).append("\"\n");
            }

            // 写入文件
            try (FileWriter writer = new FileWriter(YAML_FILE)) {
                writer.write(yaml.toString());
            }
        } catch (Exception e) {
            throw new IOException("保存元素映射时出错: " + e.getMessage(), e);
        }
    }

    /**
     * 确定文件的内容类型
     *
     * @param path 文件路径
     * @return 内容类型
     */
    public static String determineContentType(String path) {
        if (path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (path.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (path.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }


    public static Map<String, Object> runTaguiCommand() {
        Map<String, Object> result = new HashMap<>();

        try {
            // 启动 TagUI 进程
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "tagui", TAGUI_SCRIPT_PATH);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 等待一段时间，让 SikuliX 窗口出现
//            Thread.sleep(2000);
//
//            // 查找标题包含"SikuliX Engine"的窗口
//            final WinDef.HWND sikulixWindow = User32.INSTANCE.FindWindow(null, "SikuliX Engine");
//            if (sikulixWindow != null) {
//                // 收集所有子窗口文本
//                final List<String> childTexts = new ArrayList<>();
//
//                User32.WNDENUMPROC callback = (hwnd, data) -> {
//                    char[] windowText = new char[512];
//                    User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
//                    String text = Native.toString(windowText);
//                    if (!text.isEmpty()) {
//                        childTexts.add(text);
//                    }
//                    return true;
//                };
//
//                User32.INSTANCE.EnumChildWindows(sikulixWindow, callback, null);
//
//                result.put("sikulixWindowFound", true);
//                result.put("sikulixTexts", childTexts);
//            } else {
//                result.put("sikulixWindowFound", false);
//            }

            // 等待进程完成
            int exitCode = process.waitFor();

            // 读取标准输出
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                result.put("output", output.toString());
            }

            result.put("success", exitCode == 0);
            result.put("exitCode", exitCode);

            return result;
        } catch (Exception e) {
            result.put("error", "执行TagUI命令时出错: " + e.getMessage());
            return result;
        }
    }
}
