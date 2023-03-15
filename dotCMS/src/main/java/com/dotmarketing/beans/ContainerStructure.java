package com.dotmarketing.beans;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDUtil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

/**
 * Holds the relationship between a given Container and one or more Content Types in dotCMS. This represents the base
 * structure that allows Containers to be able to render with different types of contents in the repository.
 *
 * @author Daniel Silva
 * @since May 14th, 2013
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContainerStructure implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;
	private String structureId;
    private String containerInode;
    private String containerId;
    private String code;

	public ContainerStructure(){}

	/**
	 * Creates an instance of this class based on the provided Container and Content Type.
	 *
	 * @param container   The {@link Container} object that will hold the Contentlets.
	 * @param contentType The {@link ContentType} that will determine what type of objects will be held by the specified
	 *                    Container.
	 */
	public ContainerStructure(final Container container, final ContentType contentType) {
		this.setId(UUIDUtil.uuid());
		this.setStructureId(contentType.id());
		this.setContainerInode(container.getInode());
		this.setContainerId(container.getIdentifier());
		this.setCode(container.getCode());
	}

	/**
	 * Returns the ID of this Container-to-Content Type association.
	 *
	 * @return The ID of this association.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the ID of this Container-to-Content Type association. This is usually a randomly generated UUID.
	 *
	 * @param id The ID of this association.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the ID of the Container in this association.
	 *
	 * @return The Container's ID.
	 */
	public String getContainerId() {
		return containerId;
	}

	/**
	 * Sets the ID of the Container in this association.
	 *
	 * @param containerId The Container's ID.
	 */
	public void setContainerId(final String containerId) {
		this.containerId = containerId;
	}

	/**
	 * Returns the Inode of the Container in this association.
	 *
	 * @return The Container's Inode.
	 */
    public String getContainerInode() {
        return containerInode;
    }

	/**
	 * Sets the Inode of the Container in this association.
	 *
	 * @param containerInode The Container's Inode.
	 */
    public void setContainerInode(final String containerInode) {
        this.containerInode = containerInode;
    }

	/**
	 * Returns the ID of the Content Type in this association.
	 *
	 * @return The Content Type's ID.
	 */
	public String getStructureId() {
		return structureId;
	}

	/**
	 * Sets the ID of the Content Type in this association.
	 *
	 * @param contentTypeId The Content Type's ID.
	 */
	public void setStructureId(final String contentTypeId) {
		this.structureId = contentTypeId;
	}

	/**
	 * Sets the code that will be used to render the contents of the associated type in the specified Container.
	 *
	 * @return The Container's code for rendering its associated Content Type.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Returns the code that will be used to render the contents of the associated type in the specified Container.
	 *
	 * @param code The Container's code for rendering its associated Content Type.
	 */
	public void setCode(final String code) {
		this.code = code;
	}

	/**
	 * Returns the Velocity Variable Name of the Content Type in this association.
	 *
	 * @return The Velocity Variable Name of the Content Type.
	 */
	public String getContentTypeVar() {
	   try {
		  return APILocator.getContentTypeAPI(APILocator.systemUser())
		   .find(getStructureId()).variable();
	   }catch( Exception  nfdb) {
		   Logger.debug(this.getClass(), nfdb.getMessage(), nfdb);
		   return null;
	   }

	}

}
