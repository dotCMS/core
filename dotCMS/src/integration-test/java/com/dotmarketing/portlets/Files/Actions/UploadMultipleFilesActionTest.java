package com.dotmarketing.portlets.Files.Actions;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.*;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockParameterRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.mock.response.MockHttpStatusAndHeadersResponse;
import com.dotcms.repackage.javax.portlet.*;
import com.dotcms.repackage.org.apache.struts.action.ActionForm;
import com.dotcms.repackage.org.directwebremoting.util.FakeHttpServletRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotcms.workflow.form.WorkflowActionForm;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.files.action.UploadMultipleFilesAction;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.SystemException;
import com.liferay.portal.action.LayoutAction;
import com.liferay.portal.ejb.PortletManagerUtil;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.User;
import com.liferay.portal.struts.PortletRequestProcessor;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.ActionRequestImpl;
import com.liferay.portlet.ConcretePortletWrapper;
import com.liferay.portlet.StrutsPortlet;
import com.liferay.portlet.admin.model.AdminConfig;
import com.liferay.util.ParamUtil;
import com.liferay.util.servlet.DynamicServletRequest;
import com.liferay.util.servlet.UploadServletRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;

public class UploadMultipleFilesActionTest extends IntegrationTestBase {
        @BeforeClass
        public static void prepare() throws Exception {
                //Setting web app environment
                IntegrationTestInitService.getInstance().init();
        }

        /**
         * Method to test:
         * Given Scenario:
         * ExpectedResult:
         *
         */
        @Test
        public void  test_uploadMultipleFiles_shouldUploadFilesOnSelectedLanguage() throws Exception {
                final UploadMultipleFilesAction action = new UploadMultipleFilesAction();
                User adminUser = APILocator.getUserAPI().loadByUserByEmail("admin@dotcms.com", APILocator.systemUser(), false);

                final String hostName = "demo.dotcms.com-" + System.currentTimeMillis();
                final Host host = new SiteDataGen().name(hostName).nextPersisted();
                final Folder folderImages = new FolderDataGen().site(host).name("images").nextPersisted();
                Portlet portlet = new Portlet("site-browser", "com.liferay.portlet.JSPPortlet", new HashMap<>());
                PortletConfig portletConfig = APILocator.getPortletAPI().getPortletConfig(portlet);
                PortletContext portletCtx = portletConfig.getPortletContext();

                final ConcretePortletWrapper concretePortletWrapper = mock(ConcretePortletWrapper.class);
                final WindowState windowState = mock(WindowState.class);
                final PortletMode portletMode = mock(PortletMode.class);
                final PortletPreferences portletPrefs = mock(PortletPreferences.class);
                final String layoutId = "1";

                Map<String, String> requestParams = Map.of(
                        WebKeys.PORTLET_URL_PORTLET_NAME, portlet.getPortletId(),
                        WebKeys.PORTLET_URL_ACTION, Boolean.TRUE.toString(),
                        "parent", folderImages.getIdentifier(),
                        "fileNames", "Test-file-1.jpgcom.dotmarketing.contentlet.form_name_value_separatorTest-file-2.jpg"
                );
                MockHeaderRequest headerRequest = new MockHeaderRequest(new MockHttpRequestIntegrationTest(
                        hostName, "/upload-multiple-files").request());
                headerRequest.setHeader("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
                MockParameterRequest paramReq = new MockParameterRequest(
                        new MockSessionRequest(headerRequest), requestParams);
                final UploadServletRequest uploadRequest = new UploadServletRequest(paramReq);

                uploadRequest.getSession().setAttribute(com.dotmarketing.util.WebKeys.CONTENT_SELECTED_LANGUAGE, 2);
                uploadRequest.getSession().setAttribute(com.dotmarketing.util.WebKeys.LANGUAGE_SEARCHED, 2);
                ActionRequestImpl actionRequest =
                        new ActionRequestImpl(uploadRequest, portlet, concretePortletWrapper, portletCtx, windowState, portletMode, portletPrefs, layoutId );

                final ActionResponse actionResponse = mock(ActionResponse.class);
                final ActionForm actionForm = mock(ActionForm.class);

                actionRequest.setAttribute("struts_action","/ext/files/upload_multiple");
                action._saveFileAsset(actionRequest,actionResponse,portletConfig, actionForm, adminUser ,"upload");


        }
}