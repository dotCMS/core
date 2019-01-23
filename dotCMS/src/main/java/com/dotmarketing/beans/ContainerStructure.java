package com.dotmarketing.beans;

import java.io.Serializable;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Logger;

public class ContainerStructure implements Serializable{

	private static final long serialVersionUID = 1L;

	private String id;
	private String structureId;
    private String containerInode;
    private String containerId;
    private String code;

	public ContainerStructure(){}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}

    public String getContainerInode() {
        return containerInode;
    }

    public void setContainerInode(String containerInode) {
        this.containerInode = containerInode;
    }

	public String getStructureId() {
		return structureId;
	}

	public void setStructureId(String structureId) {
		this.structureId = structureId;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
	
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
