package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.web.WebDotcmsException;

/**
 * Created by freddyrodriguez on 18/4/16.
 */
public class UnableToDeleteArchivedContentletException extends WebDotcmsException {

    UnableToDeleteArchivedContentletException(String conditionletIdentifier){
        super("message.contentlet.delete.error.archived", conditionletIdentifier);
    }
}
