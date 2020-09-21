package com.dotmarketing.portlets.contentlet.business.web;

import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.JsonTransformer;
import com.dotcms.datagen.*;
import com.dotcms.rendering.velocity.viewtools.DotTemplateTool;
import com.dotcms.repackage.org.directwebremoting.WebContextFactory;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.business.FileAssetContainerUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.containers.model.FileAssetContainer;
import com.dotmarketing.portlets.contentlet.model.Contentlet;

import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.quartz.DotSchedulerFactory;
import com.dotmarketing.quartz.QuartzUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.liferay.portal.model.User;
import com.liferay.util.servlet.SessionMessages;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.dotcms.repackage.org.directwebremoting.WebContext;
import org.quartz.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class ContentletWebAPIImplIntegrationTest {

    final String body =
            "<html>" +
                    "<head>" +
                        "#dotParse('%1$s/application/themes/landing-page/html_head.vtl')" +
                        "<link rel=\"stylesheet\" type=\"text/css\" href=\"/html/css/template/reset-fonts-grids.css\" />" +
                    "</head>" +
                    "<body>" +
                        "<div id=\"resp-template\" name=\"globalContainer\">" +
                        "<div id=\"hd-template\">" +
                        "#dotParse('%1$s/application/themes/landing-page/header.vtl')" +
                        "</div>" +
                        "<div id=\"bd-template\">" +
                            "<div id=\"yui-main-template\">" +
                                "<div class=\"yui-b-template\" id=\"splitBody0\">" +
                                    "#parseContainer('%1$s/application/containers/default/','1')" +
                                "</div>" +
                            "</div>" +
                        "</div>" +
                    "</body>" +
             "</html>";

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        if (!QuartzUtils.getScheduler().isStarted()) {
            QuartzUtils.getScheduler().start();
        }
    }

    /**
     * Method to Test: {@link ContentletWebAPIImpl#saveContent(Map, boolean, boolean, User)}
     * When: Change a Host' name and exists any template using this
     * Should: Update the container path into the template, just in the template working and live version
     *
     * */
    @Test
    public void whenHostNameChangeShouldUpdateContainerPath() throws Exception {

        final User user = APILocator.systemUser();
        init();

        final Host host = new SiteDataGen().nextPersisted();

        Container container = createContainer(user, host);
        Template template = createTemplate(host, container);
        final String oldVersionInode = template.getInode();

        template.setFooter("Footer");
        template = TemplateDataGen.save(template);
        TemplateDataGen.publish(template);
        final String liveVersionInode = template.getInode();

        template.setFooter("Footer_2");
        template = TemplateDataGen.save(template);
        final String workingVersionInode = template.getInode();

        final ContentletWebAPIImpl contentletWebAPI = new ContentletWebAPIImpl();
        final Map<String, Object> hostMap = host.getMap();
        final String newHostname = "newHostName_" + System.currentTimeMillis();
        hostMap.put("text1", newHostname);
        hostMap.put("contentletInode", hostMap.get("inode"));

        contentletWebAPI.saveContent(hostMap, false, false, user);
        waitUntilJobIsFinish();

        final Host hostFromDataBse = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        assertEquals(newHostname, hostFromDataBse.getHostname());

        checkTemplate(oldVersionInode, user, host.getHostname(), newHostname);
        checkTemplate(liveVersionInode, user, newHostname, host.getHostname());
        checkTemplate(workingVersionInode, user, newHostname, host.getHostname());

        final TemplateLayout templateLayout = DotTemplateTool.themeLayout(template.getInode());
        final String drawedBodyJson = JsonTransformer.mapper.writeValueAsString(templateLayout);
        assertFalse(drawedBodyJson.contains(host.getHostname()));
        assertTrue(drawedBodyJson.contains(newHostname));

        final Template workingVersion = (Template) APILocator.getVersionableAPI().findWorkingVersion(
                template.getIdentifier(), APILocator.systemUser(), false);

        assertFalse(workingVersion.getDrawedBody().contains(host.getHostname()));
        assertTrue(workingVersion.getDrawedBody().contains(newHostname));

        final FileAssetContainer containerFromDataBase = (FileAssetContainer) APILocator.getContainerAPI().getWorkingContainerByFolderPath(
                FileAssetContainerUtil.getInstance().getFullPath(hostFromDataBse, ((FileAssetContainer) container).getPath()),
                user, false,
                null);

        assertEquals(newHostname, containerFromDataBase.getHost().getName());
    }

    private void checkTemplate(
            final String templateInode,
            final User user,
            final String hostInTemplate,
            final String hostNotInTemplate) throws DotSecurityException, DotDataException, JsonProcessingException {

        final Template templateFromDatabase = APILocator.getTemplateAPI().find(templateInode, user, false);

        assertFalse(templateFromDatabase.getDrawedBody().contains(hostNotInTemplate));
        assertFalse(templateFromDatabase.getBody().contains(hostNotInTemplate));

        assertTrue(templateFromDatabase.getDrawedBody().contains(hostInTemplate));
        assertTrue(templateFromDatabase.getBody().contains(hostInTemplate));
    }


    /**
     * Method to Test: {@link ContentletWebAPIImpl#saveContent(Map, boolean, boolean, User)}
     * When: Change a Host' name and exists any html template layout using this
     * Should: Update the container path into the template
     *
     * */
    @Test
    public void whenHostNameChangeWithLegacyTemplateLayout() throws Exception {

        final String drawedBodyHTML = "" +
                "<div style=\"display: none;\" title=\"container_854ad819-8381-434d-a70f-6e2330985ea4\" id=\"splitBody0_div_854ad819-8381-434d-a70f-6e2330985ea4_1572981893151\">" +
                "#parseContainer('//%s','1572981893151')" +
                "</div>";

        final User user = APILocator.systemUser();
        init();

        final Host host = new SiteDataGen().nextPersisted();
        final Contentlet theme = new ThemeDataGen().nextPersisted();

        final Template template = new TemplateDataGen()
                .drawedBody(String.format(drawedBodyHTML, host.getHostname()))
                .body(String.format(body, host.getHostname()))
                .host(host)
                .theme(theme)
                .nextPersisted();

        final ContentletWebAPIImpl contentletWebAPI = new ContentletWebAPIImpl();
        final Map<String, Object> hostMap = host.getMap();
        final String newHostname = "newHostName_" + System.currentTimeMillis();
        hostMap.put("text1", newHostname);
        hostMap.put("contentletInode", hostMap.get("inode"));

        contentletWebAPI.saveContent(hostMap, false, false, user);
        waitUntilJobIsFinish();

        final Host hostFromDataBse = APILocator.getHostAPI().find(host.getIdentifier(), user, false);
        assertEquals(newHostname, hostFromDataBse.getHostname());

        final Template templateFromDatabase = APILocator.getTemplateAPI().find(template.getInode(), user, false);

        assertFalse(templateFromDatabase.getDrawedBody().contains(host.getHostname()));
        assertFalse(templateFromDatabase.getBody().contains(host.getHostname()));

        assertTrue(templateFromDatabase.getDrawedBody().contains(newHostname));
        assertTrue(templateFromDatabase.getBody().contains(newHostname));

        final Template workingVersion = (Template) APILocator.getVersionableAPI().findWorkingVersion(
                template.getIdentifier(), APILocator.systemUser(), false);

        assertFalse(workingVersion.getDrawedBody().contains(host.getHostname()));
        assertTrue(workingVersion.getDrawedBody().contains(newHostname));
    }

    private Template createTemplate(
            final Host host,
            final Container container) {

        final TemplateLayout templateLayout = TemplateLayoutDataGen.get()
                .withContainer(container)
                .next();

        final Contentlet theme = new ThemeDataGen().nextPersisted();
        return new TemplateDataGen()
                .drawedBody(templateLayout)
                .body(String.format(body, host.getHostname()))
                .host(host)
                .theme(theme)
                .nextPersisted();
    }

    private Container createContainer(final User user, final Host host) throws DotSecurityException, DotDataException {
        final ContentType contentType = new ContentTypeDataGen()
                .host(host)
                .nextPersisted();

        Container container = new ContainerAsFileDataGen()
                .contentType(contentType, "")
                .host(host)
                .nextPersisted();

        container = APILocator.getContainerAPI().find(container.getInode(), user, false);
        return container;
    }

    private void waitUntilJobIsFinish() throws InterruptedException, SchedulerException {
        Thread.sleep(500);

        while(true){
            final String[] jobGroupNames = QuartzUtils.getScheduler().getJobGroupNames();

            if ((jobGroupNames.length > 0 &&
                    Arrays.asList(jobGroupNames).contains("update_containers_paths_job")) &&
                    !QuartzUtils.getScheduler().getCurrentlyExecutingJobs().isEmpty()){
                Thread.sleep(500);
            } else {
                break;
            }
        }
    }

    /**
     * Method to Test: {@link ContentletWebAPIImpl#saveContent(Map, boolean, boolean, User)}
     * When: Not Change a Host' name and exists any template using this
     * Should: Not change anything into the template layout
     *
     * */
    @Test
    public void whenHostNameNotChange() throws Exception {

        final User user = APILocator.systemUser();
        init();

        final Host host = new SiteDataGen().nextPersisted();

        final Container container = createContainer(user, host);
        final Template template = createTemplate(host, container);

        final ContentletWebAPIImpl contentletWebAPI = new ContentletWebAPIImpl();
        final Map<String, Object> hostMap = host.getMap();
        hostMap.put("contentletInode", hostMap.get("inode"));

        contentletWebAPI.saveContent(hostMap, false, false, user);

        final String[] jobGroupNames = QuartzUtils.getScheduler().getJobGroupNames();

        assertFalse(jobGroupNames.length > 0 &&
                Arrays.asList(jobGroupNames).contains("update_containers_paths_job"));

        final Template templateFromDatabase = APILocator.getTemplateAPI().find(template.getInode(), user, false);

        assertTrue(templateFromDatabase.getDrawedBody().contains(host.getHostname()));
        assertTrue(templateFromDatabase.getBody().contains(host.getHostname()));
    }

    private void init() {
        final HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(SessionMessages.KEY)).thenReturn(new LinkedHashMap());

        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getSession()).thenReturn(session);

        final WebContext webContext = mock(WebContext.class);
        when(webContext.getHttpServletRequest()).thenReturn(httpServletRequest);

        final WebContextFactory.WebContextBuilder webContextBuilderMock =
                mock(WebContextFactory.WebContextBuilder.class);
        when(webContextBuilderMock.get()).thenReturn(webContext);

        final com.dotcms.repackage.org.directwebremoting.Container containerMock =
                mock(com.dotcms.repackage.org.directwebremoting.Container.class);
        when(containerMock.getBean(WebContextFactory.WebContextBuilder.class)).thenReturn(webContextBuilderMock);

        WebContextFactory.attach(containerMock);
    }
}
