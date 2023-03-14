package com.dotcms.rest;

import java.util.List;
import java.util.Map;

/**
 * Defines the signature of the entity view
 * @param <T>
 */
public interface EntityView <T> {

    List<ErrorEntity> getErrors();

    T getEntity();

    List<MessageEntity> getMessages() ;

    Map<String, String> getI18nMessagesMap();

    List<String> getPermissions();

    /**
     * Returns the pagination parameters associated to the current data request.
     *
     * @return The {@link Pagination} instance.
     */
    Pagination getPagination();

}
