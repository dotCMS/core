package com.dotcms.publisher.business;

import java.util.Date;

/**
 * Represents an entry in the <code>publishing_queue</code> table. A record in
 * this table is an object that is marked for Push Publishing, which means it
 * will be included in a bundle and sent over to a destination server.
 * 
 * @author Alberto
 * @version 1.0
 * @since Nov 5, 2012
 *
 */
public class PublishQueueElement {

	private Integer id;
	private Integer operation;
	private String asset;
	private Date enteredDate;
	private Date publishDate;
	private String bundleId;
	private Integer languageId;
	private String type;

	/**
	 * Returns the id of the asset in the queue.
	 * 
	 * @return The asset ID in the queue.
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * Sets the id of the asset in the queue.
	 * @param id - The asset ID in the queue.
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * Returns the type of operation set for this asset. There are 2 options:
	 * ADD_OR_UPDATE_ELEMENT (1), and DELETE_ELEMENT (2).
	 * 
	 * @return The operation type.
	 */
	public Integer getOperation() {
		return operation;
	}

	/**
	 * Sets the type of operation set for this asset. There are 2 options:
	 * ADD_OR_UPDATE_ELEMENT (1), and DELETE_ELEMENT (2).
	 * 
	 * @param operation
	 *            - The operation type.
	 */
	public void setOperation(Integer operation) {
		this.operation = operation;
	}

	/**
	 * Returns the asset Identifier or ID.
	 * 
	 * @return The asset Identifier or ID.
	 */
	public String getAsset() {
		return asset;
	}

	/**
	 * Sets the asset Identifier or ID.
	 * 
	 * @param asset
	 *            - The asset Identifier or ID.
	 */
	public void setAsset(String asset) {
		this.asset = asset;
	}

	/**
	 * Returns the date this asset was added to the publishing queue.
	 * 
	 * @return The date this asset was added to the queue.
	 */
	public Date getEnteredDate() {
		return enteredDate;
	}

	/**
	 * Sets the date this asset was added to the publishing queue.
	 * 
	 * @param enteredDate
	 *            - The date this asset was added to the queue.
	 */
	public void setEnteredDate(Date enteredDate) {
		this.enteredDate = enteredDate;
	}

	/**
	 * Returns the specified date when this asset will be published.
	 * 
	 * @return The publishing date.
	 */
	public Date getPublishDate() {
		return publishDate;
	}

	/**
	 * Sets the specified date when this asset will be published.
	 * 
	 * @param publishDate
	 *            - The publishing date.
	 */
	public void setPublishDate(Date publishDate) {
		this.publishDate = publishDate;
	}

	/**
	 * Returns the ID of the bundle that will contain this asset.
	 * 
	 * @return The bundle ID.
	 */
	public String getBundleId() {
		return bundleId;
	}

	/**
	 * Sets the ID of the bundle that will contain this asset.
	 * 
	 * @param bundleId
	 *            - The bundle ID.
	 */
	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	/**
	 * @deprecated This property is not currently being used.
	 */
	public Integer getLanguageId() {
		return languageId;
	}

	/**
	 * @deprecated This property is not currently being used.
	 */
	public void setLanguageId(Integer languageId) {
		this.languageId = languageId;
	}

	/**
	 * Returns the type of this asset: "structure", "user", "osgi", "category",
	 * etc.
	 * 
	 * @return The asset type.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Sets the type of this asset: "structure", "user", "osgi", "category",
	 * etc.
	 * 
	 * @param type
	 *            - The asset type.
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "PublishQueueElement [id=" + id + ", operation=" + operation + ", asset=" + asset + ", enteredDate="
				+ enteredDate + ", publishDate=" + publishDate + ", bundleId=" + bundleId + ", languageId=" + languageId
				+ ", type=" + type + "]";
	}

}
