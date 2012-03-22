package com.dotmarketing.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;

/** 
 * 	@author Hibernate CodeGenerator
 * @author David H Torres (2009) 
 */
public class PermissionReference implements Serializable {

    private static final long serialVersionUID = 1L;

	/** persistent field */
    private long id;

    /** persistent field */
    private String assetId;

    /** persistent field */
    private String referenceId;
    
    /** persistent field */
    private String type;

    /** full constructor */
    public PermissionReference(String assetId, String referenceId) {
    	this.assetId = assetId;
        this.referenceId = referenceId;
    }

    /** default constructor */
    public PermissionReference() {
    }

    
    public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getAssetId() {
		return assetId;
	}

	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
	public boolean equals(Object other) {

        if (!(other instanceof PermissionReference)) {
            return false;
        }

        PermissionReference castOther = ( PermissionReference ) other;

        return (this.getAssetId().equals(castOther.getAssetId()) &&
        		this.getReferenceId().equals(castOther.getReferenceId()));
    }

    @Override
    public int hashCode() {
    	return this.getAssetId().hashCode() & this.getReferenceId().hashCode();
    }


	
	public Map<String, Object> getMap() {
		HashMap<String, Object> theMap = new HashMap<String, Object>();
		theMap.put("id", this.getId());
		theMap.put("assetId", this.getAssetId());
		theMap.put("referenceId", this.getReferenceId());
		return theMap;
	}

}
