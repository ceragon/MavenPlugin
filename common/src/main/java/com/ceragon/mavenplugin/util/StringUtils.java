package com.ceragon.mavenplugin.util;

import org.apache.logging.log4j.message.ParameterizedMessageFactory;

public class StringUtils extends org.apache.commons.lang3.StringUtils {
    private final static ParameterizedMessageFactory factory = ParameterizedMessageFactory.INSTANCE;

    public static String format(String message, Object... params) {
        return factory.newMessage(message, params).getFormattedMessage();
    }
}
