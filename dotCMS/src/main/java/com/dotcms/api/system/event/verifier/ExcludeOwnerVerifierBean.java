package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Visibility;

/**
 * Visibility value for {@link ExcludeOwnerVerifier}
 */
public class ExcludeOwnerVerifierBean {
    private String userId;
    private Object visibilityValue;
    private Visibility visibility;

    public ExcludeOwnerVerifierBean(String userId, Object visibilityValue, Visibility visibility) {
        this.userId = userId;
        this.visibilityValue = visibilityValue;
        this.visibility = visibility;
    }

    public String getUserId() {
        return userId;
    }

    public Object getVisibilityValue() {
        return visibilityValue;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
