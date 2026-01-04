package com.atparui.rms.service.dto;

/**
 * Result of database connection test.
 */
public class DatabaseConnectionTestResult {

    private boolean success;
    private String message;
    private String errorDetails;
    private Long connectionTimeMs;

    public DatabaseConnectionTestResult() {}

    public DatabaseConnectionTestResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public DatabaseConnectionTestResult(boolean success, String message, String errorDetails) {
        this.success = success;
        this.message = message;
        this.errorDetails = errorDetails;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Long getConnectionTimeMs() {
        return connectionTimeMs;
    }

    public void setConnectionTimeMs(Long connectionTimeMs) {
        this.connectionTimeMs = connectionTimeMs;
    }
}
