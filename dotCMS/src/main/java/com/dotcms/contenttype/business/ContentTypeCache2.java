package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.business.Cachable;
import com.dotmarketing.business.DotCacheException;

public interface ContentTypeCache2 extends Cachable {
	public final static String primaryGroup = "ContentTypeCache";
	public final static String containerStructureGroup = "ContainerStructureCache";
	public final static String MASTER_STRUCTURE = "dotMaster_Structure";
    // region names for the cache
    public final static String[] groups = { primaryGroup, containerStructureGroup,MASTER_STRUCTURE };

	void remove(ContentType type);

	void removeContainerStructures(String containerIdentifier, String containerInode);

	List<ContainerStructure> getContainerStructures(String containerIdentifier, String containerInode);

	void addContainerStructures(List<ContainerStructure> containerStructures, String containerIdentifier, String containerInode);

	void addURLMasterPattern(String pattern);

	void clearURLMasterPattern();

	String getURLMasterPattern() throws DotCacheException;

	void add(ContentType type);

    ContentType byVarOrInode(String varOrInode);

	/**
	 * Cleans the containers/structures
	 */
	void clearContainerStructures();
}
