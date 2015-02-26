package com.dotmarketing.beans;

import java.io.Serializable;

public class ContainerStructure implements Serializable{

	private static final long serialVersionUID = 1L;

	private String id;
	private String structureId;
    private String structureInode;
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

    public String getStructureInode() {
        return structureInode;
    }

    public void setStructureInode(String structureInode) {
        this.structureInode = structureInode;
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

}
