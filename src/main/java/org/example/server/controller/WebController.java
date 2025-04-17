package org.example.server.controller;

import org.example.server.Init;
import org.example.server.config.ElementConfig;
import org.example.server.config.ElementMappings;
import org.example.server.locator.ElementLocatorFactory;
import org.example.server.service.ElementConfigService;
import org.example.server.service.TestCaseService;
import org.example.server.util.util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import java.util.*;

import static org.example.server.staticString.*;
import static org.example.server.util.httpRequest.uploadImagesAndProcessResponse;
import static org.example.server.util.util.*;
import org.example.server.model.TestReport;
import org.example.server.model.TestStep;

/**
 * Web控制器，提供自动化测试系统的API接口
 * <p>
 * 该控制器提供了测试用例管理、元素库访问和测试用例执行等功能的RESTful接口
 * </p>
 */
@RestController
@RequestMapping("/api")
public class WebController {

    @Autowired
    private ElementConfigService elementConfigService;
    @Autowired
    private TestCaseService testCaseService;

    /**
     * 构造函数
     * 
     * @param init 初始化对象，用于系统启动
     */
    public WebController(Init init) {
    }

    /**
     * 获取所有可用的动作类型列表
     *
     * @return 动作类型列表
     */
    @GetMapping("/actions")
    public List<String> getActions() {
        List<String> actions = new ArrayList<>();
        actions.add("全屏");
        actions.add("点击");
        actions.add("右键点击");
        actions.add("双击");
        actions.add("鼠标移动到");
        actions.add("在当前鼠标位置，按下鼠标左键");
        actions.add("在当前鼠标位置，弹起鼠标左键");
        actions.add("等待");
        actions.add("设置自动等待");
        actions.add("在");
        actions.add("访问");
        actions.add("出现");
        actions.add("存在");
        actions.add("输出");
        actions.add("如果");
        return actions;
    }

    /**
     * 获取所有测试用例列表
     * <p>
     * 该接口会扫描测试用例目录下的所有.txt文件，返回测试用例ID和名称
     * </p>
     * 
     * @return 测试用例列表，包含id和name属性
     */
    @GetMapping("/testcases")
    public List<Map<String, Object>> getTestCases() {
//        List<Map<String, Object>> testCases = new ArrayList<>();
//        File casesDir = new File(CASES_DIR);
//        if (casesDir.exists() && casesDir.isDirectory()) {
//            File[] files = casesDir.listFiles((dir, name) -> name.endsWith(".txt"));
//            if (files != null) {
//                for (File file : files) {
//                    Map<String, Object> testCase = new HashMap<>();
//                    testCase.put("id", file.getName());
//                    testCase.put("name", file.getName().replace(".txt", ""));
//                    testCases.add(testCase);
//                }
//            }
//        }
//        return testCases;
        return testCaseService.getAllTestCases();
    }

    /**
     * 根据id获取测试用例详情
     * <p>
     * 读取指定测试用例文件并解析其中的命令步骤，返回结构化的步骤列表
     * </p>
     * 
     * @param id 测试用例ID（文件名）
     * @return 包含测试用例步骤和状态的Map，steps包含步骤列表，success表示是否成功
     */
    @GetMapping("/testcases/{id}")
    public ResponseEntity<Map<String, Object>> getTestCaseContent(@PathVariable String id) {
            Map<String, Object> testCase = testCaseService.getTestCaseById(Long.valueOf(id));
            if (testCase != null) {
                return ResponseEntity.ok(testCase);
            } else {
                return ResponseEntity.notFound().build();
            }
    }

    /**
     * 保存测试用例
     * <p>
     * 将前端提交的测试步骤保存为标准格式的测试用例文件
     * </p>
     * 
     * @param request 包含测试用例名称和步骤的请求体
     */
    @PostMapping("/testcases")
    public ResponseEntity<Map<String, Object>> saveTestCase(@RequestBody Map<String, Object> request) {
        Long id = request.get("id") != null ? Long.valueOf(request.get("id").toString()) : null;
        String name = (String) request.get("name");
        String originalName = (String) request.get("originalName");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) request.get("steps");

