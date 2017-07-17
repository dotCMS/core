package com.dotcms.vanityurl.model;

import com.dotmarketing.business.*;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

import java.io.Serializable;
import java.util.Map;


/**
 * Represents an Vanity URL in dotCMS. As of version 4.2.0, Vanity URL are not
 * considered as {@link VirtualLink} objects, but as {@link Contentlet} objects.
 *
 * @author oswaldogallango
 */
public interface VanityUrl extends Serializable, Versionable, Permissionable, Treeable, Ruleable {

    /**
     * Get the Vanity URL identifier
     *
     * @return the vanityurl URL identifier
     */
    String getIdentifier();

    /**
     * Set the Vanity URL identifier
     *
     * @param identifier the identifier
     */
    void setIdentifier(String identifier);

    /**
     * Get the Vanity URL inode
     *
     * @return the vanityurl URL inode
     */
    String getInode();

    /**
     * Set the Vanity URL inode
     *
     * @param inode The inode
     */
    void setInode(String inode);

    /**
     * Get the Vanity URL language Id
     *
     * @return the vanityurl URL language Id
     */
    long getLanguageId();

    /**
     * Set the Vanity URL language Id
     *
     * @param languageId The language Id
     */
    void setLanguageId(long languageId);

    /**
     * Get the Vanity URL title
     *
     * @return the vanityurl URL title
     */
    String getTitle();

    /**
     * Set the Vanity URL title
     *
     * @param title The Vanity Url title
     */
    void setTitle(String title);

    /**
     * Get the Vanity URL site identifier
     *
     * @return the vanityurl URL site identifier
     */
    String getSite();

    /**
     * Set the Vanity URL Site identifier
     *
     * @param site the Site identifier
     */
    void setSite(String site);

    /**
     * Get the Vanity URL URI
     *
     * @return the vanityurl URL Uri
     */
    String getURI();

    /**
     * Set the Vanity URI
     *
     * @param uri the vanityurl URL Uri
     */
    void setURI(String uri);

    /**
     * The Vanity URL forward to path
     *
     * @return the vanityurl URL forward path
     */
    String getForwardTo();

    void setForwardTo(String forwardTo);

    /**
     * Get the Vanity URL action (redirect, forward or die)
     *
     * @return the vanityurl URL action
     */
    int getAction();

    /**
     * Set the VAnity URL action (redirect, forward or die)
     *
     * @param action the action
     */
    void setAction(int action);

    /**
     * Get the Vanity URL order
     *
     * @return the vanityurl URL order
     */
    int getOrder();

    /**
     * Set the vanityurl URL order
     *
     * @param order the order
     */
    void setOrder(int order);

    Map<String, Object> getMap() throws DotDataException, DotSecurityException;

}
