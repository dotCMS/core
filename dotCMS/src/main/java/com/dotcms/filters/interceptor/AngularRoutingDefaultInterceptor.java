package com.dotcms.filters.interceptor;

import com.dotcms.filters.interceptor.Result;
import com.dotcms.filters.interceptor.WebInterceptor;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotmarketing.util.Config;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link WebInterceptor} from Angular routes
 */
public class AngularRoutingDefaultInterceptor implements WebInterceptor{

    private String angularRoutingUrlRex;
    private String indexURL;
    private AtomicBoolean activate = new AtomicBoolean();

    @Override
    public void init() {
        this.angularRoutingUrlRex = Config.getStringProperty("ANGULAR_ROUTING_URL_REX",
                "^\\/html\\/ng\\/(public|dotCMS|fromCore)\\/.*");
        this.indexURL = Config.getStringProperty("ANGULAR_INDEX_URL", "/html/ng/index.html");
        activate.set(Config.getBooleanProperty("DEFAULT_ANGULAR_ROUTING_INTERCEPTOR_ACTIVATE", true));
    }

    @Override
    public Result intercept(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getServletPath();

        if (path.matches(angularRoutingUrlRex)) {
            ServletContext context = req.getServletContext();

            String indexRealPath = context.getRealPath(indexURL);

            IOUtils.copy(new FileInputStream(indexRealPath), res.getOutputStream());
            return Result.SKIP_NO_CHAIN;
        }else {
            return Result.NEXT;
        }
    }

    @Override
    public boolean isActive() {
        return activate.get();
    }

    public void setAngularRoutingUrlRex(String angularRoutingUrlRex) {
        this.angularRoutingUrlRex = angularRoutingUrlRex;
    }

    public void setIndexURL(String indexURL) {
        this.indexURL = indexURL;
    }

    public void setActivate(boolean activate) {
        this.activate.set(activate);
    }
}