        if (name == null || steps == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "缺少必要的参数：name 或 steps");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = testCaseService.saveTestCase(id, name, originalName, steps);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 删除测试用例
     *
     * @param id 测试用例ID
     * @return 删除结果
     */
    @DeleteMapping("/testcases/{id}")
    public ResponseEntity<Map<String, Object>> deleteTestCase(@PathVariable Long id) {
        Map<String, Object> result = testCaseService.deleteTestCase(id);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * 执行测试用例
     * <p>
     * 接收前端提交的测试步骤，将其转换为标准命令格式并执行，返回执行结果日志
     * </p>
     *
     * @param request 包含测试步骤的请求体
     * @return 测试执行日志
     */
    @PostMapping("/run")
    public Map<String, Object> runTestCase(@RequestBody Map<String, Object> request) {
        List<Map<String, Object>> steps = (List<Map<String, Object>>) request.get("steps");
        List<String> logs = new ArrayList<>();
        StringBuilder testCases = new StringBuilder();
        Map<String, Object> finalResult = new HashMap<>();


        for (Map<String, Object> step : steps) {
            String type = (String) step.get("type");
            String content = (String) step.get("content");

            if (type.equals("全屏")) {
                testCases.append("全屏\n");
            } else {
                testCases.append(type).append(content).append("\n");
            }


        }
        try {
            List<String> commands = Init.runTestCase(testCases.toString());
            if (commands != null) {
                logs.add("执行命令: " + commands);
            }

            // 执行命令文件
            String res;
            Map<String, Object> result = util.runTaguiCommand(TAGUI_SCRIPT_PATH);

            if ((Boolean) result.get("success")) {
                res = "详细输出：" + result.get("output") + "\n\n测试通过";
            } else {
                res = "详细输出：" + result.get("output") + "\n\n测试失败：" + result.get("error");
            }
            logs.add("执行结果：\n" + res);
            System.out.println("执行结果: " + res);

            finalResult.put("success", true);

        } catch (Exception e) {
            logs.add("错误: " + e.getMessage());
            finalResult.put("success", false);
        }
        finalResult.put("logs", logs);
        return finalResult;
    }


    /*
     ************************************************元素库相关接口**************************************************
     */

    /**
     * 获取UI元素库
     * <p>
     * 从YAML配置文件中读取元素映射配置，返回所有UI元素的列表
     * </p>
     *
     * @return UI元素配置列表
     */
    @GetMapping("/elements")
    public List<ElementConfig> getElements() {
        return elementConfigService.getAllElements();
    }

    /**
     * 获取按context分组的元素列表
     *
     * @return 按context分组的元素列表
     */
    @GetMapping("/elements/grouped")
    public Map<String, List<ElementConfig>> getGroupedElements() {
        try {
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);

            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));
            if (mappings != null && mappings.getConfigs() != null) {
                Map<String, List<ElementConfig>> groupedElements = new HashMap<>();
                for (ElementConfig element : mappings.getConfigs()) {
                    groupedElements.computeIfAbsent(element.getContext(), k -> new ArrayList<>()).add(element);
                }
                return groupedElements;
            }
            return new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
//        List<ElementConfig> allElements = elementConfigService.getAllElements();
//        Map<String, List<ElementConfig>> groupedElements = new HashMap<>();
//
//        for (ElementConfig element : allElements) {
//            String context = element.getContext();
//            groupedElements.computeIfAbsent(context, k -> new ArrayList<>())
//                    .add(element);
//        }
//
//        return groupedElements;
    }

    /**
     * 处理图片上传，识别UI元素并更新元素库
     * <p>
     * 该接口接收上传的图片，调用图像处理服务识别图片中的UI元素，
     * 更新元素库配置文件，并返回处理结果
     * </p>
     *
     * @param image 上传的图片文件
     * @return 处理结果，包含成功状态和识别的元素列表
     */
    @PostMapping(value = "/upload/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("image") MultipartFile image) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 确保上传目录存在
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            // 保存上传的图片到临时文件
            String filename = image.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR, filename);
            Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 调用图像处理服务识别UI元素
            uploadImagesAndProcessResponse(url, String.valueOf(filePath));
            response.put("success", true);
            writeToYaml();

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "图片处理失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 获取元素对应的图片路径映射
     * <p>
     * 返回每个元素ID和其对应的图片URL或文件路径的映射关系
     * </p>
     * 
     * @return 包含元素标识符和图片路径的映射
     */
    @GetMapping("/elements/images")
    public Map<String, String> getElementImages() {
        Map<String, String> imageMap = new HashMap<>();

        try {
            // 获取元素库
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);
            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));

            if (mappings != null && mappings.getConfigs() != null) {
                for (ElementConfig element : mappings.getConfigs()) {
                    String elementKey = element.getContext() + "/" + element.getName();

                    // 查找元素对应的图片
                    String imagePath = findElementImage(element);
                    if (imagePath != null) {
                        imageMap.put(elementKey, imagePath);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageMap;
    }

    /**
     * 更新元素信息
     * <p>
     * 接收前端提交的元素更新信息，更新YAML配置文件中的元素定义
     * </p>
     * 
     * @param elementUpdate 包含元素原始信息和更新信息的请求体
     * @return 更新结果，包含成功状态和可能的错误信息
     */
    @PostMapping("/elements/update")
    public Map<String, Object> updateElement(@RequestBody Map<String, String> elementUpdate) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取原始元素信息和更新后的信息
            String originalContext = elementUpdate.get("originalContext");
            String originalName = elementUpdate.get("originalName");
            String newContext = elementUpdate.get("context");
            String newName = elementUpdate.get("name").replace("_", " ");
            String selector = elementUpdate.get("cssSelector");
            String xpath = elementUpdate.get("xpath");

            // 读取YAML文件中的元素配置
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);
            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));

            boolean updated = false;
            if (mappings != null && mappings.getConfigs() != null) {
                for (ElementConfig element : mappings.getConfigs()) {
                    if (element.getContext().equals(originalContext) &&
                            element.getName().equals(originalName)) {
                        // 更新元素信息
                        element.setContext(newContext);
                        element.setName(newName);
                        element.setCssSelector(selector);
                        element.setXpath(xpath);

                        String newImagePath = "images/" + newContext + "/" + elementUpdate.get("name") + ".png";

                        // 如果名称变了，要更改文件名以及yaml的imagePath
                        if (!newName.equals(originalName)) {
                            Path oldImagePath = Paths.get(resourcesPath, element.getImagePath());
                            // 将原始名称的图片文件重命名为新名称
                            String newPath = oldImagePath.getParent().resolve(elementUpdate.get("name") + ".png").toString();
                            Files.move(oldImagePath, Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
                            // 更新元素的图片路径
                            element.setImagePath(newImagePath);
                        }

                        if (!newContext.equals(originalContext)) {
                            // 如果上下文变了，图片的位置要变
                            String oldImagePath = element.getImagePath();

                            // // 构造新的图片路径
                            // String filename = oldImagePath.substring(oldImagePath.lastIndexOf('/') + 1);
                            // newImagePath = "images/" + newContext + "/" + filename;

                            // 确保目标目录存在
                            File targetDir = new File(IMAGES_DIR + "/" + newContext);
                            if (!targetDir.exists()) {
                                targetDir.mkdirs();
                            }

                            // 移动文件
                            File sourceFile = new File(IMAGES_DIR + "/" + oldImagePath.substring(7)); // 去掉"images/"前缀
                            File targetFile = new File(IMAGES_DIR + "/" + newImagePath.substring(7));

                            if (sourceFile.exists()) {
                                try {
                                    Files.move(sourceFile.toPath(), targetFile.toPath(),
                                            StandardCopyOption.REPLACE_EXISTING);
                                    // 更新元素的图片路径
                                    element.setImagePath(newImagePath);
                                } catch (IOException e) {
                                    throw new RuntimeException("移动图片文件失败: " + e.getMessage(), e);
                                }
                            }
                        }
                        updated = true;
                        break;
                    }
                }

                if (updated) {
                    // 写回YAML文件
                    saveElementMappings(mappings);
                    ElementLocatorFactory.init(); // 更新全局配置
                    response.put("success", true);
                } else {
                    response.put("success", false);
                    response.put("error", "未找到指定元素");
                }
            } else {
                response.put("success", false);
                response.put("error", "元素配置为空");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * 删除元素
     * <p>
     * 从YAML配置文件中删除指定元素
     * </p>
     * 
     * @param elementInfo 包含要删除元素的上下文和名称的请求体
     * @return 删除结果，包含成功状态和可能的错误信息
     */
    @PostMapping("/elements/delete")
    public Map<String, Object> deleteElement(@RequestBody Map<String, String> elementInfo) {
        Map<String, Object> response = new HashMap<>();

        try {
            String context = elementInfo.get("context");
            String name = elementInfo.get("name");

            // 读取YAML文件中的元素配置
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);
            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));

            boolean deleted = false;
            String imagePathToDelete = null;

            if (mappings != null && mappings.getConfigs() != null) {
                Iterator<ElementConfig> iterator = mappings.getConfigs().iterator();
                while (iterator.hasNext()) {
                    ElementConfig element = iterator.next();
                    if (element.getContext().equals(context) &&
                            element.getName().equals(name)) {
                        // 保存图片路径以便后续删除
                        imagePathToDelete = element.getImagePath();
                        // 删除元素
                        iterator.remove();
                        deleted = true;
                        break;
                    }
                }

                if (deleted) {
                    // 写回YAML文件
                    saveElementMappings(mappings);
                    // 删除图片文件
                    if (imagePathToDelete != null) {
                        // 获取图片文件的完整路径
                        Path imagePath = Paths.get(resourcesPath, imagePathToDelete);
                        try {
                            Files.deleteIfExists(imagePath);
                            System.out.println("已删除图片文件: " + imagePath);
                        } catch (IOException e) {
                            System.err.println("删除图片文件失败: " + e.getMessage());
                            // 即使图片删除失败，我们仍然认为元素删除成功
                        }
                    }
                    ElementLocatorFactory.init(); // 更新全局配置
                    response.put("success", true);
                } else {
                    response.put("success", false);
                    response.put("error", "未找到指定元素");
                }
            } else {
                response.put("success", false);
                response.put("error", "元素配置为空");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    /**
     * 获取元素图片
     * <p>
     * 根据请求参数返回指定元素的图片资源
     * </p>
     *
     * @param path 图片路径
     * @return 图片资源
     */
    @GetMapping("/elements/image")
    public ResponseEntity<Resource> getElementImage(@RequestParam String path) {
        try {
            // 日志记录请求路径
            System.out.println("请求的图片路径: " + path);

            Path imagePath;
            // 检查是否包含 resources 目录
            if (path.contains("resources")) {
                imagePath = Paths.get(path);
            } else {
                // 根据实际结构调整路径
                imagePath = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", path);
            }

            System.out.println("完整图片路径: " + imagePath);

            Resource resource = new FileSystemResource(imagePath.toFile());

            if (resource.exists()) {
                // 根据文件扩展名确定媒体类型
                String contentType = determineContentType(path);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                System.err.println("图片不存在: " + imagePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace(); // 添加日志以便调试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 更新元素图片
     * <p>
     * 接收上传的图片文件，更新指定元素的图片路径
     * </p>
     *
     * @param image 上传的图片文件
     * @param context 元素上下文
     * @param name 元素名称
     * @return 更新结果，包含成功状态和可能的错误信息
     */
    @PostMapping("/elements/update-image")
    public ResponseEntity<Map<String, Object>> updateElementImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("context") String context,
            @RequestParam("name") String name) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 读取YAML文件中的元素配置
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);
            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));

            boolean updated = false;
            if (mappings != null && mappings.getConfigs() != null) {
                for (ElementConfig element : mappings.getConfigs()) {
                    if (element.getContext().equals(context) &&
                            element.getName().equals(name.replace("_", " "))) {
                        // 更新元素信息
                        String filename = image.getOriginalFilename();

                        String imagePath = element.getImagePath();

                        // 生成新的图片路径，保留原始目录但使用新文件名
                        String directory = imagePath.substring(0, imagePath.lastIndexOf("/") + 1);
                        String newImagePath = directory + name + ".png";
                        element.setImagePath(newImagePath);

                        // 使用新文件路径保存上传的图片
                        Path filePath = Paths.get(resourcesPath, newImagePath);
                        Files.createDirectories(filePath.getParent()); // 确保目录存在
                        Files.copy(image.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                        updated = true;
                        break;
                    }
                }

                if (updated) {
                    // 写回YAML文件
                    saveElementMappings(mappings);
                    ElementLocatorFactory.init(); // 更新全局配置
                    response.put("success", true);
                } else {
                    response.put("success", false);
                    response.put("error", "未找到指定元素");
                }
            } else {
                response.put("success", false);
                response.put("error", "元素配置为空");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);

    }

    /**
     * 创建新元素
     * <p>
     * 将新元素添加到YAML配置文件中
     * </p>
     * 
     * @param elementData 包含新元素数据的请求体
     * @return 创建结果，包含成功状态和可能的错误信息
     */
    @PostMapping("/elements/create")
    public Map<String, Object> createElement(@RequestBody Map<String, Object> elementData) {
        Map<String, Object> response = new HashMap<>();

        try {
            MultipartFile image = (MultipartFile) elementData.get("image");
            String context = (String) elementData.get("context");
            String name = ((String) elementData.get("name")).replace("_", " ");
            String selector = (String) elementData.get("cssSelector");
            String xpath = (String) elementData.get("xpath");
            String locatorType = elementData.get("locatorType") != null ? (String) elementData.get("locatorType") : "image";

            // 读取YAML文件中的元素配置
            Constructor constructor = new Constructor(ElementMappings.class);
            Yaml yaml = new Yaml(constructor);
            ElementMappings mappings = yaml.load(Files.newInputStream(Paths.get(YAML_FILE)));

            // 检查元素是否已存在
            boolean exists = false;
            if (mappings != null && mappings.getConfigs() != null) {
                for (ElementConfig element : mappings.getConfigs()) {
                    if (element.getContext().equals(context) && element.getName().equals(name)) {
                        exists = true;
                        break;
                    }
                }
            }

            if (exists) {
                response.put("success", false);
                response.put("error", "元素已存在");
                return response;
            }

            // 创建新元素
            ElementConfig newElement = new ElementConfig();
            newElement.setContext(context);
            newElement.setName(name);
            newElement.setCssSelector(selector);
            newElement.setXpath(xpath);
            newElement.setLocatorType(locatorType);

            // 默认图片路径，后续可通过update-image更新
            String defaultImagePath = "images/" + context + "/" + name + ".png";
            newElement.setImagePath(defaultImagePath);

            // 确保目标目录存在
            File targetDir = new File(IMAGES_DIR + "/" + context);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            // 添加到映射列表
            if (mappings.getConfigs() == null) {
                mappings.setConfigs(new ArrayList<>());
            }
            mappings.getConfigs().add(newElement);

            // 写回YAML文件
            saveElementMappings(mappings);
            ElementLocatorFactory.init(); // 更新全局配置

            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

}