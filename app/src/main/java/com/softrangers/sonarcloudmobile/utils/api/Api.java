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

    public static final String EXCEPTION = "exception";
    public static final String COMMAND = "command";
    public static final String DEVICE = "device";
    public static final String METHOD = "method";

    public static final String ACTION_ADD_GROUP = "com.softrangers.sonarcloudmobile.ADD_NEW_GROUP";
    public static final String ACTION_EDIT_GROUP = "com.softrangers.sonarcloudmobile.EDIT_GROUP";

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
        public static final String ORGANISATIONS = "organizations";
        public static final String RECEIVER_GROUPS = "receiverGroups";
        public static final String CREATE_GROUP = "createReceiverGroup";
        public static final String UPDATE_GROUP = "updateReceiverGroup";
        public static final String RECORDINGS = "recordings";
        public static final String DELETE_GROUP = "deleteReceiverGroup";
        public static final String SCHEDULES = "schedules";
        public static final String DELETE_SCHEDULE = "deleteSchedule";
        public static final String UPDATE_SCHEDULE = "updateSchedule";
        public static final String CREATE_SCHEDULE = "createSchedule";
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
    }

    public static class Options {
        public static final String USER_ID = "userID";
        public static final String ORGANISATION_ID = "organisationID";
        public static final String SEQ_FIELD = "seq";
    }
}
