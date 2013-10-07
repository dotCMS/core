package com.dotcms.publisher.pusher.wrapper;

import com.dotcms.publisher.pusher.PushPublisherConfig.Operation;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;

import java.util.List;
import java.util.Map;

public class HTMLPageWrapper {

    private HTMLPage page;
    private List<Map<String, Object>> multiTree;
    private Identifier pageId;
    private VersionInfo vi;
    private Operation operation;

    public HTMLPageWrapper ( HTMLPage page, Identifier pageId ) {
        this.page = page;
        this.pageId = pageId;
    }

    public HTMLPage getPage () {
        return page;
    }

    public void setPage ( HTMLPage page ) {
        this.page = page;
    }

    public Identifier getPageId () {
        return pageId;
    }

    public void setPageId ( Identifier pageId ) {
        this.pageId = pageId;
    }

    /**
     * @return the vi
     */
    public VersionInfo getVi () {
        return vi;
    }

    /**
     * @param vi the vi to set
     */
    public void setVi ( VersionInfo vi ) {
        this.vi = vi;
    }

    /**
     * @return the operation
     */
    public Operation getOperation () {
        return operation;
    }

    /**
     * @param operation the operation to set
     */
    public void setOperation ( Operation operation ) {
        this.operation = operation;
    }

    public List<Map<String, Object>> getMultiTree () {
        return multiTree;
    }

    public void setMultiTree ( List<Map<String, Object>> multiTree ) {
        this.multiTree = multiTree;
    }

}