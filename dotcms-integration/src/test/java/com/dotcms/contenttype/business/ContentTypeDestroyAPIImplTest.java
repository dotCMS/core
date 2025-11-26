package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.JUnit4WeldRunner;
import com.dotcms.business.CloseDBIfOpened;
import com.dotcms.content.elasticsearch.business.ESSearchResults;
import com.dotcms.contenttype.business.ContentTypeDestroyAPIImpl.ContentletVersionInfo;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FieldRelationshipDataGen;
import com.dotcms.datagen.PersonaDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TagDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.personas.model.Persona;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.util.UUIDGenerator;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Here we test the {@link ContentTypeDestroyAPIImpl}
 * @author Fabrizzio
 */
@ApplicationScoped
@RunWith(JUnit4WeldRunner.class)
public class ContentTypeDestroyAPIImplTest extends IntegrationTestBase {

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }


    /**
     * Creates a simple content type with a number of contentlets*
     * @return
     */
    Tuple2<ContentType,Set<String>> simpleContentTypeWithData(int numContentlets)
            throws DotDataException {

        final Host host = new SiteDataGen().nextPersisted();
        final long currentTime = System.currentTimeMillis();
        final String name =  String.format("Async_CT_Remove_%s", currentTime);
        List<Field> fields = new ArrayList<>();
        fields.add(new FieldDataGen().name("Title").velocityVarName("title").next());
        final ContentType contentType = new ContentTypeDataGen().fields(fields).name(name).host(host).velocityVarName(name).nextPersisted();

        final Set<String> inodes = new HashSet<>();
        for (int i = 0; i < numContentlets; i++) {
            final Contentlet persisted = new ContentletDataGen(contentType.id()).setProperty("title", "test" + i).nextPersisted();
            ContentletDataGen.publish(persisted);
            inodes.add(persisted.getInode());
        }
        return Tuple.of(contentType, inodes);
    }

    /**
     * Given scenario: We have a content type with a number of contentlets
     * Expected Result: This test should return a map with the identifier as key and a list of contentlet version info from the given content type
     * @throws DotDataException
     */
    @Test
    public void testNextBatch() throws DotDataException {
        final ContentTypeDestroyAPIImpl impl = new ContentTypeDestroyAPIImpl();
        final Tuple2<ContentType,Set<String>> contentTypeAndInode = simpleContentTypeWithData(10);
        final ContentType contentType = contentTypeAndInode._1();
        final Set<String> inodes = contentTypeAndInode._2();
        int offset = 0;
        int limit = 5;
        final Map<String, List<ContentletVersionInfo>> batch = impl.nextBatch(contentType, inodes.size(), offset);

        batch.forEach((identifier, versions) -> {
            Assert.assertNotNull(identifier);
            versions.forEach(version -> {
                Assert.assertTrue(inodes.contains(version.getInode()));
                Assert.assertNotNull(version.getInode());
                Assert.assertNotNull(version.getLanguageId());
                Assert.assertNotNull(version.getHostInode());
                Assert.assertNotNull(version.getFolderInode());
            });
        });

    }


    /**
     * Given scenario: This is a general test to verify contents are removed successfully
     * Expected Result: The content type should be removed from the db and the index
     * @throws DotDataException
     */
    @Test
    public void Destroy_General_Test() throws DotDataException, DotSecurityException {
        final Tuple2<ContentType,Set<String>> contentTypeAndInode = simpleContentTypeWithData(10);
        final ContentType contentType = contentTypeAndInode._1();

        final Optional<String> anyInode = contentTypeAndInode._2().stream().findAny();

        Assert.assertFalse(searchIndex(contentType, true).isEmpty());
        Assert.assertFalse(searchIndex(contentType, false).isEmpty());

        Assert.assertTrue(anyInode.isPresent());

        Assert.assertFalse(searchIndex(anyInode.get(), true).isEmpty());
        Assert.assertFalse(searchIndex(anyInode.get(), false).isEmpty());

        final ContentTypeDestroyAPIImpl impl = new ContentTypeDestroyAPIImpl();
        final List<WorkflowScheme> workflowSchemes = APILocator.getWorkflowAPI().findSchemesForContentType(contentType);
        Assert.assertFalse(workflowSchemes.isEmpty());

        //Now Let's remove the content type!!!!
        impl.destroy(contentType, APILocator.systemUser());

        final ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());

        //This should give us a good sense of the ct not being present in db nor in cache
        final ContentType deletedContentType = Try.of(()->contentTypeAPI.find(contentType.id())).getOrNull();
        Assert.assertNull(deletedContentType);

        //Test no copy structure is left hang around
        final String likeName =  String.format("%s_disposed_*", getTestName());
        int count = new DotConnect().setSQL("select count(*) as x from structure where velocity_var_name like ? ").addParam(likeName).getInt("x");
        Assert.assertEquals(0, count);

        final Set<String> inodes = contentTypeAndInode._2();
        //Test no content is left hang around
        for (String inode:inodes) {
            count = new DotConnect().setSQL("select count(*) as x from contentlet where inode = ? ").addParam(inode).getInt("x");
            Assert.assertEquals(0, count);
        }
        //Make sure no workflow references are still tied to the content type
        final List<WorkflowScheme> workflowSchemesAfterDelete = APILocator.getWorkflowAPI().findSchemesForContentType(contentType);
        Assert.assertTrue(workflowSchemesAfterDelete.isEmpty());
        Assert.assertTrue(searchIndex(contentType, true).isEmpty());
        Assert.assertTrue(searchIndex(contentType, false).isEmpty());

        Assert.assertTrue(searchIndex(anyInode.get(), true).isEmpty());
        Assert.assertTrue(searchIndex(anyInode.get(), false).isEmpty());
    }

    private ESSearchResults searchIndex(final ContentType contentType, final boolean live )
            throws DotDataException, DotSecurityException {

        final String esQuery = String.format("{\n"
                + "        \"query\": {\n"
                + "            \"query_string\" : {\n"
                + "                \"query\" : \"+contenttype:%s\" \n"
                + "            }\n"
                + "        }\n"
                + "}", contentType.variable());

            return APILocator.getEsSearchAPI()
                    .esSearch(esQuery, live, APILocator.systemUser(), false);
    }


    private ESSearchResults searchIndex(final String inode, final boolean live )
            throws DotDataException, DotSecurityException {

        final String esQuery = String.format("{\n"
                + "        \"query\": {\n"
                + "            \"query_string\" : {\n"
                + "                \"query\" : \"+inode:%s\" \n"
                + "            }\n"
                + "        }\n"
                + "}", inode);

        return APILocator.getEsSearchAPI()
                .esSearch(esQuery, live, APILocator.systemUser(), false);
    }
    /**
     * Given Scenario: We create a Content Type with a relationship field
     * Expected Result: This test will validate that when a content type holding a relationship is deleted, the relationship is also removed from the database
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_Destroy_With_Categories() throws DotSecurityException, DotDataException {
        final Host host = new SiteDataGen().nextPersisted();

        final ContentType parent = new ContentTypeDataGen()
                .host(host)
                .fields(
                        List.of(new FieldDataGen().name("Title").velocityVarName("title").next())
                )
                .nextPersisted();

        final ContentType child = new ContentTypeDataGen()
                .host(host)
                .fields(
                        List.of(new FieldDataGen().name("Title").velocityVarName("title").next())
                )
                .nextPersisted();

        final Relationship relationship = new FieldRelationshipDataGen()
                .parent(parent)
                .child(child)
                .nextPersisted();

        Contentlet parentInstance = new ContentletDataGen(parent)
                .host(host)
                .setProperty("title", "parent")
                .nextPersisted();

        Contentlet childInstance = new ContentletDataGen(child)
                .host(host)
                .setProperty("title", "child")
                .nextPersisted();

        final Contentlet parentCheckout = ContentletDataGen.checkout(parentInstance);
        parentCheckout.setProperty(relationship.getChildRelationName(), List.of(childInstance));
        ContentletDataGen.checkin(parentCheckout);

        final ContentTypeDestroyAPIImpl impl = new ContentTypeDestroyAPIImpl();
        impl.destroy(parent, APILocator.systemUser());

        final int count = new DotConnect().setSQL("select count(*) as x from relationship where child_structure_inode = ?  and  parent_structure_inode=?  ")
                .addParam(child.inode())
                .addParam(parent.inode())
                .getInt("x");

        Assert.assertEquals(0, count);
    }

    /**
     * Given Scenario: Here we borrow the ContentType Persona to create another using all the defaults preset on the Original CT
     * Expected Result: We should be able to create a new CT using the Persona CT as a base and then delete it without any issues
     * @throws DotSecurityException
     * @throws DotDataException
     */
    @Test
    public void Test_Destroy_Persona_Like_Content_Type() throws DotSecurityException, DotDataException {
        final ContentType contentType = TestDataUtils.createPersonaLikeContentType("PersonaLikeContentType", null);
        new TagDataGen().name("lol").nextPersisted();

        PersonaDataGen personaDataGen = new PersonaDataGen();
        personaDataGen.description("Persona Description")
                .keyTag(UUIDGenerator.shorty())
                .name("Persona Name"+System.currentTimeMillis())
                .tags("lol");

        //Making sure the tags we are going to use exist

        Persona persona = personaDataGen.next();
        final Map<String, Object> map = persona.getMap();
        map.put("stInode", contentType.inode());
        map.put("contentType", contentType.versionable());

        persona.setIndexPolicy(IndexPolicy.FORCE);
        persona.setIndexPolicyDependencies(IndexPolicy.FORCE);
        persona.setBoolProperty(Contentlet.IS_TEST_MODE, true);

        Contentlet persisted = personaDataGen.persist(persona);
        ContentletDataGen.publish(persisted);
        Persona personaLike =  APILocator.getPersonaAPI().fromContentlet(persisted);
       //Our PersonaLikeContentType should be present in the index
        Assert.assertFalse(searchIndex(contentType, true).isEmpty());
        Assert.assertFalse(searchIndex(contentType, false).isEmpty());
        Assert.assertFalse(searchIndex(personaLike.getInode(), true).isEmpty());
        Assert.assertFalse(searchIndex(personaLike.getInode(), false).isEmpty());

        final int countBeforeDestroy = new DotConnect().setSQL("select count(*) from tag_inode ti where inode = ?").addParam(personaLike.getInode()).getInt("count");
        Assert.assertTrue("There should be at least one tag entry ",countBeforeDestroy > 0);

        final ContentTypeDestroyAPIImpl impl = new ContentTypeDestroyAPIImpl();
        impl.destroy(contentType,APILocator.systemUser());

        final int countAfterDestroy = new DotConnect().setSQL("select count(*) from tag_inode ti where inode = ?").addParam(personaLike.getInode()).getInt("count");
        Assert.assertEquals("There should be at least one tag entry ",0,countAfterDestroy);

    }

    /**
     * Method to Test: {@link ContentTypeDestroyAPIImpl#relocateContentletsForDeletion(ContentType, ContentType)}
     * Given Scenario: We create a Content Type with a random number or contentlets in it.
     * Expected Result: We call relocate and all the contentlets should be moved to the target content type.
     * @throws DotDataException
     */
    @Test
    public void Test_Relocate_Contentlets() throws DotDataException {
        final Random random = new Random();
        for (int i = 0; i < 10; i++) {
            TestContentRelocation(random.nextInt(19));
        }
    }

    /**
     * Method to Test: {@link ContentTypeDestroyAPIImpl#relocateContentletsForDeletion(ContentType, ContentType)}
     * @param numOfContentlets
     * @throws DotDataException
     */
    @CloseDBIfOpened
    void TestContentRelocation(int numOfContentlets) throws DotDataException{
        final Tuple2<ContentType,Set<String>> source = simpleContentTypeWithData(numOfContentlets);
        final Tuple2<ContentType, Set<String>> target = simpleContentTypeWithData(0);
        final ContentTypeDestroyAPIImpl impl = new ContentTypeDestroyAPIImpl();
        final int relocated = impl.relocateContentletsForDeletion(source._1(), target._1());
        Assert.assertEquals(numOfContentlets, relocated);
        int sourceCount = new DotConnect().setSQL("select count(*) as x from contentlet where structure_inode = ?")
                .addParam(source._1().inode())
                .getInt("x");

        int relocatedCount = new DotConnect().setSQL("select count(*) as x from contentlet where structure_inode = ?")
                .addParam(target._1().inode())
                .getInt("x");

        Assert.assertEquals(0, sourceCount);
        Assert.assertEquals(numOfContentlets, relocatedCount);
    }

}
