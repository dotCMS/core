package com.dotmarketing.tag.business;

import com.dotmarketing.util.web.WebDotcmsException;

/**
 * Created by freddyrodriguez on 17/3/16.
 */
public class TagAlreadyExistsException extends WebDotcmsException {

    public TagAlreadyExistsException(String tagName) {
        super("tag-for-host-already-exists", tagName);
    }
}
