package com.softrangers.sonarcloudmobile.utils.api;

/**
 * Created by eduard on 3/13/16.
 */
public class Api {

    public static final String URL = "woodward.parentglue.com";
    public static final int PORT = 6523;

    public static final String COMMAND = "command";
    public static final String DEVICE = "device";
    public static final String METHOD = "method";

    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    public static final String IDENTIFIER = "identifier";
    public static final String ACTION = "action";

    public static class Command {
        public static final String AUTHENTICATE = "authenticate";
        public static final String IDENTIFIER = "identifier";
    }

    public static class Method {
        public static final String USER = "user";
    }

    public static class Device {
        public static final String CLIENT = "client";
    }

    public static class Action {
        public static final String NEW = "new";
        public static final String RENEW = "renew";
    }
}
