package com.dotmarketing.portlets.contentlet.business.hook;

import com.dotcms.contenttype.model.type.PageContentType;

import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPIPostHookAbstractImp;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Logger;

import java.util.List;

import com.liferay.portal.model.User;

public class HTMLPageHook extends ContentletAPIPostHookAbstractImp {

    @Override
    public void publish(final Contentlet contentlet, final User user, final boolean respectFrontendRoles) {

        super.publish(contentlet, user, respectFrontendRoles);

        if (contentlet !=null && contentlet.getContentType() instanceof PageContentType) {
            IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
            publishTemplateLayout(page, user, respectFrontendRoles);
        }
    }

    @Override
    public void publish(final List<Contentlet> contentlets, final User user, final boolean respectFrontendRoles) {

        super.publish(contentlets, user, respectFrontendRoles);
        
        if(contentlets !=null ) {
            for (Contentlet contentlet : contentlets) {
                if (contentlet.getContentType() instanceof PageContentType) {
                    IHTMLPage page = APILocator.getHTMLPageAssetAPI().fromContentlet(contentlet);
                    publishTemplateLayout(page, user, respectFrontendRoles);
                }
            }
        }
    }

    private void publishTemplateLayout(final IHTMLPage page, final User user, final boolean respectFrontendRoles) {

        try {
            final Template template =  APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), user, respectFrontendRoles);
            if(template==null) {
                return;
            }
            if (!APILocator.getTemplateAPI().isLive(template)) {
                APILocator.getTemplateAPI().setLive(template);
            }


        } catch (DotDataException e) {
            throw new DotStateException(
                    "unable to publish page layout with the page: " + page.getIdentifier() + " " + page.getTitle() + ": " + e, e);
        } catch (DotSecurityException ex) {
            Logger.warn(this.getClass(), "permission error, unable to publish page layout with the page: " + page.getIdentifier()
                    + " : " + page.getTitle() + " :" + ex.getMessage(), ex);
        }


    }


}


