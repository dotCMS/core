package com.dotcms.cdn.viewtool;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.tools.ViewTool;
import com.dotcms.cdn.CDNConstants;
import com.dotcms.security.apps.AppSecrets;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import io.vavr.Lazy;
import io.vavr.control.Try;

public class DotCDNTool implements ViewTool {

    private HttpServletRequest request;
    private Host host;

    private final Lazy<Optional<AppSecrets>> appSecrets = Lazy.of(() ->
            Try.of(() -> APILocator.getAppsAPI()
                    .getSecrets(CDNConstants.DOT_CDN_APP_KEY, true, host, APILocator.systemUser()))
                    .getOrElse(Optional.empty()));

    @Override
    public void init(Object initData) {
        this.request = ((ViewContext) initData).getRequest();
        this.host = WebAPILocator.getHostWebAPI().getCurrentHostNoThrow(this.request);
    }

    /**
     * Creates the full url where the file lives in the cdn.
     *
     * @return the cdn domain + url, that is where the file lives
     */
    public String cdnify(final String fileUrl) {
        if (!appSecrets.get().isPresent() || fileUrl == null
                || fileUrl.contains("//") || !fileUrl.startsWith("/")) {
            return fileUrl;
        }

        return appSecrets.get().get().getSecrets()
                .get(CDNConstants.DOT_CDN_DOMAIN).getString() + fileUrl;
    }
}
