package com.dotmarketing.servlets.ajax;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.publisher.bundle.bean.Bundle;
import com.dotcms.publisher.business.DotPublisherException;
import com.dotcms.publisher.business.PublishQueueElement;
import com.dotcms.publisher.business.PublisherAPI;
import com.dotcms.publisher.environment.bean.Environment;
import com.dotcms.publishing.FilterDescriptor;
import com.dotcms.rest.InitDataObject;
import com.dotcms.rest.WebResource;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.DateUtil;
import com.google.common.collect.ImmutableMap;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class AjaxDirectorServletIntegrationTest {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @DataProvider
    public static Object[] testCases(){

        // first test case RemotePublishAjaxAction.addToBundle
        final AjaxDirectorServletTestCase remotePublishAjaxActionAddToBundle = new AjaxDirectorServletTestCase();

        final String assetIdentifier = "71bf1747-56b9-41ca-a3fa-1fc7b8471dba";
        final String bundleName = "testingBundle";

        remotePublishAjaxActionAddToBundle.setRequestJSON("{\n"
                + "    \"assetIdentifier\": \""+assetIdentifier+"\" ,\n"
                + "    \"bundleName\": \""+bundleName+"\",\n"
                + "}");

        remotePublishAjaxActionAddToBundle.setRequestURI("/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/addToBundle");

        remotePublishAjaxActionAddToBundle.setAssertion(() -> {
            try {
                Bundle bundle = APILocator.getBundleAPI().getBundleByName("testingBundle");

                PublisherAPI publisherAPI = PublisherAPI.getInstance();
                List<PublishQueueElement> foundElements =
                        publisherAPI.getQueueElementsByBundleId(bundle.getId());

                final boolean anyMatch = foundElements.stream().anyMatch(
                        (element) -> element.getAsset()
                                .equals("71bf1747-56b9-41ca-a3fa-1fc7b8471dba"));

                assertTrue("Asset added to the Bundle", anyMatch);
            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }

        });

        remotePublishAjaxActionAddToBundle.setDisposer(() -> Sneaky.sneaked(()-> {
            Bundle bundle = APILocator.getBundleAPI().getBundleByName("testingBundle");

            if (bundle != null) {
                APILocator.getBundleAPI().deleteBundle(bundle.getId());
            }
        }));

        // second test case = RemotePublishAjaxAction.pushBundle
        final AjaxDirectorServletTestCase remotePublishAjaxActionPushBundle = new AjaxDirectorServletTestCase();

        final String bundleId = UUID.randomUUID().toString();

        final Date publishDate = Date.from(LocalDate.of(2001, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        final String publishDateString = DateUtil.format(publishDate, "yyyy-MM-dd");

        final Date expireDate = Date.from(LocalDate.of(2051, 1, 1)
                .atStartOfDay(ZoneId.systemDefault()).toInstant());

        final String expireDateString = DateUtil.format(expireDate, "yyyy-MM-dd");

        final List<String> environmentsIds = Arrays.asList(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), UUID.randomUUID().toString());

        final String environmentIdsString = String.join(",", environmentsIds);

        remotePublishAjaxActionPushBundle.setInitializer(() -> {
            try {
                environmentsIds.forEach((environmentId) -> {
                    try {
                        final Environment environment = new Environment();
                        environment.setId(environmentId);
                        environment.setName(environmentId);
                        environment.setPushToAll(true);
                        APILocator.getEnvironmentAPI().saveEnvironment(environment, null);
                    } catch(DotDataException | DotSecurityException e) {
                        throw new RuntimeException(e);
                    }
                });

                final Bundle bundle = new Bundle();
                bundle.setId(bundleId);
                bundle.setPublishDate(publishDate);
                bundle.setExpireDate(expireDate);
                bundle.setOwner(APILocator.systemUser().getUserId());

                APILocator.getBundleAPI().saveBundle(bundle);

                // add an asset to the bundle
                PublisherAPI.getInstance()
                        .saveBundleAssets(List.of(assetIdentifier),
                                bundle.getId(), APILocator.systemUser());
            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }
        });


        remotePublishAjaxActionPushBundle.setRequestJSON("{\n"
                + "    \"assetIdentifier\": \""+bundleId+"\",\n"
                + "    \"remotePublishDate\":\""+publishDateString+"\",\n"
                + "    \"remotePublishTime\":\"10-00\",\n"
                + "    \"remotePublishExpireDate\":\""+expireDateString+"\",\n"
                + "    \"remotePublishExpireTime\":\"10-00\",\n"
                + "    \"iWantTo\":\"publish\",\n"
                + "    \"whoToSend\":\""+environmentIdsString+"\"\n"
                + "}");

        remotePublishAjaxActionPushBundle.setRequestURI("/DotAjaxDirector/com.dotcms.publisher.ajax.RemotePublishAjaxAction/cmd/pushBundle");

        remotePublishAjaxActionPushBundle.setAssertion(() -> {
            try {
                List<String> foundEnvironmentsIdsByBundle = APILocator.getEnvironmentAPI()
                        .findEnvironmentsByBundleId(bundleId).stream().map(Environment::getId)
                        .collect(Collectors.toList());

                assertTrue("Bundle ready to be pushed to all environments",
                    foundEnvironmentsIdsByBundle.containsAll(environmentsIds));

                assertTrue(PublisherAPI.getInstance()
                        .getQueueElementsByBundleId(bundleId).stream()
                        .allMatch((queueElement)->
                                queueElement.getOperation()==PublisherAPI.ADD_OR_UPDATE_ELEMENT));

            } catch(DotDataException | DotPublisherException e) {
                throw new RuntimeException(e);
            }

        });

        remotePublishAjaxActionPushBundle.setDisposer(() -> Sneaky.sneaked(()-> {
            Bundle bundle = APILocator.getBundleAPI().getBundleById(bundleId);

            if(bundle != null) {
                APILocator.getBundleAPI().deleteBundle(bundle.getId());
            }

            environmentsIds.forEach((environmentId) -> Sneaky.sneaked(()-> {
                APILocator.getEnvironmentAPI().deleteEnvironment(environmentId);
            }));
        }));



        return new AjaxDirectorServletTestCase[] {
                remotePublishAjaxActionAddToBundle,
                remotePublishAjaxActionPushBundle
        };

    }

    @Test
    @UseDataProvider("testCases")
    public void testService(final AjaxDirectorServletTestCase testCase)
            throws IOException, ServletException {

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final HttpSession session = mock(HttpSession.class);

        final WebResource webResource = mock(WebResource.class);
        final InitDataObject dataObject = mock(InitDataObject.class);
        when(dataObject.getUser()).thenReturn(APILocator.systemUser());
        when(webResource.init(nullable(String.class), anyBoolean(), any(HttpServletRequest.class),
                anyBoolean(), any()))
                .thenReturn(dataObject);

        final AjaxDirectorServlet ajaxDirectorServlet = new AjaxDirectorServlet(webResource);

        final Reader inputString = new StringReader(testCase.getRequestJSON());

        final BufferedReader reader = new BufferedReader(inputString);
        when(request.getReader()).thenReturn(reader);
        when(request.getRequestURI()).thenReturn(testCase.getRequestURI());
        when(request.getSession()).thenReturn(session);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));

        try {
            testCase.getInitializer().initialize();

            ajaxDirectorServlet.service(request, response);

            testCase.getAssertion().executeAssertion();
        } finally {
            testCase.getDisposer().dispose();
        }
    }
}
