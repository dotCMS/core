package com.dotmarketing.common.business.journal;

public class IndexJournal {

    private long id;
    private String identToIndex;
    private long priority;
    private boolean delete;
    private String serverId;
    private int count;

    public IndexJournal() {}

    public IndexJournal(long id, String objectToIndex, long priority) {
        this.id = id;
        this.identToIndex = objectToIndex;
        this.priority = priority;
    }

    public IndexJournal(String serverId, int count, long priority) {
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
     * @return the identToIndex
     */
    public String getIdentToIndex() {
        return identToIndex;
    }

    public boolean isReindex() {
        return getPriority() == DistributedJournalFactory.REINDEX_JOURNAL_PRIORITY_NEWINDEX;
    }

    /**
     * @param identToIndex the identToIndex to set
     */
    public void setIdentToIndex(String identToIndex) {
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
        IndexJournal other = (IndexJournal) obj;
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

}
