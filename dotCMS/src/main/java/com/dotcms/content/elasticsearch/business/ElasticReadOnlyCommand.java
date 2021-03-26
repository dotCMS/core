package com.dotcms.content.elasticsearch.business;

/**
 * The command check the elastic index, if it is on ready only mode tries to switch to write mode
 * @author jsanca
 */
public interface ElasticReadOnlyCommand {

    /**
     * Executes the check of the elastic index, in case it is read only will try to
     * switch to write.
     */
    void executeCheck();

    static ElasticReadOnlyCommand getInstance() {
        return ElasticReadOnlyCommandImpl.getInstance();
    }

    /**
     * returns true if the index or cluster is on read only mode
     * @return
     */
    boolean isIndexOrClusterReadOnly();

    /**
     * Sends a read only message
     */
    void sendReadOnlyMessage();
}
