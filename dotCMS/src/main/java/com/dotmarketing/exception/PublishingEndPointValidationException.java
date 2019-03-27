package com.dotmarketing.exception;

import com.google.common.collect.Lists;
import java.util.List;

/**
 * {@link Exception} to be use as Validation of {@link com.dotcms.publisher.endpoint.bean.PublishingEndPoint}
 * implementations.
 */
public class PublishingEndPointValidationException extends Exception {

    List<String> i18nmessages;

    public PublishingEndPointValidationException(final String i18nmessage) {
        this.i18nmessages = Lists.newArrayList(i18nmessage);
    }

    public PublishingEndPointValidationException(final List<String> i18nmessages) {
        this.i18nmessages = i18nmessages;
    }

    public List<String> getI18nmessages() {
        if (i18nmessages == null) {
            i18nmessages = Lists.newArrayList();
        }
        return i18nmessages;
    } // getI18nmessages.
}
