package com.softrangers.sonarcloudmobile.utils.api;

/**
 * Created by Eduard Albu on 14 03 2016
 * project SonarCloud
 *
 * @author eduard.albu@gmail.com
 */
public class Api {

    // Server port and url
    public static final String URL = "woodward.parentglue.com";
    public static final String M_URL = "maestro.parentglue.com";
    public static final int PORT = 6523;

    //---------- Components used to build server requests ------------//
    public static final int ERROR = 0;
    public static final int RESPONSE = 1;

    public static final String COMMAND = "command";
    public static final String DEVICE = "device";
    public static final String METHOD = "method";

    public static final String REQUEST_MESSAGE = "json_request";
    public static final String CONNECTION_BROADCAST = "com.softrangers.sonarcloudmobile.CONNECTED";
    public static final String RESPONSE_BROADCAST = "com.softrangers.sonarcloudmobile.RESPONSE_BROADCAST";
    public static final String RESPONSE_MESSAGE = "json_response";

    public static final String EMAIL = "email";
    public static final String PASSWORD = "password";

    public static final String IDENTIFIER = "identifier";
    public static final String ACTION = "action";
    public static final String SECRET = "secret";


    /**
     * Contains all server commands
     */
    public static class Command {
        public static final String AUTHENTICATE = "authenticate";
        public static final String IDENTIFIER = "identifier";
        public static final String RECEIVERS = "receivers";
    }

    /**
     * Contains all server methods
     */
    public static class Method {
        public static final String USER = "user";
        public static final String IDENTIFIER = "identifier";
        public static final String GROUP_ID = "groupID";
    }

    /**
     * Contains all devices supported by server
     */
    public static class Device {
        public static final String CLIENT = "client";
    }

    /**
     * Contains all server actions
     */
    public static class Action {
        public static final String NEW = "new";
        public static final String RENEW = "renew";

        public static final String ACTION_CONNECT = "connect";
        public static final String ACTION_SEND = "send_request";
        public static final String ACTION_DISCONECT = "disconnect";
    }
}
