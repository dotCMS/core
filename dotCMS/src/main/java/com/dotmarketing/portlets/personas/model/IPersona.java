package com.dotmarketing.portlets.personas.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.Versionable;

/**
 * Personas new content type. More info at the links:
 * https://www.lucidchart.com/documents/view/1a7689c0-2f8d-4a76-925d-387ea5ce3a5b/1
 * https://docs.google.com/document/d/1eJ-uwoZzHVPigbRMS6iZ1HKCUOOIFkZHumYRNvSv0T0/edit
 * 
 * @author Erick Gonzalez
 * @version 1.1
 * @since 08-03-2014
 *
 */

public interface IPersona extends Serializable, Versionable, Permissionable{

	String getIdentifier();
	void setIdentifier(String identifier);
	
	String getInode();
	void setInode(String inode);
	
	String getName();
	void setName(String name);
	
	String getKeyTag();
	void setKeyTag(String keyTag);
	
	String getDescription();
	void setDescription(String description);

	String getTags();
	void setTags(List<String> tags);
}