package com.dotcms.rest.api.v1.page;

import com.dotcms.datagen.*;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.portlets.templates.model.Template;

public final class PageRenderUtilTest {

    private PageRenderUtilTest(){}


    public static PageRenderTest createPage(final int containersNumber, final Host host) {
        try {
            final PageRenderTest templateTest = new PageRenderTest();

            final PageRenderTest pageRenderTest = PageRenderUtilTest.createTemplate(templateTest, containersNumber);
            final Template template = pageRenderTest.getTemplate();

            final HTMLPageAsset page = PageRenderUtilTest.createHTMLPageAsset(template, host);
            pageRenderTest.setPage(page);

            return pageRenderTest;
        } catch (DotDataException | DotSecurityException e) {
            throw new DotRuntimeException(e);
        }
    }

    private static PageRenderTest createTemplate(final PageRenderTest templateTest , final int containersNumber) {

        for (int i = 0; i < containersNumber; i++) {
            final Structure structure1 = new StructureDataGen().nextPersisted();
            final Container container = new ContainerDataGen()
                    .withStructure(structure1, "")
                    .friendlyName(String.format("container-%d-friendly-name", i))
                    .title(String.format("container-%d-title", i))
                    .nextPersisted();

            templateTest.addContainer(container);
        }

        templateTest.persist();
        return templateTest;
    }

    private static HTMLPageAsset createHTMLPageAsset(final Template template, final Host host)
            throws DotDataException, DotSecurityException {

        final Folder folder = new FolderDataGen()
                .site(host)
                .nextPersisted();

        final HTMLPageAsset pageAsset = (HTMLPageAsset) new HTMLPageDataGen(folder, template)
                .host(host)
                .languageId(1)
                .nextPersisted();

        APILocator.getVersionableAPI().setWorking(pageAsset);
        APILocator.getVersionableAPI().setLive(pageAsset);

        return pageAsset;
    }
}
