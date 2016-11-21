package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Payload;
import com.dotcms.api.system.event.PayloadVerifier;
import com.dotcms.api.system.event.PayloadVerifierFactory;
import com.dotcms.rest.api.v1.system.websocket.SessionWrapper;
import com.liferay.portal.model.User;


public class ExcludeOwnerVerifier implements PayloadVerifier {

    final PayloadVerifierFactory factory = PayloadVerifierFactory.getInstance();

    @Override
    public boolean verified(final Payload payload, final SessionWrapper session) {
        ExcludeOwnerVerifierBean visibilityValue = (ExcludeOwnerVerifierBean) payload.getVisibilityValue();
        User user = session.getUser();

        if (user.getUserId().equals(visibilityValue.getUserId())){
            return false;
        }else{
            PayloadVerifier verifier = factory.getVerifier(visibilityValue.getVisibility());
            Payload newPayload = new Payload(payload.getData(), visibilityValue.getVisibility(),
                    visibilityValue.getVisibilityValue());
            return verifier.verified(newPayload, session);
        }
    }
}
