package com.dotmarketing.portlets.workflows.actionlet;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableMap;
import com.dotcms.system.event.local.model.EventSubscriber;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.workflows.actionlet.copy.*;
import com.dotmarketing.portlets.workflows.actionlet.event.CopyActionletEvent;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowProcessor;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CopyActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI workflowAPI = null;
    private static ContentletAPI contentletAPI = null;
    private static ContentTypeAPI contentTypeAPI = null;
    private static ContentType customContentType = null;
    private static Contentlet contentletCopy = null;
    private static final int LIMIT = 20;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        workflowAPI = APILocator.getWorkflowAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
        contentletAPI = APILocator.getContentletAPI();

        // creates the scheme and actions
        schemeStepActionResult = createSchemeStepActionActionlet
                ("CopyActionlet" + UUIDGenerator.generateUuid(), "step1", "action1",
                        CopyActionlet.class);

        // creates the type to trigger the scheme
        customContentType = createTestType(contentTypeAPI);

        // associated the scheme to the type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(customContentType).asStructure(),
                Collections.singletonList(schemeStepActionResult.getScheme()));

        setDebugMode(false);
    }

    private static ContentType createTestType(final ContentTypeAPI contentTypeAPI)
            throws DotDataException, DotSecurityException {

        final ContentType type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                        .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                        .description("Dot..")
                        .name("DotCopyActionletTest" + System.currentTimeMillis())
                        .owner(APILocator.systemUser().toString())
                        .variable("DotCopyActionletTest" + System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        return contentTypeAPI.save(type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup()
            throws DotDataException, DotSecurityException, AlreadyExistException {

        if (null != customContentType) {

            final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
            contentTypeAPI.delete(customContentType);
        }

        if (null != schemeStepActionResult) {

            cleanScheme(schemeStepActionResult.getScheme());
        }
        cleanupDebug(CopyActionletTest.class);
    } // cleanup

    public void saveCustomTestContentType() throws DotDataException, DotSecurityException {

        final long languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User user = APILocator.systemUser();
        contentlet.setContentTypeId(customContentType.id());
        contentlet.setOwner(APILocator.systemUser().toString());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt", "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);

        APILocator.getLocalSystemEventsAPI()
                .subscribe(CopyActionletEvent.class, new EventSubscriber<CopyActionletEvent>() {
                    @Override
                    public void notify(final CopyActionletEvent event) {
                        contentletCopy = event.getCopyContentlet();
                    }
                });

        // first save
        final Contentlet contentlet1 = contentletAPI.checkin(contentlet, user, false);
        final String firstIdentifier = contentlet1.getIdentifier();
        final String firstInode = contentlet1.getInode();

        // triggering the copy action
        contentlet1.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                schemeStepActionResult.getAction().getId());
        contentlet1.setBoolProperty(CopyActionlet.NOTIFY_SYNC_COPY_EVENT,
                true);

        contentlet1.setIndexPolicy(IndexPolicy.FORCE);
        final WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(contentlet1, user);

        workflowAPI.fireWorkflowPostCheckin(processor);

        assertNotNull(contentletCopy);
        assertNotNull(contentletCopy.getIdentifier());
        assertNotNull(contentletCopy.getInode());

        final Contentlet contentlet2 = contentletAPI.findContentletByIdentifier
                (contentletCopy.getIdentifier(),
                        false, languageId, user, false);

        // the contentlet saved by the action must not be null, and should have a new version.
        assertNotNull(contentlet2);
        assertNotNull(contentlet2.getIdentifier());
        assertNotNull(contentlet2.getInode());
        assertNotEquals(contentlet2.getIdentifier(), firstIdentifier);
        assertNotEquals(contentlet2.getInode(), firstInode);
        assertEquals("Test Save", contentlet2.getStringProperty("title"));
        assertEquals("Test Save Text", contentlet2.getStringProperty("txt"));

    }

    /**
     *
     * @param original
     * @return
     */
    private void submitForCopy(final Contentlet original, final User user) throws Exception {

        // triggering the copy action
        original.setStringProperty(Contentlet.WORKFLOW_ACTION_KEY,
                schemeStepActionResult.getAction().getId());
        original.setBoolProperty(CopyActionlet.NOTIFY_SYNC_COPY_EVENT,
                true);

        final WorkflowProcessor processor =
                workflowAPI.fireWorkflowPreCheckin(original, user);

        workflowAPI.fireWorkflowPostCheckin(processor);
    }


    @Test
    public void Test_Copy_Content_Expect_Success() throws Exception {
        final User systemUser = APILocator.systemUser();
        final Map<Contentlet, Contentlet> originalAndCopyMap = new HashMap<>();
        try {
            APILocator.getLocalSystemEventsAPI()
                    .subscribe(CopyActionletEvent.class,
                            (EventSubscriber<CopyActionletEvent>) event -> {
                                final Contentlet original = event.getOriginalContentlet();
                                final Contentlet copy = event.getCopyContentlet();
                                originalAndCopyMap.put(original, copy);
                            });

            final Map<ContentType, List<Contentlet>> contentSamplesByType = findContentSamples();

            final Set<ContentType> contentTypes = contentSamplesByType.keySet();
            for(final ContentType contentType:contentTypes){
                Logger.info(this,"ContentType:" + contentType.name());
                final List<Contentlet> contentSamples = contentSamplesByType.get(contentType);
                if (UtilMethods.isSet(contentSamples)) {
                    Logger.info(this,"Number of samples found : " + contentSamples.size());
                    for (final Contentlet original : contentSamples) {
                        submitForCopy(original, systemUser);
                    }
                } else {
                    Logger.info(this,"No samples found skipping validation.");
                }
            }
            originalAndCopyMap.forEach(this::validateCopyVsOriginal);

        }finally {
             for(final Contentlet copy : originalAndCopyMap.values()){
                try{
                  contentletAPI.destroy(copy, systemUser, false);
                }catch(Exception e){
                    Logger.error(this,"Error performing clean-up.", e);
                }
             }
        }

    }

    private void validateCopyVsOriginal(final Contentlet original, final Contentlet copy) {

        // printDebugInfo(original, copy);

        Logger.info(this, "Content type " + original.getContentType().name());
        Logger.info(this, "Original content: " +original);

        assertEquals(original.getContentType(), copy.getContentType());

        final AbstractContentletValidationStrategy validatorStrategy = contentletCopyValidationStrategyMap.get(original.getContentType().baseType());
        validatorStrategy.apply(original,copy);

    }

    private final Map<BaseContentType, AbstractContentletValidationStrategy> contentletCopyValidationStrategyMap = ImmutableMap.<BaseContentType, AbstractContentletValidationStrategy>builder()
            .put(BaseContentType.CONTENT, new ContentValidationStrategy())
            .put(BaseContentType.FILEASSET, new FileAssetValidationStrategy())
            .put(BaseContentType.FORM, new FormValidationStrategy())
            .put(BaseContentType.HTMLPAGE, new HtmlPageValidationStrategy())
            .put(BaseContentType.PERSONA, new PersonaValidationStrategy())
            .put(BaseContentType.VANITY_URL, new VanityUrlValidationStrategy())
            .put(BaseContentType.WIDGET, new WidgetValidationStrategy())
            .put(BaseContentType.KEY_VALUE, new WidgetValidationStrategy())
            .build();

    /**
     * This will get you a Map with a number of samples organized by Content type
     * @return
     * @throws Exception
     */
    private Map<ContentType, List<Contentlet>> findContentSamples() throws Exception {

        final List<ContentType> contentTypes = contentTypeAPI.findAll().stream()
                // Exclude the custom CT created by this Test since it gets removed later we want to avoid any odd behavior.
                .filter(contentType -> contentType.equals(customContentType))
                //In case you want to track any specific CT
                //.filter(contentType -> contentType.baseType() == BaseContentType.PERSONA)
                // Or even a sub type
                //.filter(contentType -> "Page Asset".equals(contentType.name()))
                .collect(Collectors.toList());

        final Map<ContentType, List<Contentlet>> contentTypeSamplesMap = new HashMap<>();

        for (final ContentType contentType : contentTypes) {

            final List<Contentlet> contentlets = contentletAPI
                    .search("+contentType:" + contentType.variable() +
                             " +languageId:1 +deleted:false ", LIMIT, 0,
                            "modDate desc",
                            APILocator.systemUser(), false);

            contentTypeSamplesMap.computeIfAbsent(contentType, ct -> new ArrayList<>(LIMIT))
                    .addAll(contentlets);

        }
        return contentTypeSamplesMap;
    }

    /*
    // Leaving this code here this is really handy when tracking contentlet issues
    private void printDebugInfo(final Contentlet original, final Contentlet copy){
        System.out.println("CT="+original.getContentType().name() + " BaseType= "+original.getContentType().baseType() + "\n");
        final Map<String, Object> p = original.getMap();
        System.out.println("---------------------- ORIGINAL----------------------");
        p.forEach((s, o) -> {
            System.out.println("" + s + " = " + o + " "+o.getClass() );
        });

        Map<String, Object> q = copy.getMap();
        System.out.println("---------------------- COPY ----------------------");
        q.forEach((s, o) -> {
            System.out.println("" + s + " = " + o + " "+o.getClass() );
        });
    } */
}