package com.dotcms.rest.api.v1.page;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.transform.DotContentletTransformer;
import com.dotmarketing.portlets.contentlet.transform.DotTransformerBuilder;
import com.dotmarketing.portlets.htmlpageasset.business.render.ContainerRaw;
import com.dotmarketing.portlets.htmlpageasset.business.render.page.PageView;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


public final class PageRenderVerifier {

    private PageRenderVerifier(){}

    public static  void verifyPageView(
            final PageView pageView,
            final PageRenderTestUtil.PageRenderTest pageRenderTest,
            final User user)
            throws DotSecurityException, DotDataException {

        final Collection<? extends ContainerRaw> pageContainers = pageView.getContainers();
        final List<String> containerIds = pageContainers.stream()
                .map((ContainerRaw containerRaw) -> containerRaw.getContainer().getIdentifier())
                .collect(Collectors.toList());

        final Collection<String> containersId = pageRenderTest.getContainersId();
        assertEquals(pageRenderTest.getContentsNumber(), pageView.getNumberContents());
        assertEquals(containerIds.size(), containersId.size());

        assertTrue(containerIds.containsAll(containersId));

        checkPermissionAttributes(pageView, pageRenderTest, user);

        checkContent(pageContainers, pageRenderTest);
    }

    private static void checkPermissionAttributes(final PageView pageView, final PageRenderTestUtil.PageRenderTest pageRenderTest,
                                                  final User user) throws DotDataException {

        final boolean haveWritepermission = APILocator.getPermissionAPI().doesUserHavePermission(pageRenderTest.getPage(),
                PermissionAPI.PERMISSION_WRITE, user, false);

        final DotContentletTransformer transformer = new DotTransformerBuilder()
                .graphQLDataFetchOptions().content(pageView.getPage()).forUser(user).build();

        final Contentlet pageViewAsContent = transformer.hydrate().get(0);

        assertEquals(haveWritepermission, pageViewAsContent.get("canEdit"));


        final boolean haveReadpermission = APILocator.getPermissionAPI().doesUserHavePermission(pageRenderTest.getPage(),
                PermissionAPI.PERMISSION_READ, user, false);
        assertEquals(haveReadpermission, pageViewAsContent.get("canRead"));
    }

    private static void checkContent(
            final Collection<? extends ContainerRaw> pageContainers,
            final PageRenderTestUtil.PageRenderTest pageRenderTest)
            throws DotDataException, DotSecurityException {

        for (final ContainerRaw pageContainer : pageContainers) {
            final Map<String, List<Map<String, Object>>> contentlets = pageContainer.getContentlets();
            final Container container = pageContainer.getContainer();
            final List<Structure> structures = APILocator.getContainerAPI().getStructuresInContainer(container);

            if (pageRenderTest.getContainer(container.getIdentifier()) == null) {
                fail("Unknown container with id " + container.getIdentifier());
            } else {
                assertEquals(pageRenderTest.getContainer(container.getIdentifier()).getTitle(), container.getTitle());
                assertEquals(pageContainer.getContainerStructures().size(), structures.size());
            }

            int nPageViewContents = 0;
            for (final List<Map<String, Object>> pageViewContents : contentlets.values()) {
                nPageViewContents += pageViewContents.size();
            }

            final List<Contentlet> contents = pageRenderTest.getContents(container.getIdentifier());
            assertEquals(contents == null ? 0 : contents.size(), nPageViewContents);

        }
    }
}