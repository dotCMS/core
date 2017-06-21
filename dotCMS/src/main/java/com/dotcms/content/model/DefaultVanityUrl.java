package com.dotcms.content.model;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

/**
 * This class represents a Vanity URL as a {@link Contentlet}
 *
 * @version 4.2.0
 * @autor oswaldogallango
 * @since June 16, 2017
 */
public class DefaultVanityUrl extends Contentlet implements VanityUrl {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String getTitle() {
        return getStringProperty(VanityUrlContentType.TITLE_FIELD_VAR);
    }

    @Override
    public void setTitle(String title) {
        setStringProperty(VanityUrlContentType.TITLE_FIELD_VAR, title);
    }

    @Override
    public String getSite() {
        return getStringProperty(VanityUrlContentType.SITE_FIELD_VAR);
    }

    @Override
    public void setSite(String site) {
        setStringProperty(VanityUrlContentType.SITE_FIELD_VAR, site);
    }

    @Override
    public String getURI() {
        return getStringProperty(VanityUrlContentType.URI_FIELD_VAR);
    }

    @Override
    public void setURI(String uri) {
        setStringProperty(VanityUrlContentType.URI_FIELD_VAR, uri);
    }

    @Override
    public String getForwardTo() {
        return getStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR);
    }

    @Override
    public void setForwardTo(String forwardTo) {
        setStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, forwardTo);
    }

    @Override
    public int getAction() {
        return (int) getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR);
    }

    @Override
    public void setAction(int action) {
        setLongProperty(VanityUrlContentType.ACTION_FIELD_VAR, action);
    }

    @Override
    public int getOrder() {
        return (int) getLongProperty(VanityUrlContentType.ORDER_FIELD_VAR);
    }

    @Override
    public void setOrder(int order) {
        setProperty(VanityUrlContentType.ORDER_FIELD_VAR, order);
    }

}
