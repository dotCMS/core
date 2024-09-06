package com.dotcms.rendering.velocity.viewtools;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.FileUtil;
import graphql.Assert;
import java.io.File;
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
        final Contentlet stylesheetAsset = new FileAssetDataGen(folder, binary).nextPersisted();
        ContentletDataGen.publish(stylesheetAsset);

        final String vtlData = "#set($xmlUrl = 'http://www.w3schools.com/XML/cd_catalog.xml')\n"
                + "#set($xsltUrl = '/test/demo-stylesheet.xsl')\n"
                + "$xslttool.transform($xmlUrl, $xsltUrl, 30)";

        final File file = File.createTempFile("testing-file", ".vtl");
        FileUtil.write(file, vtlData);
        final Contentlet fileAssetShown = new FileAssetDataGen(folder, file).host(site).nextPersisted();
        ContentletDataGen.publish(fileAssetShown);

        final VelocityContext velocityContext = mock(VelocityContext.class);
        when(velocityContext.getCurrentTemplateName()).thenReturn("/"+fileAssetShown.getInode()+"_");

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final ViewContext viewContext = mock(ViewContext.class);
        final HttpSession session = mock(HttpSession.class);

        final User user = APILocator.systemUser();
        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getAttribute(WebKeys.USER)).thenReturn(user);
        when(request.getAttribute(com.dotmarketing.util.WebKeys.CURRENT_HOST)).thenReturn(site);
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
}