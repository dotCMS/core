package com.dotmarketing.portlets.folders.business.strategy;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.IdentifierAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.PathUtil;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import io.vavr.Tuple2;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class SiteBrowserReaderFileStrategyImpl implements ReaderFileStrategy {

    private final LanguageAPI   languageAPI   = APILocator.getLanguageAPI();
    private final IdentifierAPI identifierAPI = APILocator.getIdentifierAPI();
    private final ContentletAPI contentletAPI = APILocator.getContentletAPI();
    private final FileAssetAPI  fileAssetAPI  = APILocator.getFileAssetAPI();

    @Override
    public boolean test(final String file) {

        return null != file && (file.toLowerCase().startsWith(SITE_BROWSER_SYSTEM_PREFIX) ||
                file.toLowerCase().startsWith(StringPool.FORWARD_SLASH));
    }

    @Override
    public Reader apply(final String file)  throws IOException {

        try {

            final User user = APILocator.systemUser();
            final Tuple2<String, Host> pathHost =
                    PathUtil.getInstance().getHostAndPath(file, user, Sneaky.sneaked(this::getHost));
            final String path               = pathHost._1;
            final Host   site               = pathHost._2;
            final long languageId           = this.languageAPI.getDefaultLanguage().getId();
            final Identifier identifier     = this.identifierAPI.find(site, path);
            final Contentlet getFileContent = this.contentletAPI.findContentletByIdentifier
                    (identifier.getId(), true, languageId, user, true);
            final FileAsset getFileAsset    = this.fileAssetAPI.fromContentlet(getFileContent);

            return new InputStreamReader(getFileAsset.getInputStream());
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private Host getHost () throws DotSecurityException, DotDataException {

        final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
        return (null != request)?
                WebAPILocator.getHostWebAPI().getHost(request):
                APILocator.getHostAPI().findDefaultHost(APILocator.systemUser(), false);
    }

    @Override
    public Source source() {
        return Source.SITE_BROWSER;
    }
}
