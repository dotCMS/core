package com.dotcms.vanityurl.model;

import com.dotcms.contenttype.model.type.VanityUrlContentType;
import com.dotmarketing.exception.DotRuntimeException;
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
    public void setTitle(final String title) {
        setStringProperty(VanityUrlContentType.TITLE_FIELD_VAR, title);
    }

    @Override
    public String getSite() {
        return getStringProperty(VanityUrlContentType.SITE_FIELD_VAR);
    }

    @Override
    public void setSite(final String site) {
        setStringProperty(VanityUrlContentType.SITE_FIELD_VAR, site);
    }

    @Override
    public String getURI() {
        return getStringProperty(VanityUrlContentType.URI_FIELD_VAR);
    }

    @Override
    public void setURI(final String uri) {
        setStringProperty(VanityUrlContentType.URI_FIELD_VAR, uri);
    }

    @Override
    public String getForwardTo() {
        return getStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR);
    }

    @Override
    public void setForwardTo(final String forwardTo) {
        setStringProperty(VanityUrlContentType.FORWARD_TO_FIELD_VAR, forwardTo);
    }

    @Override
    public int getAction() {
        return (int) getLongProperty(VanityUrlContentType.ACTION_FIELD_VAR);
    }

    @Override
    public void setAction(final int action) {
        setLongProperty(VanityUrlContentType.ACTION_FIELD_VAR, action);
    }

    @Override
    public int getOrder() {
        return (int) getLongProperty(VanityUrlContentType.ORDER_FIELD_VAR);
    }

    @Override
    public void setOrder(final int order) {
        setLongProperty(VanityUrlContentType.ORDER_FIELD_VAR, order);
    }


}
