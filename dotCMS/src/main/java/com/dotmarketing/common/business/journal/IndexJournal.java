package com.dotmarketing.common.business.journal;

public class IndexJournal<T> {

	private long id;
	private T inodeToIndex;
	private T identToIndex;
	private long priority;
	private boolean delete;
	private String serverId;
	private int count;
	
	public IndexJournal() {
	}
	
	public IndexJournal(long id, T objectToIndex, long priority) {
		this.id = id;
		this.identToIndex = objectToIndex;
		this.priority = priority;
	}
	
	public IndexJournal(String serverId,int count,long priority){
		this.serverId = serverId;
		this.count = count;
		this.priority = priority;
	}
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the priority
	 */
	public long getPriority() {
		return priority;
	}
	/**
	 * @param priority the priority to set
	 */
	public void setPriority(long priority) {
		this.priority = priority;
	}

	/**
	 * @return the delete
	 */
	public boolean isDelete() {
		return delete;
	}

	/**
	 * @param delete the delete to set
	 */
	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	/**
	 * @return the inodeToIndex
	 */
	public T getInodeToIndex() {
		return inodeToIndex;
	}

	/**
	 * @param inodeToIndex the inodeToIndex to set
	 */
	public void setInodeToIndex(T inodeToIndex) {
		this.inodeToIndex = inodeToIndex;
	}

	/**
	 * @return the identToIndex
	 */
	public T getIdentToIndex() {
		return identToIndex;
	}

	/**
	 * @param identToIndex the identToIndex to set
	 */
	public void setIdentToIndex(T identToIndex) {
		this.identToIndex = identToIndex;
	}
	/**
	 * @return the serverId
	 */
	public String getServerId() {
		return serverId;
	}
	/**
	 * @param serverId the serverId to set
	 */
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}
	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}
	
	
	
}
