package com.dotcms.rendering.velocity.viewtools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.RoleDataGen;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.FileUtil;
import graphql.Assert;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.BeforeClass;
import org.junit.Test;

public class XsltToolTest extends IntegrationTestBase {

    static final String ENABLE_SCRIPTING = "ENABLE_SCRIPTING";

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * Test Our View Tool still works using the example from our documentation <a href="https://www.dotcms.com/docs/latest/xslttool">xslTool</a>
     * Even after adding the security changes
     * @throws Exception
     */
    @Test
    public void TestTransform() throws Exception {

        final ContentType contentType = APILocator.getContentTypeAPI(APILocator.systemUser())
                .find(Host.HOST_VELOCITY_VAR_NAME);

        Assert.assertNotNull(contentType);

        final Host site = new SiteDataGen().nextPersisted();

        final File binary = new File(Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("xml/demo-stylesheet.xsl")).getFile());
        final Folder folder = new FolderDataGen().name("test").site(site).nextPersisted();

        // Content saved by the system user is deliberately refused: it does not say who wrote the
        // code. So the fixture needs a real author, and one that holds the role.
        final User author = new UserDataGen().roles(scriptingDeveloperRole()).nextPersisted();

        final Contentlet stylesheetAsset = new FileAssetDataGen(folder, binary).user(author).nextPersisted();
        ContentletDataGen.publish(stylesheetAsset);

        final String vtlData = "#set($xmlUrl = 'http://www.w3schools.com/XML/cd_catalog.xml')\n"
                + "#set($xsltUrl = '/test/demo-stylesheet.xsl')\n"
                + "$xslttool.transform($xmlUrl, $xsltUrl, 30)";

        final File file = File.createTempFile("testing-file", ".vtl");
        FileUtil.write(file, vtlData);
        final Contentlet fileAssetShown = new FileAssetDataGen(folder, file).host(site).user(author).nextPersisted();
        ContentletDataGen.publish(fileAssetShown);

        final VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityContext.getCurrentTemplateName()).thenReturn("/"+fileAssetShown.getInode()+"_");

        final HttpServletRequest request = requestWithAttributes();
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpSession session = mock(HttpSession.class);

        final User user = APILocator.systemUser();
        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        request.setAttribute(WebKeys.USER, user);
        request.setAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST, site);
        // What a render reports about the code it is about to run.
        RestrictedViewToolAccess.announceRenderingAuthor(request, author.getUserId());
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(WebKeys.USER_ID)).thenReturn(user.getUserId());
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST)).thenReturn(site);

        final String xmlUrl = "http://www.w3schools.com/XML/cd_catalog.xml";
        final String stylesheetPath = String.format("/%s/%s", folder.getName(), "demo-stylesheet.xsl");

        final boolean enableScripting = Config.getBooleanProperty(ENABLE_SCRIPTING, false);
        try {
            Config.setProperty(ENABLE_SCRIPTING, true);
            final XsltTool tool = new XsltTool();
            tool.init(viewContext);
            final String transform = tool.transform(xmlUrl, stylesheetPath, 30);
            Assert.assertNotNull(transform);
        }finally {
            Config.setProperty(ENABLE_SCRIPTING, enableScripting);
        }
    }

    /**
     * @return a request whose attributes behave like a real one, which a plain mock does not do
     */
    private static HttpServletRequest requestWithAttributes() {
        final Map<String, Object> attributes = new HashMap<>();
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(anyString())).thenAnswer(call -> attributes.get(call.getArgument(0)));
        doAnswer(call -> attributes.put(call.getArgument(0), call.getArgument(1)))
                .when(request).setAttribute(anyString(), any());
        doAnswer(call -> attributes.remove(call.getArgument(0)))
                .when(request).removeAttribute(anyString());
        return request;
    }

    /**
     * @return the Scripting Developer role, created when the starter data does not carry it
     */
    private static Role scriptingDeveloperRole() throws Exception {
        final Role role = APILocator.getRoleAPI().loadRoleByKey(Role.SCRIPTING_DEVELOPER);
        return null != role ? role
                : new RoleDataGen().key(Role.SCRIPTING_DEVELOPER).name(Role.SCRIPTING_DEVELOPER)
                        .nextPersisted();
    }
}
