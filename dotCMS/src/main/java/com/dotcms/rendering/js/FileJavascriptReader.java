package com.dotcms.rendering.js;

import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotcms.rendering.velocity.viewtools.ScriptingFileAttribute;
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
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * This strategy reads the javascript code from a {@link FileAsset} corresponding (via name-convention) to the requesting
 * {@link com.dotcms.rest.api.v1.HTTPMethod} and returns it's content
 * <p>
 * File name convention (case-insensitive):
 * <ul>
 * <li>GET HTTP Method: get.js
 * <li>POST HTTP Method: post.js
 * <li>PUT HTTP Method: put.js
 * <li>PATCH HTTP Method: patch.js
 * <li>DELETE HTTP Method: delete.js
 * </ul>
 */
public class FileJavascriptReader implements JavascriptReader {
    private static final String FILE_EXTENSION = ".js";

    @Override
    public Reader getJavaScriptReader(final ScriptingReaderParams params) throws DotSecurityException,
            IOException, DotDataException {

        final PageMode pageMode        = params.getPageMode();
        final Language currentLanguage = WebAPILocator.getLanguageWebAPI().getLanguage(params.getRequest());
        final Host site = APILocator.getHostAPI().resolveHostName(params.getRequest().getServerName(), APILocator.systemUser(), false);
        final String jsFilePath = JsResource.JS_PATH + StringPool.SLASH + params.getFolderName() + StringPool.SLASH
                + params.getHttpMethod().fileName() + FILE_EXTENSION;
        final Contentlet jsFile = findJavascriptFile(jsFilePath,
                null != site? site: APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false),
                null != currentLanguage? currentLanguage: APILocator.getLanguageAPI().getDefaultLanguage(),
                pageMode, params.getUser());
        // lets $dotsecrets check the file's last editor, as it does for page content
        ScriptingFileAttribute.set(params.getRequest(), jsFile);
        return new InputStreamReader(APILocator.getFileAssetAPI().fromContentlet(jsFile).getInputStream());
    }

    /**
     * Retrieve a javascript reader from a given path
     * @param jsFilePath
     * @param site
     * @param currentLanguage
     * @param pageMode
     * @param user
     * @return
     * @throws DotSecurityException
     * @throws IOException
     * @throws DotDataException
     */
    public static Reader getJavascriptReaderFromPath (final String jsFilePath, final Host site,
                                                      final Language currentLanguage, final PageMode pageMode,
                                                      final User user) throws DotSecurityException,
            IOException, DotDataException {

        final Contentlet jsFile = findJavascriptFile(jsFilePath, site, currentLanguage, pageMode, user);
        return new InputStreamReader(APILocator.getFileAssetAPI().fromContentlet(jsFile).getInputStream());
    }

    /**
     * Finds the javascript file asset at the given path.
     * @param jsFilePath path of the javascript file asset
     * @param site site holding the file
     * @param currentLanguage language of the file
     * @param pageMode page mode, decides live or working version and anonymous permissions
     * @param user user loading the file
     * @return the file asset {@link Contentlet}
     * @throws DotSecurityException if the user cannot read the file
     * @throws DotDataException if the file does not exist or cannot be loaded
     */
    private static Contentlet findJavascriptFile (final String jsFilePath, final Host site,
                                                  final Language currentLanguage, final PageMode pageMode,
                                                  final User user) throws DotSecurityException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(site, jsFilePath);
        final Contentlet getFileContent;

        if (UtilMethods.isEmpty(identifier::getId)) {

            throw new DoesNotExistException ("The Javascript: " + jsFilePath + " does not exists");
        }

        try {
            getFileContent = APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), pageMode.showLive,
                    currentLanguage.getId(), user, pageMode.respectAnonPerms);
        } catch (DotContentletStateException e) {

            throw new DoesNotExistException (e.getMessage(), e);
        }

        return getFileContent;
    }

}
