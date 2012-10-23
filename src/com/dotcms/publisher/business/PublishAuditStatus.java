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
		BUNDLE_REQUESTED(1),
		BUNDLING(2),
		PUBLISHING(3),
		FAILED_TO_BUNDLE(4), 
		FAILED_TO_PUBLISH(5),
		SUCCESS(6);
		
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
	private PublishAuditHistory statusPojo;
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
		this.status = Status.BUNDLE_REQUESTED;
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
	
	public PublishAuditHistory getStatusPojo() {
		return statusPojo;
	}
	public void setStatusPojo(PublishAuditHistory statusPojo) {
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

