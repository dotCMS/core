package com.dotmarketing.tag.business;

import com.dotmarketing.util.web.WebDotcmsException;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public class GenericTagException extends WebDotcmsException {

    public GenericTagException(final Throwable e) {
        super(e, "tag.save.error.default");
    }

    public GenericTagException(final String msg, final Throwable e) {
        super(e, msg);
    }

}
