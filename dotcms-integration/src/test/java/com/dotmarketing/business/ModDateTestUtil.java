package com.dotmarketing.business;

import com.dotcms.business.WrapInTransaction;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.VersionableFactoryImpl;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletVersionInfo;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.templates.model.Template;
import java.util.Date;
import java.util.Optional;

public class ModDateTestUtil {

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
    public static void updateTemplateModeDate(final Template template, final Date modDate) {
        template.setModDate(modDate);
        try {
            FactoryLocator.getTemplateFactory().save(template);
        } catch (DotDataException e) {
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

    public static void updateContentletVersionDate(final Contentlet contentlet, final Date modDate) {
        updateContentletVersionDate(contentlet.getIdentifier(), contentlet.getLanguageId(), modDate);
    }

    public static void updateContainerVersionDate(final Container container, final Date modDate) {
        updateAssetVersionDate(container.getIdentifier(), modDate);
    }

    @WrapInTransaction
    public static void updateTemplateVersionDate(final Template template, final Date modDate) {
        updateAssetVersionDate(template.getIdentifier(), modDate);
    }

    @WrapInTransaction
    public static void updateAssetVersionDate(final String identifierStr, final Date modDate) {
        try {
            final Identifier identifier = APILocator.getIdentifierAPI().find(identifierStr);
            VersionInfo versionInfoFromDb = FactoryLocator.getVersionableFactory()
                    .findVersionInfoFromDb(identifier);

            versionInfoFromDb.setVersionTs(modDate);

            FactoryLocator.getVersionableFactory().saveVersionInfo(versionInfoFromDb, false);
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    @WrapInTransaction
    public static void updateContentletVersionDate(final String assetId, final long langId, final Date modDate) {

        try {
            final Optional<ContentletVersionInfo> contentletVersionInfoOptional = FactoryLocator
                    .getVersionableFactory().getContentletVersionInfo(assetId, langId);

            if (contentletVersionInfoOptional.isPresent()) {
                final ContentletVersionInfo contentletVersionInfo = contentletVersionInfoOptional
                        .get();

                contentletVersionInfo.setVersionTs(modDate);
                FactoryLocator.getVersionableFactory().saveContentletVersionInfo(contentletVersionInfo, false);
            }
        } catch (DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }
}
