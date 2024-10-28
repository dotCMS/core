package com.dotcms.rest.api.v1.content;

import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;

/**
 * This class is used to represent a reference to a content item.
 * @author
 */
public class ContentReferenceView {

    private final IHTMLPage page;
    private final Container container;
    private final String personaName;

    public ContentReferenceView(final IHTMLPage page,
                                final Container container,
                                final String personaName) {
        this.page = page;
        this.container = container;
        this.personaName = personaName;
    }

    public IHTMLPage getPage() {
        return page;
    }

    public Container getContainer() {
        return container;
    }

    public String getPersonaName() {
        return personaName;
    }
}
