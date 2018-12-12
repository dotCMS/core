package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.liferay.util.StringPool;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;

public class FileVelocityReader implements VelocityReader {
    private static final String FILE_EXTENSION = ".vtl";

    @Override
    public Reader getVelocity(final VTLResource.VelocityReaderParams params) throws DotSecurityException,
            IOException, DotDataException {

        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(params.getRequest());
        final Host site = APILocator.getHostAPI().resolveHostName(params.getRequest().getServerName(), APILocator.systemUser(), false);
        final String vtlFilePath = VTL_PATH + StringPool.SLASH + params.getFolderName() + StringPool.SLASH
                + params.getHttpMethod().fileName() + FILE_EXTENSION;
        final Identifier identifier = APILocator.getIdentifierAPI().find(site, vtlFilePath);
        final Contentlet getFileContent = APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), true,
                currentLanguage.getId(), params.getUser(), true);
        final FileAsset getFileAsset = APILocator.getFileAssetAPI().fromContentlet(getFileContent);

        return new InputStreamReader(getFileAsset.getInputStream());
    }

}
