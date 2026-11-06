package com.dotcms.rest.api.v1.authentication;

import com.dotcms.rest.ResponseEntityMapMapView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/** Session metadata is separate from the users' saved profile preferences. */
public class ResponseEntitySessionUsersView extends ResponseEntityMapMapView {

    private final String sessionLanguageId;

    public ResponseEntitySessionUsersView(final Map<String, Map<String, Object>> users,
                                         final String sessionLanguageId) {
        super(users);
        this.sessionLanguageId = sessionLanguageId;
    }

    @Schema(description = "Effective admin session locale, or null when no session locale is set",
            accessMode = Schema.AccessMode.READ_ONLY)
    public String getSessionLanguageId() {
        return sessionLanguageId;
    }
}
