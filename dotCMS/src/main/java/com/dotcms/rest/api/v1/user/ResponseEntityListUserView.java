package com.dotcms.rest.api.v1.user;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.Pagination;
import com.liferay.portal.model.User;
import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<User> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListUserView extends ResponseEntityView<List<User>> {

    public ResponseEntityListUserView(List<User> entity) {
        super(entity);
    }

    public ResponseEntityListUserView(List<User> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListUserView(List<User> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListUserView(List<User> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListUserView(List<User> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListUserView(List<User> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListUserView(List<User> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListUserView(List<User> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListUserView(List<User> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListUserView(List<User> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}