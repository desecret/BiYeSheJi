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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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


    /**
     * 执行TagUI命令
     *
     * @param scriptPath TagUI脚本文件路径
     * @return 执行结果
     */
    public static Map<String, Object> runTaguiCommand(String scriptPath) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 启动 TagUI 进程
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cmd", "/c", "tagui", scriptPath, "-r");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

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

            // 生成测试报告
            boolean success = enhanceTagUIReport(TAGUI_SCRIPT_PATH, REPORT_PATH);

            result.put("success", exitCode == 0 && success);
            result.put("exitCode", exitCode);


            return result;
        } catch (Exception e) {
            result.put("error", "执行TagUI命令时出错: " + e.getMessage());
            return result;
        }
    }

    /**
     * 根据脚本与执行对比结果增强TagUI报告
     *
     * @param scriptPath TagUI脚本文件路径
     * @param reportPath TagUI报告文件路径
     * @return 是否成功增强报告
     * @throws IOException 如果文件读取或写入出错
     */
    public static boolean enhanceTagUIReport(String scriptPath, String reportPath) throws IOException {
        // 获取脚本与执行的对比结果
        Map<String, Object> comparison = compareScriptWithExecution(scriptPath, reportPath);

        // 读取原始报告内容
        String htmlContent = new String(Files.readAllBytes(Paths.get(reportPath)), StandardCharsets.UTF_8);

        // 解析HTML
        Document doc = Jsoup.parse(htmlContent);

        // 添加CSS样式
        Element head = doc.head();
        head.append("<style>\n" +
                    ".execution-summary {\n" +
                    "  background-color: #f8f9fa;\n" +
                    "  border: 1px solid #ddd;\n" +
                    "  border-radius: 5px;\n" +
                    "  padding: 15px;\n" +
                    "  margin: 20px 0;\n" +
                    "  font-size: 14px;\n" +
                    "}\n" +
                    ".execution-stats {\n" +
                    "  margin-bottom: 10px;\n" +
                    "}\n" +
                    ".not-executed {\n" +
                    "  background-color: #fff3cd;\n" +
                    "  border-left: 3px solid #ffc107;\n" +
                    "  padding: 5px 10px;\n" +
                    "  margin: 5px 0;\n" +
                    "}\n" +
                    ".execution-rate {\n" +
                    "  font-weight: bold;\n" +
                    "  color: #0d6efd;\n" +
                    "}\n" +
                    "</style>");

        // 创建执行摘要区域
        Element body = doc.body();

        // 查找FINISH位置，在其前面插入摘要
        Elements elements = body.select("h3");
        Element reportBody = null;
        if (!elements.isEmpty()) {
            reportBody = elements.first();
        } else {
            return false; // 无法找到报告主体
        }

        // 创建执行摘要HTML
        int totalExpected = (int) comparison.get("预期执行总数");
        int notExecutedCount = (int) comparison.get("未执行总数");
        double executionRate = (double) comparison.get("执行率") * 100;

        StringBuilder summaryHtml = new StringBuilder();
        summaryHtml.append("<div class=\"execution-summary\">\n");
        summaryHtml.append("  <h4>执行摘要</h4>\n");
        summaryHtml.append("  <div class=\"execution-stats\">\n");
        summaryHtml.append("    <div>预期执行命令总数: ").append(totalExpected).append("</div>\n");
        summaryHtml.append("    <div>未执行命令数: ").append(notExecutedCount).append("</div>\n");
        summaryHtml.append("    <div>执行率: <span class=\"execution-rate\">").append(String.format("%.2f", executionRate)).append("%</span></div>\n");
        summaryHtml.append("  </div>\n");

        // 显示未执行的命令
        @SuppressWarnings("unchecked")
        List<String> notExecuted = (List<String>) comparison.get("未执行");
        if (!notExecuted.isEmpty()) {
            summaryHtml.append("  <h4>未执行的命令:</h4>\n");
            for (String cmd : notExecuted) {
                summaryHtml.append("  <div class=\"not-executed\">").append(cmd).append("</div>\n");
            }
        }

        summaryHtml.append("</div>\n");

        // 找到FINISH行并在其前面插入摘要
        String finishMarker = "FINISH - automation finished";
        Element finishElem = null;

        // 遍历所有br标签，查找包含FINISH文本的位置
        for (Element el : reportBody.children()) {
            if (el.text().contains(finishMarker)) {
                finishElem = el;
                break;
            }
        }

        if (finishElem != null) {
            // 在FINISH行前面插入摘要
            finishElem.before(summaryHtml.toString());
        } else {
            // 如果找不到FINISH行，则在报告末尾添加
            reportBody.append(summaryHtml.toString());
        }

        // 保存修改后的HTML
        Files.write(Paths.get(reportPath), doc.outerHtml().getBytes(StandardCharsets.UTF_8));

        return notExecutedCount == 0; // 返回是否有未执行的命令
    }


    /**
     * 从TagUI HTML报告中提取测试命令
     *
     * @param reportPath 报告文件路径
     * @return 提取的命令列表
     */
    public static List<String> extractCommandsFromReport(String reportPath) throws IOException {
        List<String> commands = new ArrayList<>();
        String htmlContent;
        htmlContent = new String(Files.readAllBytes(Paths.get(reportPath)), StandardCharsets.UTF_8);

        // 查找起始标记（START标签后的第一个<br>）
        int startIndex = htmlContent.indexOf("<span style=\"color: rgb(30,130,201);\">START");
        if (startIndex == -1) return commands;

        startIndex = htmlContent.indexOf("<br>", startIndex);
        if (startIndex == -1) return commands;

        // 向后移动以跳过起始的<br>
        startIndex += 4;

        // 查找结束标记（包含"FINISH - automation finished"的<br>标签）
        int endIndex = htmlContent.indexOf("FINISH - automation finished", startIndex);
        if (endIndex == -1) return commands;

        // 向前移动到该行的行首
        endIndex = htmlContent.lastIndexOf("<br>", endIndex);
        if (endIndex == -1 || endIndex < startIndex) return commands;

        // 提取命令部分的HTML
        String commandsHtml = htmlContent.substring(startIndex, endIndex).trim();

        // 按<br>标签分割并提取命令
        String[] lines = commandsHtml.split("<br>");
        List<String> rawCommands = new ArrayList<>();

        // 先收集所有非空行
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                rawCommands.add(trimmedLine);
            }
        }

        // 检查是否有重复的URL命令在最后
        boolean removeLast = false;
        if (rawCommands.size() >= 2) {
            String lastCommand = rawCommands.get(rawCommands.size() - 1);

            // 检查最后一个命令是否是URL
            if (lastCommand.startsWith("https://") || lastCommand.startsWith("http://")) {
                // 检查URL是否在前面的命令中出现过（仅对比URL部分）
                String lastUrl = lastCommand.split(" - ")[0].trim();

                // 获取倒数第二行的位置
                int secondLastPosition = rawCommands.size() - 2;

                // 查找倒数第二行前面是否有一个空行
                boolean hasEmptyLine = false;
                for (int i = secondLastPosition; i >= 0; i--) {
                    if (lines[i].trim().isEmpty()) {
                        hasEmptyLine = true;
                        break;
                    }
                }

                // 检查该URL是否在前面的命令中出现过
                boolean urlAppearedBefore = false;
                for (int i = 0; i < rawCommands.size() - 1; i++) {
                    String cmd = rawCommands.get(i);
                    if (cmd.startsWith(lastUrl)) {
                        urlAppearedBefore = true;
                        break;
                    }
                }

                // 如果URL在前面出现过，且中间存在空行，则标记为需要移除
                if (urlAppearedBefore && hasEmptyLine) {
                    removeLast = true;
                }
            }
        }

        // 处理命令，删除URL后半部分，并根据条件舍去最后一个命令
        for (int i = 0; i < (removeLast ? rawCommands.size() - 1 : rawCommands.size()); i++) {
            String cmd = rawCommands.get(i);

            // 如果是URL命令，则删除URL后半部分（例如" - 电力运维管理系统"）
            if (cmd.startsWith("https://") || cmd.startsWith("http://")) {
                int dashIndex = cmd.indexOf(" - ");
                if (dashIndex > 0) {
                    cmd = cmd.substring(0, dashIndex);
                }
            }

            commands.add(cmd);
        }

        return commands;
    }



    /**
     * 对比脚本中预期执行的命令与实际执行的命令
     *
     * @param scriptPath TagUI脚本文件路径
     * @param reportPath TagUI报告文件路径
     * @return 包含对比结果的Map
     * @throws IOException 如果文件读取出错
     */
    public static Map<String, Object> compareScriptWithExecution(String scriptPath, String reportPath) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // 读取脚本文件中的预期命令，过滤if语句
        List<String> rawExpectedCommands = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(scriptPath), StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                rawExpectedCommands.add(line);
            }
        }

        // 处理if语句，创建实际需要执行的命令列表
        List<String> expectedCommands = new ArrayList<>();
        for (int i = 0; i < rawExpectedCommands.size(); i++) {
            String command = rawExpectedCommands.get(i);
            if (command.trim().startsWith("if ")) {
                // 跳过if语句，不添加到预期执行命令列表中
                continue;
            }
            expectedCommands.add(command);
        }

        // 从报告中提取实际执行的命令
        List<String> actualCommands = extractCommandsFromReport(reportPath);

        // 计算执行统计信息
        int totalExpected = expectedCommands.size();
        int totalExecuted = actualCommands.size();

        result.put("预期执行总数", totalExpected);
        result.put("实际执行总数", totalExecuted);

        // 记录已匹配的命令索引
        Set<Integer> matchedExpectedIndexes = new HashSet<>();
        Set<Integer> matchedActualIndexes = new HashSet<>();

        // 存储匹配结果
        List<Map<String, Object>> matchedCommands = new ArrayList<>();
        List<String> notExecuted = new ArrayList<>();
        List<String> extraExecuted = new ArrayList<>();

        // 第一轮：尝试匹配命令
        for (int i = 0; i < expectedCommands.size(); i++) {
            String expected = expectedCommands.get(i);
            boolean found = false;

            for (int j = 0; j < actualCommands.size(); j++) {
                if (matchedActualIndexes.contains(j)) continue;

                String actual = actualCommands.get(j);

                // 检查命令是否匹配
                if (commandsMatch(expected, actual)) {
                    Map<String, Object> match = new HashMap<>();
                    match.put("expected", expected);
                    match.put("actual", actual);
                    match.put("expectedIndex", i);
                    match.put("actualIndex", j);
                    match.put("status", "EXECUTED");

                    matchedCommands.add(match);
                    matchedExpectedIndexes.add(i);
                    matchedActualIndexes.add(j);
                    found = true;
                    break;
                }
            }

            // 如果未找到匹配项，则记录为未执行
            if (!found) {
                notExecuted.add(expected);
            }
        }

        // 记录额外执行的命令
        for (int j = 0; j < actualCommands.size(); j++) {
            if (!matchedActualIndexes.contains(j)) {
                extraExecuted.add(actualCommands.get(j));
            }
        }

        // 根据原始顺序整理详细比较结果
        List<Map<String, Object>> detailedComparison = new ArrayList<>();

        // 先添加已执行和未执行的预期命令
        for (int i = 0; i < expectedCommands.size(); i++) {
            Map<String, Object> item = new HashMap<>();
            String command = expectedCommands.get(i);
            item.put("command", command);

            if (matchedExpectedIndexes.contains(i)) {
                item.put("status", "EXECUTED");

                // 查找对应的实际执行命令
                for (Map<String, Object> match : matchedCommands) {
                    if ((int)match.get("expectedIndex") == i) {
                        item.put("actualCommand", match.get("actual"));
                        break;
                    }
                }
            } else {
                item.put("status", "NOT_EXECUTED");
            }

            item.put("index", i);
            detailedComparison.add(item);
        }

        // 添加额外执行的命令
        for (String command : extraExecuted) {
            Map<String, Object> item = new HashMap<>();
            item.put("command", command);
            item.put("status", "EXTRA");
            detailedComparison.add(item);
        }

        result.put("未执行", notExecuted);
        result.put("额外执行", extraExecuted);
        result.put("未执行总数", notExecuted.size());
        result.put("额外执行总数", extraExecuted.size());
        result.put("执行率", totalExpected > 0 ? (double)(totalExpected - notExecuted.size()) / totalExpected : 0);
        result.put("详细比较", detailedComparison);

        return result;
    }

    /**
     * 检查两个命令是否匹配
     *
     * @param expected 预期命令
     * @param actual 实际命令
     * @return 如果匹配返回true，否则返回false
     */
    private static boolean commandsMatch(String expected, String actual) {
        // 将命令按空格分割
        String[] expectedParts = expected.trim().split("\\s+");
        String[] actualParts = actual.trim().split("\\s+");

        // 至少要有一个部分
        if (expectedParts.length == 0 || actualParts.length == 0) {
            return false;
        }

        // 检查第一部分是否相同（命令类型）
        if (!expectedParts[0].equals(actualParts[0])) {
            return false;
        }

        // 对于简单命令如wait、keyboard等，直接比较全部内容
        if (expectedParts[0].equals("wait") ||
            expectedParts[0].equals("keyboard") ||
            expectedParts[0].startsWith("http")) {
            return expected.equals(actual);
        }

        // 对于带路径的命令如type、click等
        if (expectedParts.length >= 2 && actualParts.length >= 2) {
            // 对于type命令，检查第三部分(as后面的值)是否匹配
            if (expectedParts[0].equals("type") && expectedParts.length >= 4 && actualParts.length >= 4) {
                return actualParts[0].equals(expectedParts[0]) &&
                       actualParts[2].equals(expectedParts[2]) && // "as"
                       actualParts[3].equals(expectedParts[3]);   // 输入值
            }

            // 对于其他命令，检查路径的文件名部分是否匹配
            String expectedFileName = getFileNameFromPath(expectedParts[1]);
            String actualFileName = getFileNameFromPath(actualParts[1]);

            return expectedFileName.equals(actualFileName);
        }

        // 默认情况下，比较完整命令
        return expected.equals(actual);
    }

    /**
     * 从路径中提取文件名
     *
     * @param path 文件路径
     * @return 文件名
     */
    private static String getFileNameFromPath(String path) {
        // 支持Windows和Unix风格的路径
        int lastSlashIndex = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        if (lastSlashIndex >= 0 && lastSlashIndex < path.length() - 1) {
            return path.substring(lastSlashIndex + 1);
        }
        return path;
    }

}
