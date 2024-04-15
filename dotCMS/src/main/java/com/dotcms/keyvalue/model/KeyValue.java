package com.dotcms.keyvalue.model;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents a content of type Key/Value in the system. These types of contents are very useful for
 * both inner workings of dotCMS and custom content. For example, Language Variables are a subtype
 * of Key/Value objects.
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public interface KeyValue extends Serializable, Versionable, Permissionable, Treeable, Ruleable {

    /**
     * Returns the Key/Value identifier.
     * 
     * @return The Key/Value identifier
     */
    String getIdentifier();

    /**
     * Returns the Key/Value inode.
     * 
     * @return The Key/Value inode.
     */
    String getInode();

    /**
     * Returns the Key/Value language ID.
     * 
     * @return The Key/Value language ID.
     */
    long getLanguageId();

    /**
     * Returns the content's key.
     * 
     * @return The content's key.
     */
    String getKey();

    /**
     * Returns the content's value.
     * 
     * @return The content's value.
     */
    String getValue();

    /**
     * Returns the property map associated to this Key/Value. It's worth noting that many properties
     * can be set to a Key/Value object as it is also a Contentlet.
     */
    Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;

}