package com.dotcms.rest;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.StreamDataBodyPart;

public class ContentResourceTest extends TestBase {
    Client client;
    WebResource contRes;
    String authheader="Authorization";
    String authvalue="Basic "+new String(Base64.encode("admin@dotcms.com:admin"));
    
    @Before
    public void before() {
        client=Client.create();
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        long serverPort = request.getServerPort();
        contRes = client.resource("http://"+serverName+":"+serverPort+"/api/content");
    }
    
    @Test
    public void singlePUT() throws Exception {
        Structure st=StructureCache.getStructureByVelocityVarName("webPageContent");
        Host demo=APILocator.getHostAPI().findByName("demo.dotcms.com", APILocator.getUserAPI().getSystemUser(), false);
        User sysuser=APILocator.getUserAPI().getSystemUser();
        String demoId=demo.getIdentifier();
        ClientResponse response=
                contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                                new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest")
                                .put("body", "this is an example text")
                                .put("contentHost", demoId).toString());
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        String location=response.getLocation().toString();
        String inode=location.substring(location.lastIndexOf("/")+1);
        Contentlet cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertEquals(st.getInode(), cont.getStructureInode());
        Assert.assertEquals(1,cont.getLanguageId());
        Assert.assertEquals("Test content from ContentResourceTest",cont.getStringProperty("title"));
        Assert.assertEquals("this is an example text",cont.getStringProperty("body"));
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: folderId
        Folder folder=APILocator.getFolderAPI().findFolderByPath("/home", demo, sysuser, false);
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                                new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", folder.getInode()).toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: hostname
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_JSON_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com").toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(demoId, cont.getHost());
        Assert.assertTrue(cont.isLive());
        
        // testing other host_or_folder formats: hostname:path
        response=contRes.path("/justsave/1").type(MediaType.APPLICATION_JSON_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                        new JSONObject()
                                .put("stInode", st.getInode())
                                .put("languageId", 1)
                                .put("title", "Test content from ContentResourceTest (folderId)")
                                .put("body", "this is an example text")
                                .put("contentHost", "demo.dotcms.com:/home").toString());
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
        Assert.assertEquals(folder.getInode(), cont.getFolder());
        Assert.assertFalse(cont.isLive());
        
        
        // testing XML
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_XML_TYPE)
                       .header(authheader, authvalue).put(ClientResponse.class,
                            "<content>" +
                            "<stInode>" +st.getInode() + "</stInode>"+
                            "<languageId>1</languageId>"+
                            "<title>Test content from ContentResourceTest XML</title>"+
                            "<body>this is an example text XML</body>"+
                            "<contentHost>"+demoId+"</contentHost>"+
                            "</content>");
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
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
        response=contRes.path("/publish/1").type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                .header(authheader, authvalue).put(ClientResponse.class,
                     "stInode=" +st.getInode() + "&"+
                     "languageId=1&"+
                     "title="+URLEncoder.encode(title, "UTF-8")+"&"+
                     "body="+URLEncoder.encode(body, "UTF-8")+"&"+
                     "contentHost="+demoId);
        Assert.assertEquals(200, response.getStatus());
        Assert.assertTrue(response.getLocation().toString().contains("/api/content/inode/"));
        location=response.getLocation().toString();
        inode=location.substring(location.lastIndexOf("/")+1);
        Assert.assertEquals(inode, response.getHeaders().getFirst("inode")); // validate consistency of inode header
        cont=APILocator.getContentletAPI().find(inode, sysuser, false);
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
        final User sysuser=APILocator.getUserAPI().getSystemUser();
        
        ClientResponse response = contRes.path("/publish/1").type(MediaType.MULTIPART_FORM_DATA_TYPE)
                                   .header(authheader, authvalue).put(ClientResponse.class, 
                                           new MultiPart()
                                             .bodyPart(new BodyPart(
                                                     new JSONObject()
                                                        .put("hostFolder", "demo.dotcms.com:/resources")
                                                        .put("title", "newfile"+salt+".txt")
                                                        .put("fileName", "newfile"+salt+".txt")
                                                        .put("languageId", "1")
                                                        .put("stInode", StructureCache.getStructureByVelocityVarName("FileAsset").getInode())
                                                        .toString(), MediaType.APPLICATION_JSON_TYPE))
                                             .bodyPart(new StreamDataBodyPart(
                                                         "newfile"+salt+".txt", 
                                                         new ByteArrayInputStream(("this is the salt "+salt).getBytes()),
                                                         "newfile"+salt+".txt",
                                                         MediaType.APPLICATION_OCTET_STREAM_TYPE)));
        Assert.assertEquals(200, response.getStatus());
        Contentlet cont=APILocator.getContentletAPI().find(response.getHeaders().getFirst("inode"),sysuser,false);
        Assert.assertNotNull(cont);
        Assert.assertTrue(InodeUtils.isSet(cont.getIdentifier()));
        Assert.assertTrue(response.getLocation().toString().endsWith("/api/content/inode/"+cont.getInode()));
        FileAsset file=APILocator.getFileAssetAPI().fromContentlet(cont);
        Assert.assertEquals("/resources/newfile"+salt+".txt",file.getURI());
        Assert.assertEquals("demo.dotcms.com", APILocator.getHostAPI().find(file.getHost(), sysuser, false).getHostname());
        Assert.assertEquals("this is the salt "+salt, IOUtils.toString(file.getFileInputStream()));
    }
}











