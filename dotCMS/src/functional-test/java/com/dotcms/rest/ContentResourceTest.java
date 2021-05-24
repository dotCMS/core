package com.dotcms.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONArray;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONException;
import com.dotcms.repackage.org.codehaus.jettison.json.JSONObject;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.DataType;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowActionClass;
import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import com.dotmarketing.portlets.workflows.model.WorkflowSearcher;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.tag.model.Tag;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ContentResourceTest {
    Client client;
    WebTarget webTarget;
    String authheader="Authorization";
    String authvalue="Basic "+new String(Base64.encode("admin@dotcms.com:admin".getBytes()));

    private static FieldAPI fieldAPI;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static HostAPI hostAPI;
    private static User user;

    private static final String REST_API_CONTENT_ALLOW_FRONT_END_SAVING = "REST_API_CONTENT_ALLOW_FRONT_END_SAVING";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void before() throws Exception{
        LicenseTestUtil.getLicense();

        client=RestClientBuilder.newClient();
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        long serverPort = request.getServerPort();
        webTarget = client.target("http://" + serverName + ":" + serverPort + "/api/content");

        //Setting the test user
        user = APILocator.getUserAPI().getSystemUser();
        fieldAPI = APILocator.getContentTypeFieldAPI();
        hostAPI = APILocator.getHostAPI();
        contentletAPI = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user, false);
    }

    @Test
    public void getContent() throws Exception {
        Structure structure = CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
        assertNotNull(structure);

        //Get Contentlets by ContentType
        String response = webTarget.path("/query/+contentType:webPageContent/orderby/modDate%20desc/limit/5")
                .request()
                .header(authheader, authvalue)
                .get(String.class);

        assertNotNull(response);
        System.out.println("Response 1: " + response);
        JSONObject json = new JSONObject(response);
        assertNotNull(json);

        JSONArray contentlets = (JSONArray) json.get("contentlets");
        assertNotNull(contentlets);

        int length = contentlets.length();
        assertTrue(length > 0);


        //Get Contentlets by Structure Name
        response = webTarget.path("/query/+stName:webPageContent/orderby/modDate%20desc/limit/5")
                .request()
                .header(authheader, authvalue)
                .get(String.class);

        assertNotNull(response);
        System.out.println("Response 2: " + response);
        json = new JSONObject(response);
        assertNotNull(json);

        contentlets = (JSONArray) json.get("contentlets");
        assertNotNull(contentlets);

        assertEquals(length, contentlets.length());

        //Get Contentlets by Structure inode
        response = webTarget.path("/query/+stInode:"+ structure.getInode() +"/orderby/modDate%20desc/limit/5")
                .request()
                .header(authheader, authvalue)
                .get(String.class);

        assertNotNull(response);
        System.out.println("Response 3: " + response);
        json = new JSONObject(response);
        assertNotNull(json);

        contentlets = (JSONArray) json.get("contentlets");
        assertNotNull(contentlets);

        assertEquals(length, contentlets.length());

    }

    @Test
    public void getContent_HeaderMimeTypeIsApplicationJson() throws Exception{
        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
        Host demo=hostAPI.findByName("demo.dotcms.com", user, false);

        String demoId=demo.getIdentifier();

        Response responsePut = webTarget.path("/publish/1/").request()
                .header(authheader, authvalue)
                .put(Entity.entity(new JSONObject()
                        .put("stInode", st.getInode())
                        .put("languageId", 1)
                        .put("title", "嗨")
                        .put("body", "this is an example text")
                        .put("contentHost", demoId).toString(), MediaType.APPLICATION_JSON_TYPE));


        Assert.assertEquals(200, responsePut.getStatus());
        String location=responsePut.getLocation().toString();
        String inode=location.substring(location.lastIndexOf("/")+1);
        Contentlet cont=contentletAPI.find(inode, user, false);
        Assert.assertNotNull(cont);

        Response responseGet = webTarget.path("/id/"+cont.getIdentifier())
                .request()
                .header(authheader, authvalue)
                .get();

        assertNotNull(responseGet);
        assertEquals("application/json", responseGet.getHeaderString("Content-Type"));

        String responseGetString = webTarget.path("/id/"+cont.getIdentifier())
                .request()
                .header(authheader, authvalue)
                .get(String.class);

        assertNotNull(responseGetString);
        JSONObject json = new JSONObject(responseGetString);
        assertNotNull(json);

        JSONArray contentlets = (JSONArray) json.get("contentlets");
        assertNotNull(contentlets);
        assertEquals("嗨", contentlets.getJSONObject(0).get("title"));

        //Cleaning up the created contentlet
        contentletAPI.destroy(cont, user, false);
    }
    @Test
    @Ignore
    public void singlePUT() throws Exception {
        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("webPageContent");
        Host demo=hostAPI.findByName("demo.dotcms.com", user, false);

        String demoId=demo.getIdentifier();

        Response response = webTarget.path("/publish/1/").request()
            .header(authheader, authvalue)
            .put(Entity.entity(new JSONObject()
                .put("stInode", st.getInode())
                .put("languageId", 1)
                .put("title", "Test content from ContentResourceTest")
                .put("body", "this is an example text")
                .put("contentHost", demoId).toString(), MediaType.APPLICATION_JSON_TYPE));


        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        String location=response.getLocation().toString();
        String inode=location.substring(location.lastIndexOf("/")+1);
        Contentlet cont=contentletAPI.find(inode, user, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals("Test content from ContentResourceTest",cont.getStringProperty("title"));
        Assert.assertEquals("this is an example text",cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());

        // testing other host_or_folder formats: folderId
        Folder folder=APILocator.getFolderAPI().findFolderByPath("/home", demo, user, false);
        response=webTarget.path("/publish/1").request()
                       .header(authheader, authvalue).put(Entity.entity(
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "test content with folderid")
                                .put("body", "this is an example text")
                                .put("contentHost", folder.getInode()).toString(), MediaType.APPLICATION_JSON_TYPE));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=contentletAPI.find(inode, user, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertTrue(cont.isLive());

        // testing other host_or_folder formats: hostname
        response=webTarget.path("/publish/1").request()
                .header(authheader, authvalue).put(Entity.entity(
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest folderId")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com").toString(), MediaType.APPLICATION_JSON_TYPE));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=contentletAPI.find(inode, user, false);
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertTrue(cont.isLive());

        // testing other host_or_folder formats: hostname:path
        response=webTarget.path("/justsave/1").request()
                .header(authheader, authvalue).put(Entity.entity(
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest folderId")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com:/home").toString(), MediaType.APPLICATION_JSON_TYPE));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=contentletAPI.find(inode, user, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertFalse(cont.isLive());


        // testing XML
        response=webTarget.path("/publish/1").request()
                       .header(authheader, authvalue).put(Entity.entity(
                            "<content>" +
                            "<stInode>" +st.getInode() + "</stInode>"+
                            "<languageId>1</languageId>"+
                            "<title>Test content from ContentResourceTest XML</title>"+
                            "<body>this is an example text XML</body>"+
                            "<contentHost>"+demoId+"</contentHost>"+
                            "</content>", MediaType.APPLICATION_XML_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=contentletAPI.find(inode, user, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals("Test content from ContentResourceTest XML",cont.getStringProperty("title"));
        Assert.assertEquals("this is an example text XML",cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());

        // testing form-urlencoded
        String title="Test content from ContentResourceTest FORM "+UUIDGenerator.generateUuid();
        String body="this is an example text FORM "+UUIDGenerator.generateUuid();
        response=webTarget.path("/publish/1").request()
                .header(authheader, authvalue).put(Entity.entity(
                     "stInode=" +st.getInode() + "&"+
                     "languageId=1&"+
                     "title="+URLEncoder.encode(title, "UTF-8")+"&"+
                     "body="+URLEncoder.encode(body, "UTF-8")+"&"+
                     "contentHost="+demoId, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        Assert.assertEquals(inode, response.getHeaders().getFirst("inode")); // validate consistency of inode header
        cont=contentletAPI.find(inode, user, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(cont.getIdentifier(), response.getHeaders().getFirst("identifier")); // consistency of identifier header
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals(title,cont.getStringProperty("title"));
        Assert.assertEquals(body,cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());


    }

    @Test
    public void multipartPUT() throws Exception {
        final String salt=Long.toString(System.currentTimeMillis());
        final Client client = ClientBuilder.newClient().register(MultiPartFeature.class);

        Response response = client.target(webTarget.getUri() + "/publish/1").request()
                                   .header(authheader, authvalue).put(Entity.entity(
                        new MultiPart()
                                .bodyPart(new BodyPart(
                                    new JSONObject()
                                        .put("hostFolder", "demo.dotcms.com:/resources")
                                        .put("title", "newfile" + salt + ".txt")
                                        .put("fileName", "newfile" + salt + ".txt")
                                        .put("languageId", "1")
                                        .put("stInode", CacheLocator.getContentTypeCache().getStructureByVelocityVarName("FileAsset").getInode())
                                        .toString(), MediaType.APPLICATION_JSON_TYPE))
                                .bodyPart(new StreamDataBodyPart(
                                    "newfile" + salt + ".txt",
                                    new ByteArrayInputStream(("this is the salt " + salt).getBytes()),
                                    "newfile" + salt + ".txt",
                                    MediaType.APPLICATION_OCTET_STREAM_TYPE)), MediaType.MULTIPART_FORM_DATA_TYPE));

        Assert.assertEquals(200, response.getStatus());
        Contentlet cont=contentletAPI.find((String) response.getHeaders().getFirst("inode"),user,false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertTrue(response.getLocation().toString().endsWith("/api/content/inode/"+cont.getInode()));
        FileAsset file=APILocator.getFileAssetAPI().fromContentlet(cont);
        Assert.assertEquals("/resources/newfile"+salt+".txt",file.getURI());
        Assert.assertEquals("demo.dotcms.com", hostAPI.find(file.getHost(), user, false).getHostname());
        Assert.assertEquals("this is the salt "+salt, IOUtils.toString(file.getInputStream()));
    }

    /**
     * https://github.com/dotCMS/core/issues/11950
     */
    @Test
    public void testContentWithTwoBinaryFieldsAndSameFile_afterCheckinShouldContainBothFields() throws Exception {
        final Client client = ClientBuilder.newClient().register(MultiPartFeature.class);

        ContentType contentType = null;
        com.dotcms.contenttype.model.field.Field textField = null;
        com.dotcms.contenttype.model.field.Field binaryField1 = null;
        com.dotcms.contenttype.model.field.Field binaryField2 = null;

        Contentlet contentlet = null;

        Host demo = hostAPI.findByName("demo.dotcms.com", user, false);

        try {
            //Create Content Type.
            contentType = ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .description("Test ContentType Two Fields")
                .host(demo.getIdentifier())
                .name("Test ContentType Two Fields")
                .owner("owner")
                .variable("testContentTypeWithTwoBinaryFields")
                .build();

            contentType = contentTypeAPI.save(contentType);

            //Save Fields. 1. Text, 2. Binary, 3. Binary.
            //Creating Text Field.
            textField = ImmutableTextField.builder()
                .name("Title")
                .variable("title")
                .contentTypeId(contentType.id())
                .dataType(DataTypes.TEXT)
                .build();

            textField = fieldAPI.save(textField, user);

            //Creating First Binary Field.
            binaryField1 = ImmutableBinaryField.builder()
                .name("Binary 1")
                .variable("binary1")
                .contentTypeId(contentType.id())
                .build();

            binaryField1 = fieldAPI.save(binaryField1, user);

            //Creating Second Binary Field.
            binaryField2 = ImmutableBinaryField.builder()
                .name("Binary 2")
                .variable("binary2")
                .contentTypeId(contentType.id())
                .build();

            binaryField2 = fieldAPI.save(binaryField2, user);

            //Creating a temporary File to use in the binary fields.
            File imageFile = temporaryFolder.newFile("DummyFile.txt");
            writeTextIntoFile(imageFile, "This is the same file");

            Response response = client.target(webTarget.getUri() + "/publish/1").request()
                .header(authheader, authvalue).put(Entity.entity(
                    new MultiPart()
                        .bodyPart(new BodyPart(
                            new JSONObject()
                                .put("hostFolder", "demo.dotcms.com:/resources")
                                .put(textField.variable(), "Test Content with Same File")
                                .put(binaryField1.variable(), imageFile.getName())
                                .put(binaryField2.variable(), imageFile.getName())
                                .put("languageId", "1")
                                .put("stInode", contentType.inode())
                                .toString(), MediaType.APPLICATION_JSON_TYPE))
                        .bodyPart(new StreamDataBodyPart(
                            imageFile.getName(),
                            FileUtils.openInputStream(imageFile),
                            imageFile.getName(),
                            MediaType.APPLICATION_OCTET_STREAM_TYPE))
                        .bodyPart(new StreamDataBodyPart(
                            imageFile.getName(),
                            FileUtils.openInputStream(imageFile),
                            imageFile.getName(),
                            MediaType.APPLICATION_OCTET_STREAM_TYPE)), MediaType.MULTIPART_FORM_DATA_TYPE));

            Assert.assertEquals(200, response.getStatus());
            contentlet = contentletAPI.find((String) response.getHeaders().getFirst("inode"), user, false);
            Assert.assertNotNull(contentlet);
            Assert.assertTrue(InodeUtils.isSet(contentlet.getIdentifier()));
            Assert
                .assertTrue(response.getLocation().toString().endsWith("/api/content/inode/" + contentlet.getInode()));

            //Check that the properties still exist.
            assertTrue(contentlet.getMap().containsKey(binaryField1.variable()));
            assertTrue(contentlet.getMap().containsKey(binaryField2.variable()));

            //Check that the properties have value.
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField1.variable())));
            assertTrue(UtilMethods.isSet(contentlet.getMap().get(binaryField2.variable())));

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            try {
                //Delete Contentlet.
                if (contentlet != null) {
                    contentletAPI.archive(contentlet, user, false);
                    contentletAPI.delete(contentlet, user, false);
                }
                //Deleting Fields.
                if (textField != null) {
                    fieldAPI.delete(textField);
                }
                if (binaryField1 != null) {
                    fieldAPI.delete(binaryField1);
                }
                if (binaryField2 != null) {
                    fieldAPI.delete(binaryField2);
                }
                //Deleting Content Type
                if (contentType != null) {
                    contentTypeAPI.delete(contentType);
                }
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void categoryAndTagFields() throws Exception {
        Structure st=CacheLocator.getContentTypeCache().getStructureByVelocityVarName("Blog");
        String salt=Long.toString(System.currentTimeMillis());
        Response response=webTarget.path("/justsave/1").request()
                .header(authheader, authvalue).put(Entity.entity(
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("host1", "demo.dotcms.com")
                                .put("title", "blog post " + salt)
                                .put("urlTitle", "blog-post-" + salt)
                                .put("author", "junit")
                                .put("sysPublishDate", new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")
                                    .format(Calendar.getInstance().getTime()))
                                .put("body", "blog post content " + salt)
                                .put("topic", "investing,banking")
                                .put("tags", "junit,integration tests,jenkins")
                                .put("contentHost", "demo.dotcms.com:/home").toString(), MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(200, response.getStatus());
        String inode=(String)response.getHeaders().getFirst("inode");
        Contentlet cont=contentletAPI.find(inode, user, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));

        /////////////////////////
        // checking categories //
        /////////////////////////

        List<Category> cats=APILocator.getCategoryAPI().getParents(cont, user, false);
        Assert.assertNotNull(cats);
        Assert.assertEquals(2,cats.size());

        Set<String> expectedIds=new HashSet<String>();
        expectedIds.add("investing"); expectedIds.add("banking");
        expectedIds.remove(cats.get(0).getCategoryVelocityVarName());
        expectedIds.remove(cats.get(1).getCategoryVelocityVarName());
        Assert.assertEquals(0, expectedIds.size());

        ///////////////////
        // checking tags //
        ///////////////////

        List<Tag> tags=APILocator.getTagAPI().getTagsByInode(cont.getInode());
        Assert.assertNotNull(tags);
        Assert.assertEquals(3, tags.size());
        Set<String> expectedTags=new HashSet<String>(Arrays.asList("junit,integration tests,jenkins".split(",")));
        for(Tag tt : tags) {
            Assert.assertTrue(expectedTags.remove(tt.getTagName()));
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void workflowTask() throws  Exception {
        final String salt=Long.toString(System.currentTimeMillis());

        // a mandatory scheme to test
        WorkflowScheme scheme = new WorkflowScheme();
        scheme.setName("Rest Mandatory Workflow "+salt);
        scheme.setDescription("testing rest save content");
        scheme.setCreationDate(new Date());
        APILocator.getWorkflowAPI().saveScheme(scheme, user);

        WorkflowStep step1=new WorkflowStep();
        step1.setCreationDate(new Date());
        step1.setEnableEscalation(false);
        step1.setMyOrder(1);
        step1.setName("Step 1");
        step1.setResolved(false);
        step1.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step1, user);

        WorkflowStep step2=new WorkflowStep();
        step2.setCreationDate(new Date());
        step2.setEnableEscalation(false);
        step2.setMyOrder(2);
        step2.setName("Step 2");
        step2.setResolved(false);
        step2.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step2, user);

        WorkflowStep step3=new WorkflowStep();
        step3.setCreationDate(new Date());
        step3.setEnableEscalation(false);
        step3.setMyOrder(3);
        step3.setName("Step 3");
        step3.setResolved(true);
        step3.setSchemeId(scheme.getId());
        APILocator.getWorkflowAPI().saveStep(step3, user);

        // Save as Draft Step1 -> Step1
        WorkflowAction saveDraft=new WorkflowAction();
        saveDraft.setId(UUIDGenerator.generateUuid());
        saveDraft.setSchemeId(scheme.getId());
        saveDraft.setName("Save as Draft");
        saveDraft.setOrder(1);
        saveDraft.setNextStep(step1.getId());
        saveDraft.setRequiresCheckout(false);
        saveDraft.setStepId(step1.getId());
        saveDraft.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        saveDraft.setShowOn(WorkflowState.LOCKED,WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        APILocator.getWorkflowAPI().saveAction(saveDraft,
                Arrays.asList(new Permission[] {
                        new Permission(
                                saveDraft.getPermissionType(),
                                saveDraft.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }),
                user);
        APILocator.getWorkflowAPI().saveAction(saveDraft.getId(), step1.getId(),user);

        // Save as Draft Step1 -> Step1
        WorkflowAction escalate=new WorkflowAction();
        escalate.setId(UUIDGenerator.generateUuid());
        escalate.setSchemeId(scheme.getId());
        escalate.setName("Save and Assign");
        escalate.setOrder(2);
        escalate.setNextStep(step1.getId());
        escalate.setRequiresCheckout(false);
        escalate.setStepId(step1.getId());
        escalate.setAssignable(true);
        escalate.setCommentable(true);
        escalate.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        escalate.setShowOn(WorkflowState.LOCKED,WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        APILocator.getWorkflowAPI().saveAction(escalate,
                Arrays.asList(new Permission[] {
                        new Permission(
                                escalate.getPermissionType(),
                                escalate.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }),
                user);
        APILocator.getWorkflowAPI().saveAction(escalate.getId(), step1.getId(),user);
        APILocator.getWorkflowAPI().saveScheme(scheme, user);

        // Send for review Step1 -> Step2
        WorkflowAction sendReview=new WorkflowAction();
        sendReview.setId(UUIDGenerator.generateUuid());
        sendReview.setSchemeId(scheme.getId());
        sendReview.setName("Send for review");
        sendReview.setOrder(3);
        sendReview.setNextStep(step2.getId());
        sendReview.setRequiresCheckout(false);
        sendReview.setStepId(step1.getId());
        sendReview.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        sendReview.setShowOn(WorkflowState.LOCKED,WorkflowState.UNLOCKED,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        APILocator.getWorkflowAPI().saveAction(sendReview,
                Arrays.asList(new Permission[] {
                        new Permission(
                                sendReview.getPermissionType(),
                                sendReview.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }),
                user);
        APILocator.getWorkflowAPI().saveAction(sendReview.getId(), step1.getId(),user);

        // reject Step2 -> Step1
        WorkflowAction reject=new WorkflowAction();
        reject.setId(UUIDGenerator.generateUuid());
        reject.setSchemeId(scheme.getId());
        reject.setName("Reject");
        reject.setOrder(1);
        reject.setNextStep(step1.getId());
        reject.setRequiresCheckout(false);
        reject.setStepId(step2.getId());
        reject.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        reject.setShowOn(WorkflowState.LOCKED,WorkflowState.UNLOCKED,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        APILocator.getWorkflowAPI().saveAction(reject,
                Arrays.asList(new Permission[] {
                        new Permission(
                                reject.getPermissionType(),
                                reject.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }),
                user);
        APILocator.getWorkflowAPI().saveAction(sendReview.getId(), step2.getId(),user);

        // publish Step2 -> Step3
        WorkflowAction publish=new WorkflowAction();
        publish.setId(UUIDGenerator.generateUuid());
        publish.setSchemeId(scheme.getId());
        publish.setName("Publish");
        publish.setOrder(2);
        publish.setNextStep(step3.getId());
        publish.setRequiresCheckout(false);
        publish.setStepId(step2.getId());
        publish.setNextAssign(APILocator.getRoleAPI().loadCMSAnonymousRole().getId());
        publish.setShowOn(WorkflowState.LOCKED,WorkflowState.UNLOCKED,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        APILocator.getWorkflowAPI().saveAction(publish,
                Arrays.asList(new Permission[] {
                        new Permission(
                                publish.getPermissionType(),
                                publish.getId(),
                                APILocator.getRoleAPI().loadCMSAnonymousRole().getId(),
                                PermissionAPI.PERMISSION_USE) }),
                user);
        APILocator.getWorkflowAPI().saveAction(publish.getId(), step2.getId(),user);

        WorkflowActionClass publishlet=new WorkflowActionClass();
        publishlet.setActionId(publish.getId());
        publishlet.setClazz(com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet.class.getCanonicalName());
        publishlet.setName("publish");
        publishlet.setOrder(1);
        APILocator.getWorkflowAPI().saveActionClass(publishlet, user);

        // a test structure with that scheme
        Structure st=new Structure();
        st.setName("Rest test st "+salt);
        st.setVelocityVarName("restTestSt"+salt);
        st.setDescription("testing rest content creation with mandatory workflow");
        StructureFactory.saveStructure(st);
        Field field=new Field("Title",FieldType.TEXT,DataType.TEXT,st,true,true,true,1,false,false,true);
        FieldFactory.saveField(field);
        List<WorkflowScheme> schemes = new ArrayList<>();
        schemes.add(scheme);
        APILocator.getWorkflowAPI().saveSchemesForStruct(st, schemes);

        // send the Rest api call
        User bill=APILocator.getUserAPI().loadUserById("dotcms.org.2806");
        Role billrole=APILocator.getRoleAPI().getUserRole(bill);
        Response response =webTarget.path("/Save%20and%20Assign/1/wfActionComments/please%20do%20this%20for%20me/wfActionAssign/"+billrole.getId())
            .request()
            .header(authheader, authvalue).put(Entity.entity(
                new JSONObject()
                    .put("stInode", st.getInode())
                    .put("languageId", 1)
                    .put(field.getVelocityVarName(), "test title "+salt)
                    .toString(), MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(200, response.getStatus());

        Contentlet cont = contentletAPI.find((String)response.getHeaders().getFirst("inode"), user, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));

        // must be in the first step
        WorkflowStep contentStep = APILocator.getWorkflowAPI().findStepByContentlet(cont);
        assertNotNull(contentStep);
        Assert.assertEquals(step1.getId(), contentStep.getId());

        boolean assigned=false;

        HashMap<String, Object> map = new HashMap<String,Object>();
        map.put("assignedTo",billrole.getId());
        for(WorkflowTask task : APILocator.getWorkflowAPI().searchTasks(new WorkflowSearcher(map, user))) {
            if(task.getWebasset().equals(cont.getIdentifier())) {
                assigned=true;
                Assert.assertEquals("please do this for me",task.getDescription());
                break;
            }
        }
        Assert.assertTrue(assigned);

    }

    @Test
    public void uriFileImageFields() throws Exception {
        final String salt=Long.toString(System.currentTimeMillis());

        Structure st=new Structure();
        st.setName("Rest Test File Img "+salt);
        st.setVelocityVarName("restTestSt"+salt);
        st.setDescription("testing rest content creation with file&image fields");
        StructureFactory.saveStructure(st);
        Field title=new Field("Title",FieldType.TEXT,DataType.TEXT,st,true,true,true,1,false,false,true);
        FieldFactory.saveField(title);
        Field file=new Field("aFile",FieldType.FILE,DataType.TEXT,st,true,false,true,2,false,false,true);
        FieldFactory.saveField(file);
        Field image=new Field("aImage",FieldType.IMAGE,DataType.TEXT,st,true,false,true,3,false,false,true);
        FieldFactory.saveField(image);

        Host demo=hostAPI.findByName("demo.dotcms.com", user, false);
        Folder ff=APILocator.getFolderAPI().createFolders("/rest/"+salt, demo, user, false);

        java.io.File filefile = java.io.File.createTempFile("filefile", ".txt");
        FileUtil.write(filefile, "helloworld");
        java.io.File imgimg = java.io.File.createTempFile("imgimg", ".jpg");
        FileUtil.write(imgimg, "helloworld");

        Contentlet filea=new Contentlet();
        filea.setFolder(ff.getInode());
        filea.setHost(demo.getIdentifier());
        filea.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("fileAsset").getInode());
        filea.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, ff.getInode());
        filea.setStringProperty(FileAssetAPI.TITLE_FIELD, "filefile.txt");
        filea.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "filefile.txt");
        filea.setBinary(FileAssetAPI.BINARY_FIELD, filefile);
        filea.setLanguageId(1);
        filea = contentletAPI.checkin(filea, user, false);

        Contentlet imga=new Contentlet();
        imga.setFolder(ff.getInode());
        imga.setHost(demo.getIdentifier());
        imga.setStructureInode(CacheLocator.getContentTypeCache().getStructureByVelocityVarName("fileAsset").getInode());
        imga.setStringProperty(FileAssetAPI.HOST_FOLDER_FIELD, ff.getInode());
        imga.setStringProperty(FileAssetAPI.FILE_NAME_FIELD, "imgimg.jpg");
        imga.setStringProperty(FileAssetAPI.TITLE_FIELD, "imgimg.jpg");
        imga.setBinary(FileAssetAPI.BINARY_FIELD, imgimg);
        imga.setLanguageId(1);
        imga = contentletAPI.checkin(imga, user, false);

        Response response = webTarget.path("/publish/1")
            .request()
            .header(authheader, authvalue).put(Entity.entity(
                new JSONObject()
                    .put("stName", st.getVelocityVarName())
                    .put(file.getVelocityVarName(),"//demo.dotcms.com/rest/"+salt+"/filefile.txt")
                    .put(image.getVelocityVarName(), "//demo.dotcms.com/rest/"+salt+"/imgimg.jpg")
                    .put(title.getVelocityVarName(), "a simple title")
                    .toString(), MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(200, response.getStatus());

        String inode=(String)response.getHeaders().getFirst("inode");
        Contentlet cont = contentletAPI.find(inode, user, false);
        Assert.assertEquals(filea.getIdentifier(),cont.getStringProperty(file.getVelocityVarName()));
        Assert.assertEquals(imga.getIdentifier(),cont.getStringProperty(image.getVelocityVarName()));
    }

    @Test
    public void relationShips() throws Exception {
        final String salt=Long.toString(System.currentTimeMillis());

        Structure st1=new Structure();
        st1.setName("Rest Test rel "+salt);
        st1.setVelocityVarName("restTestSt"+salt);
        st1.setDescription("testing rest content creation with relationships");
        StructureFactory.saveStructure(st1);
        Field title1=new Field("Title",FieldType.TEXT,DataType.TEXT,st1,true,true,true,1,false,false,true);
        FieldFactory.saveField(title1);

        Structure st2=new Structure();
        st2.setName("Rest Test rel 2 "+salt);
        st2.setVelocityVarName("restTestSt2"+salt);
        st2.setDescription("testing rest content creation with relationships 2");
        StructureFactory.saveStructure(st2);
        Field title2=new Field("Title",FieldType.TEXT,DataType.TEXT,st2,true,true,true,1,false,false,true);
        FieldFactory.saveField(title2);

        Contentlet c1=new Contentlet();
        c1.setLanguageId(1);
        c1.setStringProperty(title2.getVelocityVarName(), "title 2");
        c1.setStructureInode(st2.getInode());
        c1 = contentletAPI.checkin(c1, user, false);

        Contentlet c2=new Contentlet();
        c2.setLanguageId(1);
        c2.setStringProperty(title2.getVelocityVarName(), "title 222");
        c2.setStructureInode(st2.getInode());
        c2 = contentletAPI.checkin(c2, user, false);

        contentletAPI.isInodeIndexed(c1.getInode());
        contentletAPI.isInodeIndexed(c2.getInode());
        Thread.sleep(2000);

        Relationship rel=new Relationship(st1,st2,"st1"+salt,"st2"+salt,0,false,false);
        APILocator.getRelationshipAPI().save(rel);

        Response response = webTarget.path("/publish/1")
                .request()
                .header(authheader, authvalue).put(Entity.entity(
                    new JSONObject()
                        .put("stName", st1.getVelocityVarName())
                        .put(title1.getVelocityVarName(), "a simple title")
                        .put(rel.getRelationTypeValue(), "+structureName:" + st2.getVelocityVarName())
                        .toString(), MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(200, response.getStatus());

        Thread.sleep(2000); // wait for relation fields update

        String inode=(String)response.getHeaders().getFirst("inode");
        Contentlet cc=contentletAPI.find(inode, user, false);

        List<Contentlet> relatedContent = contentletAPI.getRelatedContent(cc, rel, user, false);
        Assert.assertEquals(2, relatedContent.size());

        Set<String> inodes=new HashSet<String>();
        inodes.add(c1.getInode()); inodes.add(c2.getInode());
        inodes.remove(relatedContent.get(0).getInode());
        inodes.remove(relatedContent.get(1).getInode());
        Assert.assertEquals(0, inodes.size());

    }

    @Test
    public void newVersion() throws Exception {

        Response response = webTarget.path("/justSave/1")
                .request()
                .header(authheader, authvalue).put(Entity.entity(
                    new JSONObject()
                        .put("stName", "webPageContent")
                        .put("contentHost", "demo.dotcms.com")
                        .put("title", "testing newVersion")
                        .put("body", "just testing")
                        .toString(), MediaType.APPLICATION_JSON_TYPE));
        Assert.assertEquals(200, response.getStatus());

        String inode=(String)response.getHeaders().getFirst("inode");


        String identifier=(String)response.getHeaders().getFirst("identifier");

        response = webTarget.path("/justSave/1")
                .request()
                .header(authheader, authvalue).put(Entity.entity(
                    new JSONObject()
                        .put("stName", "webPageContent")
                        .put("contentHost", "demo.dotcms.com")
                        .put("title", "testing newVersion 2")
                        .put("body", "just testing 2")
                        .put("identifier", identifier)
                        .toString(), MediaType.APPLICATION_JSON_TYPE));
        String inode2=(String)response.getHeaders().getFirst("inode");
        String identifier2=(String)response.getHeaders().getFirst("identifier");

        Assert.assertEquals(identifier, identifier2);
        Assert.assertNotSame(inode, inode2);

        Contentlet c1=contentletAPI.find(inode, user, false);
        Contentlet c2=contentletAPI.find(inode2, user, false);

        Contentlet working=contentletAPI.findContentletByIdentifier(identifier, false, 1, user, false);

        Assert.assertEquals("testing newVersion 2", c2.getStringProperty("title"));
        Assert.assertEquals("just testing 2", c2.getStringProperty("body"));
        Assert.assertEquals(c1.getIdentifier(), c2.getIdentifier());

        Assert.assertEquals(working.getInode(), c2.getInode());
    }

    //Issue https://github.com/dotCMS/core/issues/12287
    @Test
    public void updateFileAssetContentTest() throws Exception{
        final Client client = ClientBuilder.newClient().register(MultiPartFeature.class);
        long currentTime = System.currentTimeMillis();
        Host demo=hostAPI.findByName("demo.dotcms.com", user, false);
        Folder folder=APILocator.getFolderAPI().createFolders("/rest/" + currentTime, demo, user, false);
        Contentlet contentlet = null;

        try{
            //Creating a temporary File to use in the binary fields.
            File imageFile = temporaryFolder.newFile("DummyFile.txt");
            writeTextIntoFile(imageFile, "This is the same file");

            Response response = client.target(webTarget.getUri() + "/publish/1").request()
                    .header(authheader, authvalue).put(Entity.entity(
                            new MultiPart()
                                    .bodyPart(new BodyPart(
                                            new JSONObject()
                                                    .put("hostFolder", "demo.dotcms.com:/rest/" + currentTime)
                                                    .put("title", "Test Content")
                                                    .put("fileName", imageFile.getName())
                                                    .put("languageId", "1")
                                                    .put("stName", "fileAsset")
                                                    .toString(), MediaType.APPLICATION_JSON_TYPE))
                                    .bodyPart(new StreamDataBodyPart(
                                            imageFile.getName(),
                                            FileUtils.openInputStream(imageFile),
                                            imageFile.getName(),
                                            MediaType.APPLICATION_OCTET_STREAM_TYPE))
                                    .bodyPart(new StreamDataBodyPart(
                                            imageFile.getName(),
                                            FileUtils.openInputStream(imageFile),
                                            imageFile.getName(),
                                            MediaType.APPLICATION_OCTET_STREAM_TYPE)), MediaType.MULTIPART_FORM_DATA_TYPE));

            Assert.assertEquals(200, response.getStatus());
            contentlet = contentletAPI.find((String) response.getHeaders().getFirst("inode"), user, false);
            Assert.assertNotNull(contentlet);

            response = client.target(webTarget.getUri() + "/publish/1").request()
                    .header(authheader, authvalue).put(Entity.entity(
                            new MultiPart()
                                    .bodyPart(new BodyPart(
                                            new JSONObject()
                                                    .put("hostFolder", "demo.dotcms.com:/rest/" + currentTime)
                                                    .put("title", "Test Content Updated")
                                                    .put("fileName", imageFile.getName())
                                                    .put("languageId", "1")
                                                    .put("stName", "fileAsset")
                                                    .put("identifier",contentlet.getIdentifier())
                                                    .toString(), MediaType.APPLICATION_JSON_TYPE))
                                    .bodyPart(new StreamDataBodyPart(
                                            imageFile.getName(),
                                            FileUtils.openInputStream(imageFile),
                                            imageFile.getName(),
                                            MediaType.APPLICATION_OCTET_STREAM_TYPE))
                                    .bodyPart(new StreamDataBodyPart(
                                            imageFile.getName(),
                                            FileUtils.openInputStream(imageFile),
                                            imageFile.getName(),
                                            MediaType.APPLICATION_OCTET_STREAM_TYPE)), MediaType.MULTIPART_FORM_DATA_TYPE));

            Assert.assertEquals(200, response.getStatus());
            Contentlet contentletUpdated = contentletAPI.find((String) response.getHeaders().getFirst("inode"), user, false);
            Assert.assertNotNull(contentletUpdated);

            Assert.assertNotEquals(contentlet.getTitle(), contentletUpdated.getTitle());


        } finally {
            //This method performs a cascade delete and also archives the content
            APILocator.getFolderAPI().delete(folder,user,false);
            //So no need to call delete again.
        }

    }

    @Test
    public void testCreateContentWithCats_AnonymousUser_RESTAPICONTENTALLOWFRONTENDSAVINGtrue_shouldSucceed200()
        throws DotSecurityException, DotDataException, JSONException {

        final boolean restAPIContentAllowFrontEndSavingValue =
            Config.getBooleanProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING);
        try {
            final long time = System.currentTimeMillis();
            final String TITLE_FIELD_VARIABLE = "title"+time;
            final String CAT_FIELD_VARIABLE = "eventType"+time;

            final ContentType type = createContentTypeWithCatAndTextField(TITLE_FIELD_VARIABLE, CAT_FIELD_VARIABLE);

            Config.setProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING, true);

            final Response response = webTarget.path("/save/1/").request()
                .put(Entity.entity(new JSONObject()
                    .put("stInode", type.id())
                    .put("languageId", 1)
                    .put(TITLE_FIELD_VARIABLE, "Test content from ContentResourceTest")
                    .put(CAT_FIELD_VARIABLE, "seminars")
                    .toString(), MediaType.APPLICATION_JSON_TYPE));

            assertTrue(response.getStatus() == Response.Status.OK.getStatusCode());
        } finally {
            Config.setProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING, restAPIContentAllowFrontEndSavingValue);
        }
    }

    @Test
    public void testCreateContentWithCats_AnonymousUser_RESTAPICONTENTALLOWFRONTENDSAVINGfalse_shouldFail403()
        throws DotSecurityException, DotDataException, JSONException {

        final boolean restAPIContentAllowFrontEndSavingValue =
            Config.getBooleanProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING);
        try {
            final long time = System.currentTimeMillis();
            final String TITLE_FIELD_VARIABLE = "title"+time;
            final String CAT_FIELD_VARIABLE = "eventType"+time;

            final ContentType type = createContentTypeWithCatAndTextField(TITLE_FIELD_VARIABLE, CAT_FIELD_VARIABLE);

            Config.setProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING, false);

            final Response response = webTarget.path("/save/1/").request()
                .put(Entity.entity(new JSONObject()
                    .put("stInode", type.id())
                    .put("languageId", 1)
                    .put(TITLE_FIELD_VARIABLE, "Test content from ContentResourceTest")
                    .put(CAT_FIELD_VARIABLE, "seminars")
                    .toString(), MediaType.APPLICATION_JSON_TYPE));

            assertTrue(response.getStatus() == Response.Status.FORBIDDEN.getStatusCode());
        } finally {
            Config.setProperty(REST_API_CONTENT_ALLOW_FRONT_END_SAVING, restAPIContentAllowFrontEndSavingValue);
        }
    }

    private ContentType createContentTypeWithCatAndTextField(final String textFieldVar, final String catFieldVar)
        throws DotSecurityException, DotDataException {
        final Host demoHost =hostAPI.findByName("demo.dotcms.com", user, false);

        final long time = System.currentTimeMillis();

        ContentType type = ContentTypeBuilder.builder(SimpleContentType.class)
            .name("TestCat" + time)
            .variable("TestCat" + time)
            .host(demoHost.getIdentifier())
            .build();

        type = APILocator.getContentTypeAPI(user).save(type);

        com.dotcms.contenttype.model.field.Field titleField = FieldBuilder.builder(TextField.class)
            .name(textFieldVar)
            .variable(textFieldVar)
            .contentTypeId(type.id())
            .build();

        APILocator.getContentTypeFieldAPI().save(titleField, user);

        final Category eventTypesCat = APILocator.getCategoryAPI().findByKey("event", user, false);

        com.dotcms.contenttype.model.field.Field catField = FieldBuilder.builder(CategoryField.class)
            .name(catFieldVar)
            .variable(catFieldVar)
            .values(eventTypesCat.getInode())
            .contentTypeId(type.id())
            .build();

        APILocator.getContentTypeFieldAPI().save(catField, user);

        return type;
    }

    /**
     * Util method to write dummy text into a file.
     *
     * @param file that we need to write. File should be empty.
     * @param textToWrite text that we are going to write into the file.
     */
    private void writeTextIntoFile(File file, final String textToWrite) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(textToWrite);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}











