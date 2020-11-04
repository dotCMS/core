package com.dotcms.rest.api.v1.versionable;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.Versionable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.languagesmanager.model.Language;

import java.util.Date;

public class VersionableView implements Versionable {

    private final Versionable versionable;

    public VersionableView(final Versionable versionable) {
        this.versionable = versionable;
    }

    @Override
    public String getVersionId() {
        return this.versionable.getVersionId();
    }

    @Override
    public void setVersionId(String versionId) {
        this.versionable.setVersionId(versionId);
    }

    @Override
    public String getVersionType() {
        return this.versionable.getVersionType();
    }

    @Override
    public String getInode() {
        return this.versionable.getInode();
    }

    @Override
    public boolean isArchived() throws DotStateException, DotDataException, DotSecurityException {
        return this.versionable.isArchived();
    }

    @Override
    public boolean isWorking() throws DotStateException, DotDataException, DotSecurityException {
        return this.versionable.isWorking();
    }

    @Override
    public boolean isLive() throws DotStateException, DotDataException, DotSecurityException {
        return this.versionable.isLive();
    }

    @Override
    public String getTitle() throws DotStateException {
        return this.versionable.getTitle();
    }

    @Override
    public String getModUser() {
        return this.versionable.getModUser();
    }

    @Override
    public Date getModDate() {
        return this.versionable.getModDate();
    }

    @Override
    public boolean isLocked() throws DotStateException, DotDataException, DotSecurityException {
        return this.versionable.isLocked();
    }

    public Language getLanguage () {

        return this.versionable instanceof Contentlet?
                APILocator.getLanguageAPI().getLanguage(Contentlet.class.cast(this.versionable).getLanguageId()):
                APILocator.getLanguageAPI().getDefaultLanguage();
    }
}
