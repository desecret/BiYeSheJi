package org.example.server.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.example.server.staticString.IMAGES_BASE_DIR;

public class httpRequest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = Logger.getLogger(httpRequest.class.getName());

    /**
     * 将多个图像文件上传到Flask服务器并处理响应
     *
     * @param serverUrl Flask服务器的URL
     * @param imagePath 图像文件的路径
     * @throws IOException 如果HTTP通信或处理响应时出错
     */
    public static void uploadImagesAndProcessResponse(String serverUrl, String imagePath) throws IOException {
    if (serverUrl == null || serverUrl.trim().isEmpty()) {
        throw new IllegalArgumentException("服务器URL不能为空");
    }

    if (imagePath == null || imagePath.trim().isEmpty()) {
        throw new IllegalArgumentException("图像路径不能为空");
    }

    List<String> imagePathsList;
    File pathFile = new File(imagePath);

    try {
        // Check if the path is a directory or a file
        if (pathFile.isDirectory()) {
            // If it's a directory, collect all image files from it
            logger.info("处理目录中的图片: " + imagePath);
            try (Stream<Path> pathStream = Files.walk(Paths.get(imagePath))) {
                imagePathsList = pathStream
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String fileName = path.toString().toLowerCase();
                            return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                                  fileName.endsWith(".png") || fileName.endsWith(".gif");
                        })
                        .map(Path::toString)
                        .collect(Collectors.toList());
            }
        } else if (pathFile.isFile()) {
            // If it's a file, just use this single file
            logger.info("处理单个图片文件: " + imagePath);
            String fileName = pathFile.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".gif")) {
                imagePathsList = Collections.singletonList(imagePath);
            } else {
                logger.warning("提供的文件不是支持的图片格式: " + imagePath);
                return;
            }
        } else {
            logger.warning("提供的路径既不是文件也不是目录: " + imagePath);
            return;
        }

        if (imagePathsList.isEmpty()) {
            logger.warning("未在指定路径找到图片文件: " + imagePath);
            return;
        }

        logger.info("找到 " + imagePathsList.size() + " 个图片文件准备上传");

        Map<String, Object> response = uploadImages(serverUrl, imagePathsList);

        if (response != null && response.containsKey("results")) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, Object>> results = (Map<String, Map<String, Object>>) response.get("results");
            saveImagesFromResponse(results);
            logger.info("成功处理服务器响应并保存图像");
        } else {
            logger.warning("服务器响应缺少预期的'results'字段");
        }
    } catch (NoSuchFileException e) {
        logger.log(Level.SEVERE, "指定的路径不存在: " + imagePath, e);
        throw new FileNotFoundException("指定的图片路径不存在: " + imagePath);
    } catch (IOException e) {
        logger.log(Level.SEVERE, "处理图片文件时发生I/O错误", e);
        throw e;
    }
}
    /**
     * 将多个图像上传到Flask服务器
     *
     * @param serverUrl Flask服务器的URL
     * @param imagePaths 图像文件的路径列表
     * @return 来自服务器的解析响应
     * @throws IOException 如果HTTP通信中有错误
     */
    private static Map<String, Object> uploadImages(String serverUrl, List<String> imagePaths) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(serverUrl);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            for (String imagePath : imagePaths) {
                File file = new File(imagePath);
                if (!file.exists()) {
                    logger.warning("文件不存在，跳过: " + imagePath);
                    continue;
                }

                if (file.length() == 0) {
                    logger.warning("文件大小为0，跳过: " + imagePath);
                    continue;
                }

                try {
                    builder.addBinaryBody(
                        "images",
                        file,
                        ContentType.IMAGE_JPEG,
                        file.getName()
                    );
                    logger.fine("添加文件到请求: " + file.getName());
                } catch (Exception e) {
                    logger.log(Level.WARNING, "添加文件到请求时出错: " + imagePath, e);
                }
            }

            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    logger.severe("服务器返回错误状态码: " + statusCode);
                    throw new IOException("服务器返回错误状态码: " + statusCode);
                }

                String responseBody = EntityUtils.toString(response.getEntity());
                logger.fine("收到服务器响应: " + responseBody.substring(0, Math.min(100, responseBody.length())) + "...");

                try {
                    return objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "解析JSON响应时出错", e);
                    throw new IOException("解析服务器响应时出错: " + e.getMessage());
                }
            }
        } catch (HttpHostConnectException e) {
            logger.log(Level.SEVERE, "无法连接到服务器: " + serverUrl, e);
            throw new IOException("无法连接到服务器: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "上传图像时发生错误", e);
            throw new IOException("上传图像时发生错误: " + e.getMessage(), e);
        }
    }

    /**
     * 处理服务器响应并将图像保存在适当的文件夹结构中
     *
     * @param results 来自服务器的解析响应的结果部分
     * @throws IOException 保存图像时出错
     */
    @SuppressWarnings("unchecked")
    private static void saveImagesFromResponse(Map<String, Map<String, Object>> results) throws IOException {
        if (results == null || results.isEmpty()) {
            logger.warning("没有结果需要处理");
            return;
        }

        for (Map.Entry<String, Map<String, Object>> entry : results.entrySet()) {
            String imageName = entry.getKey();
            Map<String, Object> imageData = entry.getValue();

            if (imageName == null || imageName.trim().isEmpty()) {
                logger.warning("结果中包含无效的图像名称，跳过");
                continue;
            }

            // 创建图像目录
            File imageDir = new File(IMAGES_BASE_DIR + imageName);
            if (!imageDir.exists()) {
                boolean created = imageDir.mkdirs();
                if (!created) {
                    logger.severe("无法创建目录: " + imageDir.getAbsolutePath());
                    throw new IOException("无法创建目录: " + imageDir.getAbsolutePath());
                }
            }

            // 处理裁剪结果
            List<Map<String, Object>> crops = imageData != null ?
                (List<Map<String, Object>>) imageData.get("crops") :
                Collections.emptyList();

            if (crops == null || crops.isEmpty()) {
                logger.info("图像 " + imageName + " 没有裁剪结果");
                continue;
            }

            int savedCount = 0;
            for (Map<String, Object> crop : crops) {
                try {
                    String base64Image = (String) crop.get("image");
                    if (base64Image == null || base64Image.trim().isEmpty()) {
                        logger.warning("裁剪结果缺少图像数据，跳过");
                        continue;
                    }

                    String className = (String) crop.get("class_name");
                    String text = (String) crop.get("text");
                    Integer num = (Integer) crop.get("num");

                    if (className == null) className = "unknown";
                    if (text == null) text = "notext";
                    if (num == null) num = savedCount;

                    // 清理文件名（移除无效字符）
                    String cleanText = text.replaceAll("[\\\\/:*?\"<>|]", "_");

                    // 创建文件名
                    String filename = String.format("%s_%s_%d.png", cleanText, className, num);

                    // 保存图像
                    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                    File outputFile = new File(imageDir, filename);

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(imageBytes);
                        savedCount++;
                        logger.fine("保存图像: " + outputFile.getAbsolutePath());
                    }
                } catch (IllegalArgumentException e) {
                    logger.log(Level.WARNING, "Base64解码失败", e);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "处理裁剪结果时出错", e);
                }
            }

            logger.info("为图像 " + imageName + " 保存了 " + savedCount + " 个裁剪结果");
        }
    }
}