package com.dotcms.test.util;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public  class StringFormatterTestUtil {

    private StringFormatterTestUtil(){};

    public static String format(final String message, final Map<String, Object> arguments) {
        String formattedMessage = message;

        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            String value = null;

            if (entry.getValue() == null) {
                continue;
            }

            if (Collection.class.isInstance(entry.getValue())){
                value = ((Collection) entry.getValue())
                        .stream()
                        .map(item -> "\"" + item + "\"")
                        .collect(Collectors.joining(",")).toString();
            }else {
                value =  entry.getValue().toString();
            }


            formattedMessage = formattedMessage.replaceAll(String.format("__%s__", entry.getKey()), value);
        }

        return formattedMessage;
    }
}
