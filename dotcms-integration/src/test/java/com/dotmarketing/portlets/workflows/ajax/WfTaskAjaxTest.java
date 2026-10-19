package com.dotmarketing.portlets.workflows.ajax;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants;
import com.liferay.portal.model.User;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WfTaskAjaxTest extends IntegrationTestBase {

    private ContentType contentType;

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @After
    public void cleanUp() {
        if (contentType != null) {
            ContentTypeDataGen.remove(contentType);
        }
    }

    @Test
    public void executeActions_recordsRequestingUserAsModifier() throws Exception {
        final User user = new UserDataGen()
                .roles(APILocator.getRoleAPI().loadBackEndUserRole(),
                        APILocator.getRoleAPI().loadCMSAdminRole())
                .nextPersisted();
        contentType = new ContentTypeDataGen()
                .workflowId(SystemWorkflowConstants.SYSTEM_WORKFLOW_ID)
                .baseContentType(BaseContentType.CONTENT)
                .field(new FieldDataGen().name("title").velocityVarName("title").next())
                .nextPersisted();
        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty("title", "bulk action content")
                .languageId(languageId)
                .nextPersisted();

        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter(Contentlet.WORKFLOW_ACTION_KEY))
                .thenReturn(SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID);
        when(request.getParameter("wfCons")).thenReturn(contentlet.getInode());
        final StringWriter output = new StringWriter();
        final HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        final WfTaskAjax wfTaskAjax = new WfTaskAjax();
        wfTaskAjax.setUser(user);
        wfTaskAjax.executeActions(request, response);

        final Contentlet live = APILocator.getContentletAPI().findContentletByIdentifier(
                contentlet.getIdentifier(), true, languageId, APILocator.systemUser(), false);
        assertTrue(output.toString(), live.isLive());
        assertEquals(user.getUserId(), live.getModUser());
    }

}
