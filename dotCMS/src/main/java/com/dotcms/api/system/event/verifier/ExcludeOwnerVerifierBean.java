package com.dotcms.api.system.event.verifier;

import com.dotcms.api.system.event.Visibility;
import java.io.Serializable;

/**
 * Visibility value for {@link ExcludeOwnerVerifier}
 */
public class ExcludeOwnerVerifierBean implements Serializable {
    private final String userId;
    private final Object visibilityValue;
    private final Visibility visibility;

    public ExcludeOwnerVerifierBean(final String userId,
                                    final Object visibilityValue,
                                    final Visibility visibility) {
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
