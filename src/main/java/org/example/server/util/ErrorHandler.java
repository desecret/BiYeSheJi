package org.example.server.util;

import org.example.server.exception.RuleEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    // 错误日志文件路径

    public static void handle(RuleEngineException ex) {
        switch (ex.getLevel()) {
            case WARNING:
                logger.warn(ex.getMessage());
                break;
            case ERROR:
                logger.error(ex.getMessage(), ex);
                break;
            case FATAL:
                logger.error("严重错误: {}", ex.getMessage(), ex);
                // 在生产环境可能需要发送警报或通知
                break;
        }
    }

    public static void handle(Exception ex) {
        logger.error("未捕获异常: {}", ex.getMessage(), ex);
    }

    public static void logWarning(String message) {
        logger.warn(message);
    }

    public static void logError(String message) {
        logger.error(message);
    }

    public static void logError(String message, Throwable cause) {
        logger.error(message, cause);
    }
}