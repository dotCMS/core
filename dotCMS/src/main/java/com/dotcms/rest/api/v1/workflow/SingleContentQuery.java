package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.contentlet.model.IndexPolicy;

public class SingleContentQuery {

    private final String identifier;
    private final String inode;
    private final long language;
    private final IndexPolicy indexPolicy;

    public SingleContentQuery(final String identifier, final String inode, final long language) {
        this.identifier = identifier;
        this.inode = inode;
        this.language = language;
        this.indexPolicy = IndexPolicy.DEFER;
    }

    public SingleContentQuery(final String identifier, final String inode, final long language, final IndexPolicy indexPolicy) {
        this.identifier = identifier;
        this.inode = inode;
        this.language = language;
        this.indexPolicy = indexPolicy;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getInode() {
        return inode;
    }

    public long getLanguage() {
        return language;
    }

    public IndexPolicy getIndexPolicy() {
        return indexPolicy;
    }


}
