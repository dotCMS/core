package com.dotcms.api.system.event.verifier;


import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.liferay.portal.model.User;

import java.util.List;

/**
 * Verified that the sessionUser user is one of the users in the lists by user id.
 * @author jsanca
 */
public class UsersVerifier implements  PayloadVerifier{

    public UsersVerifier(){}

    @Override
    public boolean verified(final Payload payload, final User sessionUser) {

        final List<String> userIdList = (List<String>)payload.getVisibilityValue();

        return userIdList.contains(sessionUser.getUserId());
    }
}
