package com.dotmarketing.common.reindex;

import java.util.Date;

public class ReindexEntry {

    private long id;
    private String identToIndex;
    private int priority;
    private boolean delete;
    private String serverId;
    private String lastResult;
    private Date timeEntered;
    
    public Date getTimeEntered() {
        return timeEntered;
    }

    public ReindexEntry setTimeEntered(Date timeEntered) {
        this.timeEntered = timeEntered;
        return this;
    }

    public ReindexEntry() {}

    public ReindexEntry(long id, String objectToIndex, int priority) {
        this.id = id;
        this.identToIndex = objectToIndex;
        this.priority = priority;
    }

    public String getLastResult() {
        return lastResult;
    }

    public ReindexEntry setLastResult(String lastResult) {
        this.lastResult = lastResult;
        return this;
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
    public ReindexEntry setId(long id) {
        this.id = id;
        return this;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority the priority to set
     */
    public ReindexEntry setPriority(int priority) {
        this.priority = priority;
        return this;
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
    public ReindexEntry setDelete(boolean delete) {
        this.delete = delete;
        return this;
    }

    /**
     * @return the identToIndex
     */
    public String getIdentToIndex() {
        return identToIndex;
    }

    public boolean isReindex() {
        return getPriority() >= ReindexQueueFactory.Priority.REINDEX.dbValue();
    }

    /**
     * @param identToIndex the identToIndex to set
     */
    public ReindexEntry setIdentToIndex(String identToIndex) {
        this.identToIndex = identToIndex;
        return this;
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
    public ReindexEntry setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public int errorCount() {
        return this.getPriority() % 100;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (delete ? 1231 : 1237);
        result = prime * result + ((identToIndex == null) ? 0 : identToIndex.hashCode());
        result = prime * result + (int) (priority ^ (priority >>> 32));
        result = prime * result + ((serverId == null) ? 0 : serverId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReindexEntry other = (ReindexEntry) obj;
        if (delete != other.delete)
            return false;
        if (identToIndex == null) {
            if (other.identToIndex != null)
                return false;
        } else if (!identToIndex.equals(other.identToIndex))
            return false;
        if (priority != other.priority)
            return false;
        if (serverId == null) {
            if (other.serverId != null)
                return false;
        } else if (!serverId.equals(other.serverId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "IndexJournal [id=" + id + ", identToIndex=" + identToIndex + ", priority=" + priority + ", delete=" + delete + ", serverId="
                + serverId + "]";
    }

}
