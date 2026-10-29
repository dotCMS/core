package com.dotcms.rendering.js;

import com.dotcms.rendering.util.ScriptingReaderParams;
import com.dotcms.rendering.util.ScriptingUtil;
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

    /**
     * Reads the file named after the request's HTTP method in the requested folder, for example
     * {@code get.js}, once {@link ScriptingUtil#checkLastModifierCanScript(Contentlet)} confirms its
     * last modifier may write scripts.
     *
     * @param params the folder, HTTP method, request, user and page mode of the call
     * @return a reader over the file's code
     * @throws DotSecurityException when the user cannot read the file or its last modifier may not write scripts
     * @throws IOException          when the file's contents cannot be opened
     * @throws DotDataException     when the file, its last modifier or the role cannot be loaded
     */
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
        ScriptingUtil.getInstance().checkLastModifierCanScript(jsFile);
        return new InputStreamReader(APILocator.getFileAssetAPI().fromContentlet(jsFile).getInputStream());
    }

    /**
     * Opens the javascript file at the given path, without checking who last saved it.
     *
     * @param jsFilePath      the file's path on the site
     * @param site            the site the file lives in
     * @param currentLanguage the language of the version to read
     * @param pageMode        the page mode to read the file in
     * @param user            the user reading the file
     * @return a reader over the file's code
     * @throws DotSecurityException when the user cannot read the file
     * @throws IOException          when the file's contents cannot be opened
     * @throws DotDataException     when the file cannot be loaded
     */
    public static Reader getJavascriptReaderFromPath (final String jsFilePath, final Host site,
                                                      final Language currentLanguage, final PageMode pageMode,
                                                      final User user) throws DotSecurityException,
            IOException, DotDataException {

        final FileAsset getFileAsset = APILocator.getFileAssetAPI()
                .fromContentlet(findJavascriptFile(jsFilePath, site, currentLanguage, pageMode, user));
        return new InputStreamReader(getFileAsset.getInputStream());
    }

    /**
     * Finds the file at the given path, in the version the page mode asks for.
     *
     * @param jsFilePath      the file's path on the site
     * @param site            the site the file lives in
     * @param currentLanguage the language of the version to read
     * @param pageMode        the page mode to read the file in
     * @param user            the user reading the file
     * @return the file
     * @throws DoesNotExistException when there is no file at that path
     * @throws DotSecurityException  when the user cannot read the file
     * @throws DotDataException      when the file cannot be loaded
     */
    private static Contentlet findJavascriptFile(final String jsFilePath, final Host site,
                                                 final Language currentLanguage, final PageMode pageMode,
                                                 final User user) throws DotSecurityException, DotDataException {

        final Identifier identifier = APILocator.getIdentifierAPI().find(site, jsFilePath);

        if (UtilMethods.isEmpty(identifier::getId)) {

            throw new DoesNotExistException ("The Javascript: " + jsFilePath + " does not exists");
        }

        try {
            return APILocator.getContentletAPI().findContentletByIdentifier(identifier.getId(), pageMode.showLive,
                    currentLanguage.getId(), user, pageMode.respectAnonPerms);
        } catch (DotContentletStateException e) {

            throw new DoesNotExistException (e.getMessage(), e);
        }
    }

}
