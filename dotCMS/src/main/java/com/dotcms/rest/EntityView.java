package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Defines the signature of the entity view
 * @param <T>
 */
public interface EntityView <T> {

    public List<ErrorEntity> getErrors();

    public T getEntity();

    public List<MessageEntity> getMessages() ;

    public Map<String, String> getI18nMessagesMap();

    public List<String> getPermissions();
}
