package com.dotcms.publisher.business;

import java.io.Serializable;
import java.util.Date;


/**
 * Publish Audit status POJO
 * @author alberto
 *
 */
public class PublishAuditStatus implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static enum Status {
		NOT_BUNDLED(1),
		BUNDLE_REQUESTED(2),
		BUNDLING(3),
		PUBLISHING(4),
		FAILED_TO_BUNDLE(5), 
		FAILED_TO_PUBLISH(6),
		SUCCESS(7);
		
		private int code;
		private Status(int code) {
			this.code = code;
		}
		
		public int getCode() {
			return code;
		}
	}
	
	private String bundleId;
	private Status status;
	private String statusPojo; //contains the pojo serialized
	private Date statusUpdated;
	private Date createDate;
	
	public PublishAuditStatus() {}
	
	/**
	 * Build a brand new Audit Status
	 * @param bundleId
	 */
	public PublishAuditStatus(String bundleId) {
		this.bundleId = bundleId;
		this.createDate = new Date();
		this.status = Status.NOT_BUNDLED;
	}
	
	/**
	 * Useful to build a Audit Status with new status attribute
	 * from an existing status
	 * @param bundleId
	 * @param status
	 */
	public PublishAuditStatus(PublishAuditStatus origin, Status status) {
		this.bundleId = origin.getBundleId();
		this.status = status;
		this.statusUpdated = new Date();
		this.createDate = origin.createDate;
		this.statusPojo = origin.getStatusPojo(); //TODO manage status POJO
	}
	
	public static String getStatusByCode(int code) {
		for(Status status: Status.values()) {
			if(status.getCode() == code)
				return status.toString();
		}
		return "";
	}
	
	public String getBundleId() {
		return bundleId;
	}
	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}
	
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public String getStatusPojo() {
		return statusPojo;
	}
	public void setStatusPojo(String statusPojo) {
		this.statusPojo = statusPojo;
	}
	
	public Date getStatusUpdated() {
		return statusUpdated;
	}
	public void setStatusUpdated(Date statusUpdated) {
		this.statusUpdated = statusUpdated;
	}
	
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}		
}

