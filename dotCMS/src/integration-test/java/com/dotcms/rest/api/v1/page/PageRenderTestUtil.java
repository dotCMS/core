package com.dotcms.rest.api.v1.page;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.*;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeAPI;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;

import java.util.*;

public final class PageRenderTestUtil {

    private PageRenderTestUtil(){}

    public static PageRenderTest createPage(final int containersNumber, final Host host) {
        return createPage(containersNumber, host, true);
    }

    public static PageRenderTest createPage(final int containersNumber, final Host host, final boolean publish) {
        try {
            final PageRenderTest pageRenderTest = new PageRenderTest(host);

            PageRenderTestUtil.addContainers(pageRenderTest, containersNumber);
            pageRenderTest.persist();

            final Template template = pageRenderTest.getTemplate();

            final HTMLPageAsset page = PageRenderTestUtil.createHTMLPageAsset(template, host, publish);
            pageRenderTest.setPage(page);

            return pageRenderTest;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static void addContainers(final PageRenderTest templateTest , final int containersNumber)
            throws DotDataException, DotSecurityException {

        final ContentType webPageContent = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find("webPageContent");
        StructureTransformer structureTransformer = new StructureTransformer(webPageContent);
        final Structure structure = structureTransformer.asStructure();

        for (int i = 0; i < containersNumber; i++) {

            final Container container = new ContainerDataGen()
                    .withStructure(structure, "testing")
                    .friendlyName(String.format("container-%d-friendly-name", i))
                    .title(String.format("container-%d-title", i))
                    .nextPersisted();

            templateTest.addContainer(container);
        }
    }

    private static HTMLPageAsset createHTMLPageAsset(final Template template, final Host host) throws DotSecurityException, DotDataException {
        return createHTMLPageAsset(template, host, true);
    }

    private static HTMLPageAsset createHTMLPageAsset(final Template template, final Host host, final boolean publish)
            throws DotDataException, DotSecurityException {

        final Folder folder = new FolderDataGen()
                .site(host)
                .nextPersisted();

        final HTMLPageAsset pageAsset = (HTMLPageAsset) new HTMLPageDataGen(folder, template)
                .host(host)
                .languageId(1)
                .nextPersisted();

        APILocator.getVersionableAPI().setWorking(pageAsset);

        if (publish) {
            APILocator.getVersionableAPI().setLive(pageAsset);
        }

        return pageAsset;
    }

    public static class PageRenderTest {
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

        public Contentlet createContent(final Container container) {
            try {
                final Contentlet contentlet = createGenericContent();
                final List<Contentlet> contents = this.contents.getContents(container.getIdentifier());
                final int nContents = contents == null ? 0 : contents.size();

                final MultiTreeAPI multiTreeAPI = APILocator.getMultiTreeAPI();
                final MultiTree multiTree = new MultiTree(page.getIdentifier(),
                        container.getIdentifier(), contentlet.getIdentifier(), "1", nContents + 1);
                multiTreeAPI.saveMultiTree(multiTree);

                this.contents.addContent(container.getIdentifier(), contentlet);

                return contentlet;
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

    private static class ContentTest {
        private final Map<String, List<Contentlet>> contents = new HashMap<>();

        void addContent (final String conteinerId, final Contentlet contentlet) {
            List<Contentlet> contentlets = contents.get(conteinerId);

            if (contentlets == null) {
                contentlets = new ArrayList<>();
                contents.put(conteinerId, contentlets);
            }

            contentlets.add(contentlet);
        }

        List<Contentlet> getContents(final String conteinerId) {
            return contents.get(conteinerId);
        }

        public long getNumber() {
            return contents.values().stream()
                    .flatMap(containerContents -> containerContents.stream())
                    .count();
        }
    }
}