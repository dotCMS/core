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
 * both inner workings of dotCMS and custom content. For example, Language Variables are a sub-type
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
    public String getIdentifier();

    /**
     * Sets the Key/Value identifier.
     * 
     * @param identifier - The Key/Value identifier.
     */
    public void setIdentifier(String identifier);

    /**
     * Returns the Key/Value inode.
     * 
     * @return The Key/Value inode.
     */
    public String getInode();

    /**
     * Sets the Key/Value inode.
     * 
     * @param inode - The Key/Value inode.
     */
    public void setInode(String inode);

    /**
     * Returns the Key/Value language ID.
     * 
     * @return The Key/Value language ID.
     */
    public long getLanguageId();

    /**
     * Sets the Key/Value language ID.
     * 
     * @param languageId - The Key/Value language ID.
     */
    public void setLanguageId(long languageId);

    /**
     * Returns the content's key.
     * 
     * @return The the content's key.
     */
    public String getKey();

    /**
     * Sets the content's key.
     * 
     * @param key - The content's key.
     */
    public void setKey(String key);

    /**
     * Returns the content's value.
     * 
     * @return The the content's value.
     */
    public String getValue();

    /**
     * Sets the content's value.
     * 
     * @param value - The content's value.
     */
    public void setValue(String value);

    /**
     * Returns the property map associated to this Key/Value. It's worth noting that many properties
     * can be set to a Key/Value object as it is also a Contentlet.
     */
    public Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;

}