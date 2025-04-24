package com.dotcms.rendering.velocity.viewtools.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.LanguageDataGen;
import com.dotcms.datagen.TestDataUtils;
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
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.PageMode;
import com.dotmarketing.util.PaginatedArrayList;
import com.dotmarketing.util.PaginatedContentList;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import io.vavr.control.Try;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.velocity.context.Context;
import org.apache.velocity.tools.view.context.ViewContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class ContentToolTest extends IntegrationTestBase {
    public static final String QUERY_BY_STRUCTURE_NAME = "+structureName:%s";
    public static final String SYS_PUBLISH_DATE = "sysPublishDate";
    public static final String SYS_EXPIRE_DATE = "sysExpireDate";

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

    public static class TestCase {
        int cardinality;
        Class parentExpectedType;
        Class childExpectedType;
        boolean selfRelated;

        public TestCase(final int cardinality, final Class parentExpectedType,
                final Class childExpectedType, final boolean selfRelated) {
            this.cardinality = cardinality;
            this.parentExpectedType = parentExpectedType;
            this.childExpectedType = childExpectedType;
            this.selfRelated = selfRelated;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                new TestCase(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), List.class,
                        List.class, false),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), ContentMap.class,
                        List.class, false),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal(), ContentMap.class,
                        ContentMap.class, false),
                new TestCase(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), List.class,
                        List.class, true),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), ContentMap.class,
                        List.class, true),
                new TestCase(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal(), ContentMap.class,
                        ContentMap.class, true)
        };
    }

    @Test
    public void testPullMultiLanguage() throws Exception { // https://github.com/dotCMS/core/issues/11172

    	// Test uses Spanish language
    	final long languageId = TestDataUtils.getSpanishLanguage().getId();

        // Get "News" content-type
        final ContentType contentType = TestDataUtils.getNewsLikeContentType();

        // Create dummy "News" content in Spanish language
        final ContentletDataGen contentletDataGen = new ContentletDataGen(contentType.inode()).host(defaultHost).languageId(languageId);

        contentletDataGen.setProperty("title", "El Titulo");
        contentletDataGen.setProperty("byline", "El Sub Titulo");
        contentletDataGen.setProperty("story", "EL Relato");
        contentletDataGen.setProperty("sysPublishDate", new Date());
        contentletDataGen.setProperty("urlTitle", "/news/el-titulo");

        // Persist dummy "News" contents to ensure at least one result will be returned
        contentletDataGen.nextPersisted();

        final ContentTool contentTool = getContentTool(languageId);

        // Query contents through Content Tool
        final List<ContentMap> results = contentTool.pull(
                "+structurename:" + contentType.variable() + " +(conhost:" + defaultHost
                        .getIdentifier() + " conhost:system_host) +working:true", 6,
                "score " + contentType.variable() + ".sysPublishDate desc"
        );

        // Ensure that every returned content is in Spanish Language
        Assert.assertFalse(results.isEmpty());
        for (ContentMap cm : results) {
            Assert.assertEquals(cm.getContentObject().getLanguageId(), languageId);
        }
    }

    @Test
    public void testPullRelated() throws DotDataException, DotSecurityException {

        final long time = System.currentTimeMillis();

        //creates parent content type
        ContentType parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

        //creates child content type
        ContentType childContentType = createAndSaveSimpleContentType("childContentType" + time);

        Field field = createField(childContentType.variable(), parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

        //One side of the relationship is set parentContentType --> childContentType
        field = fieldAPI.save(field, user);

        //creates a new parent contentlet and publishes it
        ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());

        final Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();
        ContentletDataGen.publish(parentContentlet);

        //creates children contentlets and publishes them
        contentletDataGen = new ContentletDataGen(childContentType.id());
        final Contentlet childContentlet1 = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet childContentlet2 = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        ContentletDataGen.publish(childContentlet1);
        ContentletDataGen.publish(childContentlet2);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + field.variable();

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship,
                CollectionsUtils.list(childContentlet1, childContentlet2), user, false);

        //refresh relationships in the ES index
        contentletAPI.reindex(parentContentlet);
        contentletAPI.reindex(childContentlet1);
        contentletAPI.reindex(childContentlet2);

        final ContentTool contentTool = getContentTool(defaultLanguage.getId());

        final List<ContentMap> result = contentTool
                .pullRelated(relationship.getRelationTypeValue(),
                        parentContentlet.getIdentifier(), "+live:true", false, -1, null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().map(elem -> elem.getContentObject().getIdentifier())
                .allMatch(identifier -> identifier.equals(childContentlet1.getIdentifier())
                        || identifier.equals(childContentlet2.getIdentifier())));
    }

    @Test
    public void testPullRelatedField_success() throws DotDataException, DotSecurityException {

        final long time = System.currentTimeMillis();

        //creates parent content type
        ContentType parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

        //creates child content type
        ContentType childContentType = createAndSaveSimpleContentType("childContentType" + time);

        Field field = createField(childContentType.variable(), parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

        //One side of the relationship is set parentContentType --> childContentType
        field = fieldAPI.save(field, user);

        //creates a new parent contentlet
        ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
        final Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        ContentletDataGen.publish(parentContentlet);

        //creates a new child contentlet
        contentletDataGen = new ContentletDataGen(childContentType.id());
        final Contentlet childContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        ContentletDataGen.publish(childContentlet);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + field.variable();

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship,
                CollectionsUtils.list(childContentlet), user, false);

        //refresh relationships in the ES index
        contentletAPI.reindex(parentContentlet);
        contentletAPI.reindex(childContentlet);

        final ContentTool contentTool = getContentTool(defaultLanguage.getId());

        final List<ContentMap> result = contentTool
                .pullRelatedField(
                        parentContentlet.getIdentifier(), fullFieldVar, "+working:true");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(childContentlet.getIdentifier(),
                result.get(0).getContentObject().getIdentifier());
    }

    @Test(expected = RuntimeException.class)
    public void testPullRelatedField_whenInvalidFieldIsSent_throwsAnException()
            throws DotDataException, DotSecurityException {

        final long time = System.currentTimeMillis();

        //creates parent content type
        ContentType parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

        //creates child content type
        ContentType childContentType = createAndSaveSimpleContentType("childContentType" + time);

        Field field = createField(childContentType.variable(), parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

        //One side of the relationship is set parentContentType --> childContentType
        field = fieldAPI.save(field, user);

        //creates a new parent contentlet
        ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
        final Contentlet parentContenlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        final ContentTool contentTool = getContentTool(defaultLanguage.getId());

        contentTool.pullRelatedField(
                parentContenlet.getIdentifier(), field.variable(), "+working:true");
    }

    @Test
    @UseDataProvider("testCases")
    public void testPullRelatedContent_whenRelationshipFieldExists(final TestCase testCase)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();

        //creates parent content type
        ContentType parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

        //creates child content type
        ContentType childContentType = testCase.selfRelated? parentContentType: createAndSaveSimpleContentType("childContentType" + time);

        Field parentField = createField(childContentType.variable(), parentContentType.id(),
                childContentType.variable(), String.valueOf(testCase.cardinality));

        //One side of the relationship is set parentContentType --> childContentType
        parentField = fieldAPI.save(parentField, user);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + parentField.variable();

        Field childField = createField(parentContentType.variable(), childContentType.id(),
                fullFieldVar, String.valueOf(testCase.cardinality));

        //The other side of the relationship is set childContentType --> parentContentType
        childField = fieldAPI.save(childField, user);

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //creates a new parent contentlet
        ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());
        Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        //creates a new child contentlet
        contentletDataGen = new ContentletDataGen(childContentType.id());
        Contentlet childContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship,
                CollectionsUtils.list(childContentlet), user, false);

        //refresh relationships in the ES index
        contentletAPI.reindex(parentContentlet);
        contentletAPI.reindex(childContentlet);

        //pull and validate child
        validateRelationshipSide(testCase.childExpectedType, parentField, parentContentlet,
                childContentlet);

        //pull and validate parent
        validateRelationshipSide(testCase.parentExpectedType, childField, childContentlet,
                parentContentlet);
    }

    private void validateRelationshipSide(final Class expectedType, final Field field,
            final Contentlet leftSideContentlet, final Contentlet rightSideContentlet) {
        final ContentTool contentTool = getContentTool(defaultLanguage.getId());


        final List<ContentMap> result = contentTool
                .pull("+identifier:" + leftSideContentlet.getIdentifier() + " +working:true", 1,
                        null);
        assertNotNull(result);
        assertEquals(1, result.size());

        //lazy load related contentlet
        final Object relatedContent = result.get(0).get(field.variable());
        assertNotNull(relatedContent);
        assertTrue(expectedType.isInstance(relatedContent));

        if (expectedType.equals(List.class)){
            final List relatedContentList = (List)relatedContent;
            assertEquals(1, relatedContentList.size());
            assertEquals(rightSideContentlet.getIdentifier(),
                    ((ContentMap) relatedContentList.get(0)).get("identifier"));
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }


    private Field createField(String fieldName, String contentTypeId, String relationType, String cardinality){
        return FieldBuilder.builder(RelationshipField.class).name(fieldName)
                .contentTypeId(contentTypeId).values(cardinality)
                .relationType(relationType).required(false).build();
    }

    private ContentTool getContentTool(final long languageId) {
	    return getContentTool(languageId, PageMode.PREVIEW_MODE);
    }

    private ContentTool getContentTool(final long languageId, final PageMode pageMode){
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
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getParameter(com.dotmarketing.util.WebKeys.PAGE_MODE_PARAMETER)).thenReturn(pageMode.name());
        when(session.getAttribute(com.dotmarketing.util.WebKeys.CMS_USER)).thenReturn(user);

        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

        final ContentTool contentTool = new ContentTool();
        contentTool.init(viewContext);
        return contentTool;
    }

    /*


    /**
     * Method to test: {@link ContentTool#pullPerPage(String, int, int, String)} 
     * When: there is a content with a publish date in the future and the time machine parameter in null
     * Should: Not return the content
     */
    @Test
    public void whenTheTimeMachineDateIsNullAndPublishDateInFutureShouldNotReturnAnything() {
        final Calendar contentPublishDate = Calendar.getInstance();
        contentPublishDate.add(Calendar.DATE, 1);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, contentPublishDate.getTime())
                .nextPersisted();

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final ContentTool contentTool = getContentTool(null);

        final PaginatedContentList<ContentMap> contents = contentTool.pullPerPage(query, 1, 2, null);
        assertFalse(Try.of(()->contents.get(0).isLive()).getOrElse(false));
    }

    /**
     * Method to test: {@link ContentTool#pullPerPage(String, int, int, String)}
     * When: there is a content with a publish date set to tomorrow and the time machine date is the date after tomorrow
     * Should: return one content
     */
    @Test
    public void whenTheTimeMachineDateAndPublishDateAreTomorrowShouldReturnOneContent() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 2);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .languageId(1)
                .nextPersisted();

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final ContentTool contentTool = getContentTool(timeMachine);

        final PaginatedContentList<ContentMap> contents = contentTool.pullPerPage(query, 1, 2, null);

        assertEquals(1  , contents.size());
        assertEquals(1  , contents.getTotalResults());
        assertEquals(contentlet.getIdentifier(), contents.get(0).getContentObject().getIdentifier());
    }

    @NotNull
    private ContentTool getContentTool(Calendar timeMachine) {
        final ContentTool contentTool  = new ContentTool();

        final String time = timeMachine != null ? Long.toString(timeMachine.getTime().getTime()) : null;

        final HttpSession session = mock(HttpSession.class);
        final HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletRequestThreadLocal.INSTANCE.setRequest(request);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(request.getAttribute(WebKeys.USER)).thenReturn(APILocator.systemUser());

        when(session.getAttribute("tm_date")).thenReturn(time);

        final ViewContext viewContext = mock(ViewContext.class);
        when(viewContext.getRequest()).thenReturn(request);

        contentTool.init(viewContext);
        return contentTool;
    }

    /**
     * Method to test: {@link ContentTool#pullPerPage(String, int, int, String)}
     * When: there is a content with a expire  date set to tomorrow and the time machine date is the date after tomorrow
     * Should: return a empty list
     */
    @Test
    public void whenTheTimeMachineDateIsAfterTomorrowAndExpireDateIsTomorrowShouldNotReturnContent() {
        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 1);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar afterTomorrow = Calendar.getInstance();
        afterTomorrow.add(Calendar.DATE, 2);

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final ContentTool contentTool = getContentTool(afterTomorrow);

        final PaginatedContentList<ContentMap> contents = contentTool.pullPerPage(query, 1, 2, null);

        assertTrue(contents.isEmpty());
    }

    /**
     * Method to test: {@link ContentTool#pullPerPage(String, int, int, String)}
     * When: there is a content with a publish date set to tomorrow and expire date set in the future
     * and the time machine date is set to after tomorrow
     * Should: return one content
     */
    @Test
    public void whenTheTimeMachineDateIsAfterTomorrowAndExpireDateIsInFutureShouldReturnContent() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 3);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        final Contentlet contentlet = new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 2);

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final ContentTool contentTool = getContentTool(timeMachine);
        final PaginatedContentList<ContentMap> contents = contentTool.pullPerPage(query, 1, 2, null);

        assertEquals(1, contents.size());
        assertEquals(contentlet.getIdentifier(), contents.get(0).getContentObject().getIdentifier());
    }

    /**
     * Method to test: {@link ContentTool#pullPerPage(String, int, int, String)}
     * When: there is a content with a publish date set to tomorrow and expire date set to after tomorrow
     * and the time machine date set after that
     * Should: return no one content
     */
    @Test
    public void whenPublishAndExpireDatesAreInTheFutureAndTimeMachineIsAfterBoth() {
        final Calendar publishDate = Calendar.getInstance();
        publishDate.add(Calendar.DATE, 1);

        final Calendar expireDate = Calendar.getInstance();
        expireDate.add(Calendar.DATE, 2);

        final ContentType contentType = TestDataUtils.getNewsLikeContentType();
        new ContentletDataGen(contentType.id())
                .setPolicy(IndexPolicy.FORCE)
                .setProperty(SYS_PUBLISH_DATE, publishDate.getTime())
                .setProperty(SYS_EXPIRE_DATE, expireDate.getTime())
                .nextPersisted();

        final Calendar timeMachine = Calendar.getInstance();
        timeMachine.add(Calendar.DATE, 3);

        final String query = String.format(QUERY_BY_STRUCTURE_NAME, contentType.variable());

        final ContentTool contentTool = getContentTool(timeMachine);
        final PaginatedContentList<ContentMap> contents = contentTool.pullPerPage(query, 1, 2, null);

        assertEquals(0, contents.size());
    }

    /**
     * Method to Test: {@link ContentTool#pull(String, int, String)}
     * When:
     * - With the PageMode in {@link PageMode#PREVIEW_MODE} and {@link PageMode#EDIT_MODE}
     * - Create a {@link Relationship}
     * - Create a parent content and publish it
     * - Create one child and publish it
     * - Create another child and just save not publish it
     * - Create a third child and publish it, later create a different working version
     * - Use the pull method to get the parent related content.
     * - Later try to get the child using the parent content properties
     * Should: Return all the child with the working version
     * 
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testPullRelatedParentContentType() throws DotDataException, DotSecurityException {

        final ContentType parentContentType = new ContentTypeDataGen().nextPersisted();
        final ContentType childContentType = new ContentTypeDataGen().nextPersisted();

        Field field = createField(
                childContentType.variable(),
                parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));

        field = fieldAPI.save(field, user);

        //creates a new parent contentlet and publishes it
        final Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                .nextPersisted();
        ContentletDataGen.publish(parentContentlet);

        //creates children contentlets and publishes them
        final Contentlet childContentlet1 = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet childContentlet2 = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        Contentlet childContentlet3Live = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        ContentletDataGen.publish(childContentlet1);
        ContentletDataGen.publish(childContentlet3Live);

        final Contentlet checkout = ContentletDataGen.checkout(childContentlet3Live);
        final Contentlet childContentlet3Working = ContentletDataGen.checkin(checkout);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + field.variable();

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship,
                CollectionsUtils.list(childContentlet1, childContentlet2, childContentlet3Working),
                user, false);

        //refresh relationships in the ES index
        contentletAPI.reindex(parentContentlet);
        contentletAPI.reindex(childContentlet1);
        contentletAPI.reindex(childContentlet2);
        contentletAPI.reindex(childContentlet3Working);

        final PageMode[] pageModes = {PageMode.PREVIEW_MODE, PageMode.EDIT_MODE};

        for (final PageMode pageMode : pageModes) {

            final ContentTool contentTool = getContentTool(defaultLanguage.getId(), pageMode);

            final String query = String.format("+contentType:%s", parentContentType.variable());
            final List<ContentMap> parentContent = contentTool.pull(query, 10, "modDate desc");

            assertNotNull(parentContent);
            assertEquals(1, parentContent.size());

            final Collection<ContentMap> relatedContent = (Collection) parentContent.get(0).get(field.variable());

            assertEquals(3, relatedContent.size());
            assertTrue(
                    relatedContent.stream()
                            .map(contentlet -> contentlet.get("inode"))
                            .allMatch(inode -> inode.equals(childContentlet1.getInode())
                                    || inode.equals(childContentlet2.getInode())
                                    || inode.equals(childContentlet3Working.getInode())
                            ));
        }
    }

    /**
     * Method to Test: {@link ContentTool#pull(String, int, String)}
     * When:
     * - With the PageMode in {@link PageMode#LIVE}
     * - Create a {@link Relationship}
     * - Create a parent content and publish it
     * - Create one child and publish it
     * - Create another child and just save not publish it
     * - Create a third child and publish it, later create a different working version
     * - Use the pull method to get the parent related content.
     * - Later try to get the child using the parent content properties
     * Should: Return just two childs with the live version
     *
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testPullRelatedParentContentTypeInLiveMode() throws DotDataException, DotSecurityException {

        final ContentType parentContentType = new ContentTypeDataGen().nextPersisted();
        final ContentType childContentType = new ContentTypeDataGen().nextPersisted();

        Field field = createField(
                childContentType.variable(),
                parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal()));

        field = fieldAPI.save(field, user);

        //creates a new parent contentlet and publishes it
        final Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                .nextPersisted();
        ContentletDataGen.publish(parentContentlet);

        //creates children contentlets and publishes them
        final Contentlet childContentlet1 = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet childContentlet2 = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        Contentlet childContentlet3Live = new ContentletDataGen(childContentType.id())
                .languageId(defaultLanguage.getId())
                .nextPersisted();

        ContentletDataGen.publish(childContentlet1);
        ContentletDataGen.publish(childContentlet3Live);

        final Contentlet checkout = ContentletDataGen.checkout(childContentlet3Live);
        final Contentlet childContentlet3Working = ContentletDataGen.checkin(checkout);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + field.variable();

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship,
                CollectionsUtils.list(childContentlet1, childContentlet2, childContentlet3Working),
                user, false);

        //refresh relationships in the ES index
        contentletAPI.reindex(parentContentlet);
        contentletAPI.reindex(childContentlet1);
        contentletAPI.reindex(childContentlet2);
        contentletAPI.reindex(childContentlet3Working);

        final ContentTool contentTool = getContentTool(defaultLanguage.getId(), PageMode.LIVE);

        final String query = String.format("+contentType:%s", parentContentType.variable());
        final List<ContentMap> parentContent = contentTool.pull(query, 10, "modDate desc");

        assertNotNull(parentContent);
        assertEquals(1, parentContent.size());

        final Collection<ContentMap> relatedContent = (Collection) parentContent.get(0).get(field.variable());

        assertEquals(2, relatedContent.size());
        assertTrue(
                relatedContent.stream()
                        .map(contentlet -> contentlet.get("inode"))
                        .allMatch(inode -> inode.equals(childContentlet1.getInode())
                                || inode.equals(childContentlet3Live.getInode())
                        ));
    }

    /**
     * Method to Test: {@link ContentTool#pullRelated(String, String, String, boolean, int, String)} (String, int, String)}
     * When: pulling related content in different languages passing a condition which includes all langs (languageId:*)
     * Should: Return all contents in all languages
     *
     */

    @Test
    public void testPullRelated_MultiLangContent() throws DotDataException, DotSecurityException {

        final long time = System.currentTimeMillis();

        // creates second language
        final Language secondLang = new LanguageDataGen().nextPersisted();

        //creates parent content type
        ContentType parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

        //creates child content type
        ContentType childContentType = createAndSaveSimpleContentType("childContentType" + time);

        Field field = createField(childContentType.variable(), parentContentType.id(),
                childContentType.variable(),
                String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()));

        //One side of the relationship is set parentContentType --> childContentType
        field = fieldAPI.save(field, user);

        //creates a new parent contentlet and publishes it
        ContentletDataGen contentletDataGen = new ContentletDataGen(parentContentType.id());

        final Contentlet parentContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();
        ContentletDataGen.publish(parentContentlet);

        //creates 2 children contentlets in defaultLang and 1 in secondLang
        contentletDataGen = new ContentletDataGen(childContentType.id());
        final Contentlet childContentlet1 = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet childContentlet2 = contentletDataGen.languageId(defaultLanguage.getId())
                .nextPersisted();

        final Contentlet childContentlet3 = contentletDataGen.languageId(secondLang.getId())
                .nextPersisted();

        ContentletDataGen.publish(childContentlet1);
        ContentletDataGen.publish(childContentlet2);
        ContentletDataGen.publish(childContentlet3);

        final List<Contentlet> children = CollectionsUtils.list(childContentlet1, childContentlet2,
                childContentlet3);

        final String fullFieldVar =
                parentContentType.variable() + StringPool.PERIOD + field.variable();

        final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

        //relates parent contentlet with the child contentlet
        contentletAPI.relateContent(parentContentlet, relationship, children, user, false);

        //refresh relationships in the ES index
        contentletAPI.refresh(parentContentlet);
        contentletAPI.refresh(childContentlet1);
        contentletAPI.refresh(childContentlet2);
        contentletAPI.refresh(childContentlet3);

        final ContentTool contentTool = getContentTool(defaultLanguage.getId());

        final List<ContentMap> result = contentTool
                .pullRelated(relationship.getRelationTypeValue(),
                        parentContentlet.getIdentifier(), "+languageId:*", false, -1, null);

        List<Contentlet> pullRelatedContent = result.stream().map((ContentMap::getContentObject)).collect(
                Collectors.toList());

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue("Unexpected related content pulled",
                children.containsAll(pullRelatedContent));


    }

    /**
     * Method to Test: {@link ContentTool#pull(String, int, int, String)}}
     * When: pulling content and having more than one content
     * Should: Return the total under the key "totalResults"
     *
     */
    @Test
    public void testPull_includeTotal() {
        final ContentType blogLikeType = TestDataUtils.getBlogLikeContentType();

        final ContentletDataGen contentletDataGen = new ContentletDataGen(blogLikeType.inode()).host(defaultHost);
        IntStream.range(0, 10).forEach(i -> contentletDataGen.nextPersisted());

        final ContentTool contentTool = getContentTool(defaultLanguage.getId());

        final PaginatedArrayList<ContentMap> results = contentTool.pull(
                "+contentType:" + blogLikeType.variable(), 0, 0,
                "modDate desc"
        );

        Assert.assertEquals(10, results.getTotalResults());
    }

    /**
     * Method to Test: {@link ContentTool#findHydrated(String)}
     * When: Creates a Blog type and retrieves it as raw and hydrated
     * Should: The hydrated should contain more properties
     *
     */
    @Test
    public void test_find_hydrated() {
        final ContentType blogLikeType = TestDataUtils.getBlogLikeContentType();

        final ContentletDataGen contentletDataGen = new ContentletDataGen(blogLikeType.inode()).host(defaultHost);
        final Contentlet contentlet = contentletDataGen.nextPersisted();
        final ContentTool contentTool = getContentTool(defaultLanguage.getId());
        final ContentMap rawContentlet = contentTool.find(contentlet.getIdentifier());
        final ContentMap hydratedContentlet = contentTool.findHydrated(contentlet.getIdentifier());

        Assert.assertNotNull(rawContentlet);
        Assert.assertNotNull(hydratedContentlet);
        Assert.assertEquals(rawContentlet.getContentObject().getIdentifier(), hydratedContentlet.getContentObject().getIdentifier());
        Assert.assertEquals(rawContentlet.getContentObject().getLanguageId(), hydratedContentlet.getContentObject().getLanguageId());
        Assert.assertEquals(rawContentlet.getContentObject().getTitle(), hydratedContentlet.getContentObject().getTitle());
        Assert.assertTrue(hydratedContentlet.getContentObject().getMap().containsKey("url"));
        Assert.assertTrue(hydratedContentlet.getContentObject().getMap().size() > rawContentlet.getContentObject().getMap().size());
    }
}
