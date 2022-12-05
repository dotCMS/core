package com.dotcms.datagen;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.Optional;

public abstract class AbstractDataGen<T> implements DataGen<T> {
    protected static User user;
    protected Host host;
    protected Folder folder;
    protected Language language;

    static{
        try {
            user = APILocator.getUserAPI().getSystemUser();
        } catch (DotDataException e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }

    public AbstractDataGen() {
        try {

            host = APILocator.getHostAPI().findDefaultHost(user, false);
            folder = APILocator.getFolderAPI().findSystemFolder();
            language = APILocator.getLanguageAPI().getDefaultLanguage();
        } catch (DotDataException | DotSecurityException e) {
            throw new RuntimeException("Unable to get System User and/or Default Host", e);
        }
    }

    @WrapInTransaction
    public static void updateTemplateModeDate(final Template template, final Date modDate) {
        template.setModDate(modDate);
        try {
            FactoryLocator.getTemplateFactory().save(template);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    public static void updateTemplateVersionDate(final Template template, final Date modDate) {
        updateAssetVersionDate(template.getIdentifier(), modDate);
    }

    @WrapInTransaction
    public static void updateAssetVersionDate(final String identifierStr, final Date modDate) {
        try {
            VersionInfo versionInfoFromDb = APILocator.getVersionableAPI().getVersionInfo(identifierStr);

            versionInfoFromDb.setVersionTs(modDate);

            APILocator.getVersionableAPI().saveVersionInfo(versionInfoFromDb);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static void updateContentletVersionDate(final Contentlet contentlet, final Date modDate) {
        updateContentletVersionDate(contentlet.getIdentifier(), contentlet.getLanguageId(), modDate);
    }

    @WrapInTransaction
    public static void updateContentletVersionDate(final String assetId, final long langId, final Date modDate) {

        try {
            final Optional<ContentletVersionInfo> contentletVersionInfoOptional =
            APILocator.getVersionableAPI().getContentletVersionInfo(assetId, langId);

            if (contentletVersionInfoOptional.isPresent()) {
                final ContentletVersionInfo contentletVersionInfo = contentletVersionInfoOptional
                        .get();

                contentletVersionInfo.setVersionTs(modDate);
                APILocator.getVersionableAPI().saveContentletVersionInfo(contentletVersionInfo);
            }
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    public static void updateContentletModeDate(final Contentlet contentlet, final Date modDate) {
        contentlet.setModDate(modDate);

        try {
            FactoryLocator.getContentletFactory().save(contentlet);
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    public static void updateContainerModeDate(final Container container, final Date modDate) {
        container.setModDate(modDate);
        try {
            FactoryLocator.getContainerFactory().save(container);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    public static void updateContainerVersionDate(final Container container, final Date modDate) {
        updateAssetVersionDate(container.getIdentifier(), modDate);
    }
}
