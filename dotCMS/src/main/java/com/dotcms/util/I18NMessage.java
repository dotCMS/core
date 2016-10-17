package com.dotcms.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Encapsulates a i18n message
 * @author jsanca
 */
public class I18NMessage implements Serializable {


    private final String key;
    private final Object[] arguments;
    private final String defaultMessage;

    public I18NMessage(final String key) {
        this.key = key;
        this.arguments = null;
        this.defaultMessage = null;
    }

    public I18NMessage(final String key, final String defaultMessage) {
        this.key = key;
        this.arguments = null;
        this.defaultMessage = defaultMessage;
    }

    public I18NMessage(final String key, final String defaultMessage, final Object... arguments) {
        this.key = key;
        this.arguments = arguments;
        this.defaultMessage = defaultMessage;
    }



    public String getKey() {
        return key;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        I18NMessage that = (I18NMessage) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(arguments, that.arguments);

    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }

    @Override
    public String toString() {
        return "I18NMessage{" +
                "key='" + key + '\'' +
                ", arguments=" + Arrays.toString(arguments) +
                ", defaultMessage=" + defaultMessage +
                '}';
    }
} // E:O:F:I18NMessage.
