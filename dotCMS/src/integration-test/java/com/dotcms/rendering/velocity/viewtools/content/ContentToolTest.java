package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;

import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentToolTest extends IntegrationTestBase {

	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
	}

    @Test
    public void testPullMultiLanguage() throws Exception { // https://github.com/dotCMS/core/issues/11172

    	// Test uses Spanish language
    	long LANGUAGE_ID = 2;

        User user = APILocator.getUserAPI().getSystemUser();

        // Get "Demo" host and "News" content-type
        Host host = APILocator.getHostAPI().findByName("demo.dotcms.com", user, false);
        ContentType contentType = APILocator.getContentTypeAPI(user).search(" velocity_var_name = 'News'").get(0);


        // Create dummy "News" content in Spanish language
        ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode()).host(host).languageId(LANGUAGE_ID);

        contentletDataGen.setProperty("title", "El Titulo");
        contentletDataGen.setProperty("byline", "El Sub Titulo");
        contentletDataGen.setProperty("story", "EL Relato");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/el-titulo");

        // Persist dummy "News" contents to ensure at least one result will be returned
        Contentlet contentlet = contentletDataGen.nextPersisted();


        // Mock ContentTool to retrieve content in Spanish language 
        ViewContext viewContext = mock(ViewContext.class);
        Context velocityContext = mock(Context.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getParameter("host_id")).thenReturn(host.getInode());
        when(request.getParameter("language_id")).thenReturn(String.valueOf(LANGUAGE_ID));

        ContentTool contentTool = new ContentTool();
        contentTool.init(viewContext);


        try {
        	// Wait a bit for newly persisted content to be indexed
            Thread.sleep(2000);

            // Ensure that newly persisted content is already indexed
            Assert.assertTrue(
            	APILocator.getContentletAPI().isInodeIndexed(contentlet.getInode())
            );

            // Query contents through Content Tool
            List<ContentMap> results = contentTool.pull(
            	"+structurename:news +(conhost:"+host.getIdentifier()+" conhost:system_host) +working:true", 6, "score News.sysPublishDate desc"
            );

            // Ensure that every returned content is in Spanish Language
            Assert.assertFalse(results.isEmpty());
            for(ContentMap cm : results) {
    	    	Assert.assertEquals(cm.getContentObject().getLanguageId(), LANGUAGE_ID);
            }
	    } finally {

	    	// Clean-up contents (delete dummy "News" content)
	    	contentletDataGen.remove(contentlet);
	    }
    }
}
