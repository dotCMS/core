package com.dotcms.rendering.velocity.directive;

import static org.mockito.Mockito.mock;

import com.dotcms.IntegrationTestBase;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FileAssetDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rendering.velocity.util.VelocityUtil;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.PageMode;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.File;
import java.io.Writer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.velocity.context.Context;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class DotParseTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    /**
     * Method to test: {@link DotParse#resolveTemplatePath(Context, Writer, RenderParams, String[])}
     * Given Scenario: If you upload a vtl file to a binary field, dotParse should be able to resolve
     *                  it and parse to velocity by passing the IdPath of the file (/dA/{identifier}/{fieldVar})
     * ExpectedResult: data written in the vtl file should be resolved and parsed
     */
    @Test
    public void Test_DotParse_UsingABinaryField_success() throws Exception {
        final Host host = new SiteDataGen().nextPersisted();
        final Folder folder = new FolderDataGen().site(host).nextPersisted();
        final User user = TestUserUtils.getAdminUser();
        //Create VTL file
        final String vtlData = "<h1>test</h1>";
        final File file = File.createTempFile("testing-file", ".vtl");
        FileUtil.write(file, vtlData);
        final Contentlet fileAssetShown = new FileAssetDataGen(folder, file).host(host).nextPersisted();
        ContentletDataGen.publish(fileAssetShown);
        //Get ShortyId of the contentlet
        final String shortyId = APILocator.getShortyAPI().shortify(fileAssetShown.getIdentifier());

        final String velocityCode = "#dotParse(\"/dA/"+shortyId+"/fileAsset\")";
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final Language language = APILocator.getLanguageAPI().getDefaultLanguage();
        final RenderParams renderParams = new RenderParams(user,language,host, PageMode.PREVIEW_MODE);
        Mockito.when(request.getAttribute(RenderParams.RENDER_PARAMS_ATTRIBUTE)).thenReturn(renderParams);
        final Context velocityContext   = VelocityUtil.getInstance().getContext(request, response);
        //Evaluate velocity Code
        final String parsedCode = VelocityUtil.eval(velocityCode, velocityContext);
        //Should return the data of the vtl file
        Assert.assertNotNull(parsedCode);
        Assert.assertEquals(vtlData,parsedCode);

    }


}
