package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Visibility;
import java.io.Serializable;

/**
 * Visibility value for {@link ExcludeOwnerVerifier}
 */
public class ExcludeOwnerVerifierBean implements Serializable {
    private final String userId;
    private final Serializable visibilityValue;
    private final Visibility visibility;

    public ExcludeOwnerVerifierBean(final String userId,
                                    final Serializable visibilityValue,
                                    final Visibility visibility) {
        this.userId = userId;
        this.visibilityValue = visibilityValue;
        this.visibility = visibility;
    }

    public String getUserId() {
        return userId;
    }

    public Serializable getVisibilityValue() {
        return visibilityValue;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
