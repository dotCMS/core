package com.dotcms.rest.api.v1.vtl;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.StringPool;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static com.dotcms.rest.api.v1.vtl.VTLResource.VTL_PATH;

/**
 * This strategy reads the velocity code from a {@link FileAsset} corresponding (via name-convention) to the requesting
 * {@link com.dotcms.rest.api.v1.HTTPMethod} and returns it's content
 * <p>
 * File name convention (case-insensitive):
 * <ul>
 * <li>GET HTTP Method: get.vtl
 * <li>POST HTTP Method: post.vtl
 * <li>PUT HTTP Method: put.vtl
 * <li>PATCH HTTP Method: patch.vtl
 * <li>DELETE HTTP Method: delete.vtl
 * </ul>
 */


public class FileVelocityReader implements VelocityReader {
    private static final String FILE_EXTENSION = ".vtl";

    @Override
    public Reader getVelocity(final VTLResource.VelocityReaderParams params) throws DotSecurityException,
            IOException, DotDataException {

        final PageMode pageMode        = params.getPageMode();
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(params.getRequest());
        final Host site = APILocator.getHostAPI().resolveHostName(params.getRequest().getServerName(), APILocator.systemUser(), false);
        final String vtlFilePath = VTL_PATH + StringPool.SLASH + params.getFolderName() + StringPool.SLASH
                + params.getHttpMethod().fileName() + FILE_EXTENSION;
        final Identifier identifier = APILocator.getIdentifierAPI().find(site, vtlFilePath);
        final Contentlet getFileContent;

        if (null == identifier || !UtilMethods.isSet(identifier.getId())) {

            throw new DoesNotExistException ("The vtl: " + vtlFilePath + " does not exists");
        }

        try {
            getFileContent = APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), pageMode.showLive,
                    currentLanguage.getId(), params.getUser(), pageMode.respectAnonPerms);
        } catch (DotContentletStateException e) {

            throw new DoesNotExistException (e.getMessage(), e);
        }
        final FileAsset getFileAsset = APILocator.getFileAssetAPI().fromContentlet(getFileContent);

        return new InputStreamReader(getFileAsset.getInputStream());
    }

}
