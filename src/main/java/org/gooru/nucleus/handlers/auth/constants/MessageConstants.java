package org.gooru.nucleus.handlers.auth.constants;

public class MessageConstants {

    public static final String MSG_HEADER_OP = "mb.operation";
    public static final String MSG_HEADER_TOKEN = "session.token";
    public static final String MSG_OP_AUTH = "auth";
    public static final String MSG_OP_STATUS = "mb.operation.status";
    public static final String MSG_OP_STATUS_SUCCESS = "success";
    public static final String MSG_OP_STATUS_ERROR = "error";

    public static final String CONFIG_SESSION_TIMEOUT_KEY = "sessionTimeoutInSeconds";
    public static final String CONFIG_REDIS_CONFIGURATION_KEY = "redisConfig";
}
