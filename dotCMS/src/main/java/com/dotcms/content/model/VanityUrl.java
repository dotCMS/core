package com.dotcms.content.model;

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
     * @return the vanity URL identifier
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
     * @return the vanity URL inode
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
     * @return the vanity URL language Id
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
     * @return the vanity URL title
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
     * @return the vanity URL site identifier
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
     * @return the vanity URL Uri
     */
    String getURI();

    /**
     * Set the Vanity URI
     *
     * @param uri the vanity URL Uri
     */
    void setURI(String uri);

    /**
     * The Vanity URL forward to path
     *
     * @return the vanity URL forward path
     */
    String getForwardTo();

    void setForwardTo(String forwardTo);

    /**
     * Get the Vanity URL action (redirect, forward or die)
     *
     * @return the vanity URL action
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
     * @return the vanity URL order
     */
    int getOrder();

    /**
     * Set the vanity URL order
     *
     * @param order the order
     */
    void setOrder(int order);

    Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;


}
