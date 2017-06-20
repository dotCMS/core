package com.dotcms.content.model;

import java.io.Serializable;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Ruleable;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;

/**
 * 
 * @author Jose Castro
 * @version 4.2.0
 * @since Jun 19, 2017
 *
 */
public interface KeyValue extends Serializable, Versionable, Permissionable, Treeable, Ruleable {

	/**
	 * Get the Vanity URL identifier
	 * 
	 * @return the vanity URL identifier
	 */
	String getIdentifier();

	/**
	 * Set the Vanity URL identifier
	 * 
	 * @param identifier
	 *            the identifier
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
	 * @param inode
	 *            The inode
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
	 * @param languageId
	 *            The language Id
	 */
	void setLanguageId(long languageId);

	String getKey();

	void setKey(String key);

	String getValue();

	void setValue(String value);

	Map<String, Object> getMap() throws DotStateException, DotDataException, DotSecurityException;

}
