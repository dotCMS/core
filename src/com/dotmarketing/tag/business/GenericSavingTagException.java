package com.dotmarketing.tag.business;

import com.dotmarketing.util.web.WebDotcmsException;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public class GenericSavingTagException extends WebDotcmsException {

    public GenericSavingTagException(Throwable e) {
        super(e, "tag.save.error.default");
    }
}
