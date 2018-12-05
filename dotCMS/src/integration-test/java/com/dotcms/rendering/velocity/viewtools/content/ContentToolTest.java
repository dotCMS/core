package com.dotcms.rendering.velocity.viewtools.content;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;

import com.liferay.util.StringPool;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ContentToolTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static Host defaultHost;
    private static HostAPI hostAPI;
    private static Language defaultLanguage;
    private static LanguageAPI languageAPI;
    private static RelationshipAPI relationshipAPI;
    private static UserAPI userAPI;
    private static User user;

	@BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        hostAPI = APILocator.getHostAPI();
        userAPI = APILocator.getUserAPI();
        user    = userAPI.getSystemUser();

        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        fieldAPI       = APILocator.getContentTypeFieldAPI();
        languageAPI    = APILocator.getLanguageAPI();

        relationshipAPI = APILocator.getRelationshipAPI();
        defaultHost     = hostAPI.findDefaultHost(user, false);
        defaultLanguage = languageAPI.getDefaultLanguage();
	}

    @Test
    public void testPullMultiLanguage() throws Exception { // https://github.com/dotCMS/core/issues/11172

    	// Test uses Spanish language
    	final long languageId = 2;

        // Get "News" content-type
        final ContentType contentType = contentTypeAPI.search(" velocity_var_name = 'News'").get(0);


        // Create dummy "News" content in Spanish language
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode()).host(defaultHost).languageId(languageId);

        contentletDataGen.setProperty("title", "El Titulo");
        contentletDataGen.setProperty("byline", "El Sub Titulo");
        contentletDataGen.setProperty("story", "EL Relato");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/el-titulo");

        // Persist dummy "News" contents to ensure at least one result will be returned
        final Contentlet contentlet = contentletDataGen.nextPersisted();

        final ContentTool contentTool = getContentTool(languageId);

        try {
        	// Wait a bit for newly persisted content to be indexed
            Thread.sleep(2000);

            // Ensure that newly persisted content is already indexed
            Assert.assertTrue(
            	contentletAPI.isInodeIndexed(contentlet.getInode())
            );

            // Query contents through Content Tool
            final List<ContentMap> results = contentTool.pull(
            	"+structurename:news +(conhost:"+defaultHost.getIdentifier()+" conhost:system_host) +working:true", 6, "score News.sysPublishDate desc"
            );

            // Ensure that every returned content is in Spanish Language
            Assert.assertFalse(results.isEmpty());
            for(ContentMap cm : results) {
    	    	Assert.assertEquals(cm.getContentObject().getLanguageId(), languageId);
            }
	    } finally {

	    	// Clean-up contents (delete dummy "News" content)
	    	contentletDataGen.remove(contentlet);
	    }
    }

    @Test
    public void testPullRelatedContent_whenRelationshipFieldExists_shouldReturnContentMapList()
            throws DotSecurityException, DotDataException {

        ContentType parentContentType = null;
        ContentType childContentType = null;

        final long time = System.currentTimeMillis();

        try {
            //creates parent content type
            parentContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());

            //creates child content type
            childContentType = contentTypeAPI
                    .save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("childContentType" + time)
                            .owner(user.getUserId()).build());

            Field field = FieldBuilder.builder(RelationshipField.class)
                    .name(childContentType.variable())
                    .contentTypeId(parentContentType.id()).values(String.valueOf(
                            RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                    .relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //creates a new parent contentlet
            ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
            final Contentlet parentContenlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //creates a new child contentlet
            contentletDataGen = new ContentletDataGen(childContentType.id());
            final Contentlet childContenlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .nextPersisted();

            //relates parent contentlet with the child contentlet
            contentletAPI.relateContent(parentContenlet, relationship,
                    CollectionsUtils.list(childContenlet), user, false);

            final ContentTool contentTool = getContentTool(defaultLanguage.getId());

            //pull parent contentlet
            final List<ContentMap> result = contentTool
                    .pull("+identifier:" + parentContenlet.getIdentifier() + " +working:true", 1,
                            null);
            assertNotNull(result);
            assertEquals(1, result.size());

            //lazy load related contentlet
            List relatedContent = (List) result.get(0).get(field.variable());
            assertNotNull(relatedContent);
            assertEquals(1, relatedContent.size());
            assertTrue(relatedContent.get(0) instanceof ContentMap);
            assertEquals(childContenlet.getIdentifier(),
                    ((ContentMap) relatedContent.get(0)).get("identifier"));

        } finally {

            //clean up environment
            if (parentContentType != null && parentContentType.id() != null) {
                contentTypeAPI.delete(parentContentType);
            }

            if (childContentType != null && childContentType.id() != null) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private ContentTool getContentTool(final long languageId){
        // Mock ContentTool to retrieve content in Spanish language
        final ViewContext viewContext = mock(ViewContext.class);
        final Context velocityContext = mock(Context.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpSession session = mock(HttpSession.class);

        when(viewContext.getVelocityContext()).thenReturn(velocityContext);
        when(viewContext.getRequest()).thenReturn(request);
        when(request.getParameter("host_id")).thenReturn(defaultHost.getInode());
        when(request.getParameter("language_id")).thenReturn(String.valueOf(languageId));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(user);

        final ContentTool contentTool = new ContentTool();
        contentTool.init(viewContext);
        return contentTool;
    }
}
