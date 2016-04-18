package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.util.web.WebDotcmsException;

/**
 * Created by freddyrodriguez on 18/4/16.
 */
public class ImpossibleDeleteArchivedContentletException extends WebDotcmsException {

    ImpossibleDeleteArchivedContentletException(String conditionletIdentifier){
        super("message.contentlet.delete.error.archived", conditionletIdentifier);
    }
}
