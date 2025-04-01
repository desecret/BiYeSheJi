package org.example.server.exception;

import lombok.Getter;

@Getter
public class RuleEngineException extends RuntimeException {
    private final ErrorLevel level;

    public RuleEngineException(String message) {
        super(message);
        this.level = ErrorLevel.ERROR;
    }

    public RuleEngineException(String message, ErrorLevel level) {
        super(message);
        this.level = level;
    }

    public RuleEngineException(String message, Throwable cause) {
        super(message, cause);
        this.level = ErrorLevel.ERROR;
    }

    public RuleEngineException(String message, Throwable cause, ErrorLevel level) {
        super(message, cause);
        this.level = level;
    }

    public enum ErrorLevel {
        WARNING, ERROR, FATAL
    }
}