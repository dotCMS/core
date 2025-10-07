package com.dotcms.rest.api.v1.template;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.MessageEntity;
import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.Pagination;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.List;
import java.util.Map;

/**
 * ResponseEntityView for List<Template> responses.
 * Provides proper type information for OpenAPI/Swagger documentation.
 */
public class ResponseEntityListTemplateView extends ResponseEntityView<List<Template>> {

    public ResponseEntityListTemplateView(List<Template> entity) {
        super(entity);
    }

    public ResponseEntityListTemplateView(List<Template> entity, Pagination pagination) {
        super(entity, pagination);
    }

    public ResponseEntityListTemplateView(List<Template> entity, String... permissions) {
        super(entity, permissions);
    }

    public ResponseEntityListTemplateView(List<Template> entity, Map<String, String> i18nMessagesMap) {
        super(entity, i18nMessagesMap);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<MessageEntity> messages) {
        super(entity, messages);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, messages, i18nMessagesMap);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<ErrorEntity> errors, List<MessageEntity> messages) {
        super(entity, errors, messages);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap) {
        super(entity, errors, messages, i18nMessagesMap);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions) {
        super(entity, errors, messages, i18nMessagesMap, permissions);
    }

    public ResponseEntityListTemplateView(List<Template> entity, List<ErrorEntity> errors, List<MessageEntity> messages, Map<String, String> i18nMessagesMap, List<String> permissions, Pagination pagination) {
        super(entity, errors, messages, i18nMessagesMap, permissions, pagination);
    }
}