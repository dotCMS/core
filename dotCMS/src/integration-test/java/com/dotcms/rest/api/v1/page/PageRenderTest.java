package com.dotcms.rest.api.v1.page;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TemplateLayoutDataGen;
import com.dotcms.datagen.ThemeDataGen;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.*;


public class PageRenderTest {
    private final Map<String, Container> containers = new HashMap<>();
    private final ContentTest contents = new ContentTest();
    private Template template;
    private HTMLPageAsset page;
    private final Host host;

    PageRenderTest(final Host host){
        this.host = host;
    }
    public void addContainer(final Container container) {
        containers.put(container.getIdentifier(), container);
    }

    public Template persist() {
        try {
            final TemplateLayoutDataGen templateLayoutDataGen = TemplateLayoutDataGen.get();
            final TemplateDataGen templateDataGen = new TemplateDataGen();

            for (final Container container : containers.values()) {
                templateLayoutDataGen.withContainer(container.getIdentifier());
                templateDataGen.withContainer(container.getIdentifier());
            }

            final Contentlet contentlet = new ThemeDataGen().site(host).nextPersisted();

            final TemplateLayout templateLayout = templateLayoutDataGen.next();
            this.template = templateDataGen
                    .drawedBody(templateLayout)
                    .theme(contentlet.getFolder())
                    .nextPersisted();

            APILocator.getVersionableAPI().setWorking(this.template);
            APILocator.getVersionableAPI().setLive(this.template);

            return this.template;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    public Template getTemplate() {
        return template;
    }

    public Container getFirstContainer() {
        return containers.values().iterator().next();
    }

    public void createContent(final Container container) {
        try {
            final Contentlet contentlet = createGenericContent();
            final List<Contentlet> contents = this.contents.getContents(container.getIdentifier());
            final int nContents = contents == null ? 0 : contents.size();

            final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
            final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                    container.getIdentifier(), contentlet.getIdentifier(), "1", nContents + 1);
            multiTreeAPI.saveMultiTree(multiTree);

            this.contents.addContent(container.getIdentifier(), contentlet);
        } catch (DotSecurityException | DotDataException e) {
            throw new DotRuntimeException(e);
        }
    }

    private Contentlet createGenericContent() throws DotSecurityException, DotDataException {
        final ContentType contentGenericType = APILocator.getContentTypeAPI(APILocator.systemUser()).find("webPageContent");
        return new ContentletDataGen(contentGenericType.id()).setProperty("title", "title")
                .setProperty("body", "body").languageId(1).nextPersisted();
    }

    public void setPage(final HTMLPageAsset page) {
        this.page = page;
    }

    public HTMLPageAsset getPage() {
        return page;
    }

    public long getContentsNumber() {
        return contents.getNumber();
    }

    public Collection<String> getContainersId() {
        return containers.keySet();
    }

    public Container getContainer(final String id) {
        return containers.get(id);
    }

    public List<Contentlet> getContents(final String containerId) {
        return contents.getContents(containerId);
    }
}