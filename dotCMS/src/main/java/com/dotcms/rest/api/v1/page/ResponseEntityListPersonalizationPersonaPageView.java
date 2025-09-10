package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.Pagination;

import com.dotcms.rest.api.v1.personalization.PersonalizationPersonaPageView;
import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<PersonalizationPersonaPageView> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListPersonalizationPersonaPageView extends ResponseEntityView<List<PersonalizationPersonaPageView>> {

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity) {
        super(entity);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListPersonalizationPersonaPageView(List<PersonalizationPersonaPageView> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}