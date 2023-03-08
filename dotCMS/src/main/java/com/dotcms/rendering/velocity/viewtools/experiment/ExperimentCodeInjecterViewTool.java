package com.dotcms.rendering.velocity.viewtools.experiment;

import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.experiments.business.ExperimentCodeGenerator;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import javax.servlet.http.HttpServletRequest;
import org.apache.velocity.tools.view.tools.ViewTool;

/**
 * ViewTool to inject the JS/HTML Experiment Code
 */
public class ExperimentCodeInjecterViewTool implements ViewTool {

    @Override
    public void init(Object initData) {

    }

    @Override
    public User getUser(HttpServletRequest request) {
        return ViewTool.super.getUser(request);
    }

    public String code(){
        try {
            final Host currentHost = WebAPILocator.getHostWebAPI().getCurrentHost();
            final HttpServletRequest request = HttpServletRequestThreadLocal.INSTANCE.getRequest();
            return ExperimentCodeGenerator.INSTANCE.getCode(currentHost, request).orElse(StringPool.BLANK);
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }
}
