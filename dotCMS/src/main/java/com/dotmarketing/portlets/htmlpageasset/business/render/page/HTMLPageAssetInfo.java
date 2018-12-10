package com.dotmarketing.portlets.htmlpageasset.business.render.page;

import com.dotcms.repackage.com.fasterxml.jackson.core.JsonGenerator;
import com.dotcms.repackage.com.fasterxml.jackson.databind.JsonSerializer;
import com.dotcms.repackage.com.fasterxml.jackson.databind.SerializerProvider;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotcms.repackage.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Contain information about a {@link HTMLPageAssetRendered}
 */
@JsonSerialize(using = HTMLPageAssetInfoSerializer.class)
public class HTMLPageAssetInfo {

    private HTMLPageAsset page;

    private String workingInode;
    private String shortyWorking;
    private boolean canEdit;
    private boolean canRead;
    private String liveInode;
    private String shortyLive;
    private boolean canLock;
    private Date lockedOn;
    private String lockedBy;
    private String lockedByName;

    HTMLPageAssetInfo(){}

    public HTMLPageAsset getPage() {
        return page;
    }

    HTMLPageAssetInfo setPage(HTMLPageAsset page) {
        this.page = page;
        return this;
    }

    public String getWorkingInode() {
        return workingInode;
    }

    HTMLPageAssetInfo setWorkingInode(String workingInode) {
        this.workingInode = workingInode;
        return this;
    }

    public String getShortyWorking() {
        return shortyWorking;
    }

    HTMLPageAssetInfo setShortyWorking(String shortyWorking) {
        this.shortyWorking = shortyWorking;
        return this;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    HTMLPageAssetInfo setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    public HTMLPageAssetInfo setCanRead(final boolean canRead) {
        this.canRead = canRead;
        return this;
    }

    public boolean isCanRead() {
        return canRead;
    }

    public String getLiveInode() {
        return liveInode;
    }

    HTMLPageAssetInfo setLiveInode(String liveInode) {
        this.liveInode = liveInode;
        return this;
    }

    public String getShortyLive() {
        return shortyLive;
    }

    HTMLPageAssetInfo setShortyLive(String shortyLive) {
        this.shortyLive = shortyLive;
        return this;
    }

    public Date getLockedOn() {
        return lockedOn;
    }

    HTMLPageAssetInfo setLockedOn(Date lockedOn) {
        this.lockedOn = lockedOn;
        return this;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    HTMLPageAssetInfo setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
        return this;
    }

    public String getLockedByName() {
        return lockedByName;
    }

    HTMLPageAssetInfo setLockedByName(String lockedByName) {
        this.lockedByName = lockedByName;
        return this;
    }

    HTMLPageAssetInfo setCanLock(boolean canLock) {
        this.canLock = canLock;
        return this;
    }

    public boolean isCanLock() {
        return canLock;
    }
}
