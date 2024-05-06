package com.dotcms.rendering.js.proxy;

import com.dotmarketing.tag.model.TagInode;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.Date;

/**
 * Encapsulates a {@link TagInode} in a Js context, which is basically a file sent by multipart/form-data
 * @author jsanca
 */
public class JsTagInode implements Serializable, JsProxyObject<TagInode> {

    private final TagInode tagInode;

    public JsTagInode(final TagInode tagInode) {
        this.tagInode = tagInode;
    }
    @Override
    public TagInode getWrappedObject() {
        return tagInode;
    }

    @HostAccess.Export
    public String getTagId() {
        return this.tagInode.getTagId();
    }

    @HostAccess.Export
    public String getFieldVarName() {
        return this.tagInode.getFieldVarName();
    }

    @HostAccess.Export
    public String getInode() {
        return this.tagInode.getInode();
    }

    @HostAccess.Export
    public Date getModDate () {
        return this.tagInode.getModDate();
    }

    @HostAccess.Export
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.tagInode.hashCode();
    }

    @HostAccess.Export
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return this.tagInode.equals(obj);
    }

}
