package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.PayloadVerifierFactory;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.liferay.portal.model.User;

/**
 * It is a wrapper for any other {@link PayloadVerifier}, it avoid sending the {@link com.dotcms.api.system.event.SystemEvent}
 * for the user who fire the event. Also allow apply any other {@link PayloadVerifier},
 * for example you can use it with the {@link PermissionVerifier} in the follow way:
 *
 * <code>
 *    Payload payload = new Payload(data, Visibility.EXCLUDE_OWNER,
 *          new ExcludeOwnerVerifierBean(userId, PermissionAPI.PERMISSION_READ, Visibility.PERMISSION));
 *    APILocator.getSystemEventsAPI().push(new SystemEvent(systemEventType, payload));
 * </code>
 *
 */
public class ExcludeOwnerVerifier implements PayloadVerifier {

    final PayloadVerifierFactory factory = PayloadVerifierFactory.getInstance();

    @Override
    public boolean verified(final Payload payload, final User sessionUser) {
        ExcludeOwnerVerifierBean visibilityValue = (ExcludeOwnerVerifierBean) payload.getVisibilityValue();
        User user = sessionUser;

        if (user.getUserId().equals(visibilityValue.getUserId())){
            return false;
        }else{
            PayloadVerifier verifier = factory.getVerifier(visibilityValue.getVisibility());
            Payload newPayload = new Payload(payload.getData(), visibilityValue.getVisibility(),
                    visibilityValue.getVisibilityValue());
            return verifier.verified(newPayload, sessionUser);
        }
    }
}
