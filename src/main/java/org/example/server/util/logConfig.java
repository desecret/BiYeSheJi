package org.example.server.util;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class logConfig {

    public static void configureLogging() {
        // 设置 Easy Rules 的日志级别为 INFO
        Logger easyRulesLogger = (Logger) LoggerFactory.getLogger("org.jeasy.rules");
        easyRulesLogger.setLevel(Level.INFO);

        // 设置根日志级别为 INFO
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.INFO);
    }
}
