package com.dotmarketing.portlets.linkchecker.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.LicenseTestUtil;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.datagen.HTMLPageDataGen;
import com.dotcms.mock.request.DotCMSMockRequestWithSession;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.ContainerStructure;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.HTMLPageAssetUtil;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * This class tests the Link Checker functionality provided in dotCMS. It
 * traverses the contents in an HTML page looking for the URLs in the anchor
 * tags and verifies if the URL they point to is valid or not.
 * 
 * @author Jorge Urdaneta
 * @since Nov 8, 2012
 *
 */
public class LinkCheckerAPITest extends IntegrationTestBase {

    protected static User sysuser=null;
    protected static Structure structure=null,urlmapstructure=null;
    protected static Host site =null, site2 =null;
    protected static Field field=null,urlmapfield=null;
    protected static Template template=null;
    protected static Container container=null;
    protected static String pageExt=null;
    protected static IHTMLPage detailPage=null;
    protected static List<HTMLPageAsset> pages=new ArrayList<>();

    @BeforeClass
    public static void createStructure() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        
        try {
        	LicenseTestUtil.getLicense();
        	
            pageExt = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");

            String uuid=UUIDGenerator.generateUuid();
            sysuser = APILocator.getUserAPI().getSystemUser();

            site = new Host();
            site.setHostname("linkcheckertesthost"+uuid.replaceAll("-", ".")+".demo.dotcms.com");
            site.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            site.setIndexPolicy(IndexPolicy.FORCE);
            site =APILocator.getHostAPI().save(site, sysuser, false);

            site2 = new Host();
            site2.setHostname("linkcheckertesthost-2-"+uuid.replaceAll("-", ".")+".demo.dotcms.com");
            site2.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
            site2.setIndexPolicy(IndexPolicy.FORCE);
            site2 =APILocator.getHostAPI().save(site2, sysuser, false);
            APILocator.getContentletAPI().isInodeIndexed(site2.getInode());

            structure=new Structure();
            structure.setHost(site.getIdentifier());
            structure.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
            structure.setName("linkchecker_test_structure_name"+uuid);
            structure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            structure.setOwner(sysuser.getUserId());
            structure.setVelocityVarName("linkchecker_test_structure"+uuid.replaceAll("-", "_"));
            StructureFactory.saveStructure(structure);
            CacheLocator.getContentTypeCache().add(structure);

            field = new Field("html", Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, structure,
                    true, true, true, 1, "", "", "", false, false, true);
            field.setVelocityVarName("html");
            field = FieldFactory.saveField(field);
            FieldsCache.addField(field);

            container=new Container();
            container.setTitle("LinkChecker Container "+uuid);
            container.setCode("$html");
            container.setMaxContentlets(0);

            List<ContainerStructure> csList = new ArrayList<>();
            ContainerStructure cs = new ContainerStructure();
            cs.setStructureId(structure.getInode());
            cs.setCode("this is the code");
            csList.add(cs);
            container=APILocator.getContainerAPI().save(container, csList, site, sysuser, false);

            template=new Template();
            template.setTitle("empty template "+uuid);
            template.setBody("<html><body>\n #parseContainer('"+container.getIdentifier()+"')\n </body></html>");
            template=APILocator.getTemplateAPI().saveTemplate(template, site, sysuser, false);

            // detail page for url mapped structure
            Folder folder=APILocator.getFolderAPI().createFolders("/detail/", site, sysuser, false);
            detailPage = new HTMLPageDataGen(folder, template).nextPersisted();

            // url mapped structure
            urlmapstructure=new Structure();
            urlmapstructure.setHost(site.getIdentifier());
            urlmapstructure.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
            urlmapstructure.setName("linkchecker_test_urlmapstructure_name"+uuid);
            urlmapstructure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            urlmapstructure.setOwner(sysuser.getUserId());
            urlmapstructure.setVelocityVarName("linkchecker_test_urlmapstructure"+uuid.replaceAll("-", "_"));
            urlmapstructure.setDetailPage(detailPage.getIdentifier());
            urlmapstructure.setUrlMapPattern("/test_mapped/{a}");
            StructureFactory.saveStructure(urlmapstructure);
            CacheLocator.getContentTypeCache().add(urlmapstructure);

            urlmapfield = new Field("a", Field.FieldType.TEXT, Field.DataType.TEXT, urlmapstructure,
                    true, true, true, 1, "", "", "", false, false, true);
            urlmapfield.setVelocityVarName("a");
            urlmapfield = FieldFactory.saveField(urlmapfield);
            FieldsCache.addField(urlmapfield);

        }
        finally {
            //setDebugMode(true);
            HibernateUtil.closeSession();
        }
    }

    @AfterClass
    public static void disposeThings() throws Exception {
        try {
            List<Contentlet> contentList=new ArrayList<>();
            contentList.addAll(APILocator.getContentletAPI().findByStructure(structure.getInode(), sysuser, false, 0, 0));
            contentList.addAll(APILocator.getContentletAPI().findByStructure(urlmapstructure.getInode(), sysuser, false, 0, 0));
            APILocator.getContentletAPI().archive(contentList, sysuser, false);
            APILocator.getContentletAPI().delete(contentList, sysuser, false);

            for(HTMLPageAsset pp : pages){
                    APILocator.getContentletAPI().archive(pp, sysuser, false);
            		APILocator.getContentletAPI().delete(pp, sysuser, false,true);
            }
            APILocator.getTemplateAPI().delete(template, sysuser, false);
            APILocator.getContainerAPI().delete(container, sysuser, false);

            FieldFactory.deleteField(field);
            FieldFactory.deleteField(urlmapfield);
            APILocator.getStructureAPI().delete(structure, sysuser);
            APILocator.getStructureAPI().delete(urlmapstructure, sysuser);
            APILocator.getHostAPI().archive(site, sysuser, false);
            APILocator.getHostAPI().delete(site, sysuser, false);
            APILocator.getHostAPI().archive(site2, sysuser, false);
            APILocator.getHostAPI().delete(site2, sysuser, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            HibernateUtil.closeSession();
        }
    }

   /* @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException {

        cleanupDebug(LinkCheckerAPITest.class);
    }*/

    @Test
    public void findInvalidLinks() throws Exception {
        final HttpServletRequest previousRequest = HttpServletRequestThreadLocal.INSTANCE.getRequest();

        try {
            final HttpSession session = mock(HttpSession.class);
            final DotCMSMockRequestWithSession request = new DotCMSMockRequestWithSession(session,
                    false);
            HttpServletRequestThreadLocal.INSTANCE.setRequest(request);

            //////////////////////////
            // basic external links //
            //////////////////////////
            String[] extlinks = new String[]{
                    "http://thissitedoesntexists.imsureaboutthat.badextension.",
                    "https://somebadhostovergoogle.google.com.",
                    // hope they don't create it in the future
                    "http://thisisabadhostover.dotcms.comx.", // yeah small typo
                    "mailto:dev@dotcms.com", // should ignore this one
                    "webcalc://somehostnomatter.itsbad.", // should ignore this
                    "http://www.oracle.com./index.html", // bad URL
                    "https://github.com/dotCMS/core" // this is a good link
            };
            HashSet<String> links = new HashSet<>(Arrays.asList(extlinks));
            StringBuilder sb = new StringBuilder("<html><body>\n");
            for (String ll : extlinks)
                sb.append("<a href='").append(ll)
                        .append("' title='short title'>this is a link</a>\n");
            sb.append("</body></html>");

            Contentlet con = new Contentlet();
            con.setStructureInode(structure.getInode());
            con.setStringProperty("html", sb.toString());
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            List<InvalidLink> invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertTrue(invalids.size() > 0);

            for (InvalidLink il : invalids) {
                System.out.println("url: " + il.getUrl());
            }

            assertEquals(4, invalids.size());

            for (InvalidLink il : invalids) {
                assertEquals(il.getTitle(), "short title");
                assertTrue(links.remove(il.getUrl()));
            }

            ///////////////////////////////////////
            // basic internal links to htmlpages //
            ///////////////////////////////////////

            APILocator.getFolderAPI().createFolders("/a_test/b_test/", site, sysuser, false);
            Folder Fa = APILocator.getFolderAPI()
                    .findFolderByPath("/a_test/", site, sysuser, false);
            Folder Fab = APILocator.getFolderAPI()
                    .findFolderByPath("/a_test/b_test", site, sysuser, false);

            HTMLPageAsset page1 = HTMLPageAssetUtil.createDummyPage("index", "index", "index",
                    template, Fa, site);
            pages.add(page1);
            HTMLPageAsset page2 = HTMLPageAssetUtil.createDummyPage("something", "something",
                    "something", template, Fa, site);
            pages.add(page2);
            HTMLPageAsset page3 = HTMLPageAssetUtil.createDummyPage("index", "index", "index",
                    template, Fab, site);
            pages.add(page3);
            HTMLPageAsset page4 = HTMLPageAssetUtil.createDummyPage("something", "something",
                    "something", template, Fab, site);
            pages.add(page4);

            extlinks = new String[]{
                    page1.getURI(), page2.getURI(), page3.getURI(), page4.getURI(), // direct hit!
                    "/a_test/", "/a_test/b_test", // should be good as it hits index page
                    "/a_test/notnotnot_exists." + pageExt,  // a bad one!
                    "?relative=yes", // should be ignored
                    "#id_inside_page", // ignored too
                    page2.getURI() + "?a=1&b=2" // with query string
            };

            sb = new StringBuilder("<html><body>\n");
            for (String ll : extlinks)
                sb.append("<a href='").append(ll).append("'>link</a>\n");
            sb.append("</body></html>");

            con = new Contentlet();
            con.setStructureInode(structure.getInode());
            con.setStringProperty("html", sb.toString());
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(invalids.size(), 1);
            assertEquals(invalids.get(0).getUrl(), "/a_test/notnotnot_exists." + pageExt);

            ////////////////////////
            // basic urlmap links //
            ////////////////////////
            con = new Contentlet();
            con.setStructureInode(urlmapstructure.getInode());
            con.setStringProperty("a", "url1");
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            con = new Contentlet();
            con.setStructureInode(urlmapstructure.getInode());
            con.setStringProperty("a", "url2");
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            extlinks = new String[]{
                    "/test_mapped/url1", "/test_mapped/url2/", // those should be good
                    "/test_mapped/url1#ignorethis",
                    "/test_mapped/url2/#againignore",
                    "/test_mapped/url3" // a bad one
            };
            sb = new StringBuilder("<html><body>\n");
            for (String ll : extlinks)
                sb.append("<a href='").append(ll).append("'>link</a>\n");
            sb.append("</body></html>");

            con = new Contentlet();
            con.setStructureInode(structure.getInode());
            con.setStringProperty("html", sb.toString());
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(invalids.size(), 1);
            assertEquals(invalids.get(0).getUrl(), "/test_mapped/url3");

            /* Now using two hosts. In host1 we gonna put a content with a valid
             * internal link to a page. Then we add the contentlet in a page that
             * lives in another host. That should break the internal link */
            con = new Contentlet();
            con.setStringProperty("html", "<html><body>" +
                    "<a href='" + page2.getURI() + "'>thislink</a>" +
                    "<a href='" + page3.getURI() + "'>thislink</a>" +
                    "<a href='" + page4.getURI() + "'>thislink</a>" +
                    "</body></html>");
            con.setStructureInode(structure.getInode());
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);
            MultiTree mtree = new MultiTree();
            mtree.setParent1(page1.getIdentifier());
            mtree.setParent2(container.getIdentifier());
            mtree.setChild(con.getIdentifier());
            mtree.setTreeOrder(1);
            APILocator.getMultiTreeAPI().saveMultiTree(mtree);

            // that should be ok. It is in the same host where those pages are valid
            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(invalids.size(), 0);

            // now lets add some salt here. If the content is added in a page in host2 it
            // should break the internal links
            Folder home = APILocator.getFolderAPI().createFolders("/home/", site2, sysuser, false);
            HTMLPageAsset page5 = HTMLPageAssetUtil.createDummyPage("something", "something",
                    "something", template, home, site2);
            pages.add(page5);

            con = new Contentlet();
            con.setStringProperty("html", "<html><body>" +
                    "<a href='" + page2.getURI() + "'>thislink</a>" +
                    "<a href='" + page3.getURI() + "'>thislink</a>" +
                    "<a href='" + page4.getURI() + "'>thislink</a>" +
                    "</body></html>");
            con.setStructureInode(structure.getInode());
            con.setHost(site2.getIdentifier());
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);
            mtree = new MultiTree();
            mtree.setParent1(page5.getIdentifier());
            mtree.setParent2(container.getIdentifier());
            mtree.setChild(con.getIdentifier());
            mtree.setTreeOrder(1);
            APILocator.getMultiTreeAPI().saveMultiTree(mtree);

            // now all those links should be broken
            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(3, invalids.size());
            links = new HashSet<>(
                    Arrays.asList(new String[]{page2.getURI(), page3.getURI(), page4.getURI()}));
            for (InvalidLink link : invalids)
                assertTrue(links.remove(link.getUrl()));

            ///////////////////////////////////////
            // Content htmlpages                 //
            ///////////////////////////////////////

            APILocator.getFolderAPI()
                    .createFolders("/a_html_asset_test/b_html_asset_test/", site, sysuser, false);
            Folder Fahtml = APILocator.getFolderAPI()
                    .findFolderByPath("/a_html_asset_test/", site, sysuser, false);
            Folder Fabhtml = APILocator.getFolderAPI()
                    .findFolderByPath("/a_html_asset_test/b_html_asset_test", site, sysuser, false);

            HTMLPageAsset page6 = HTMLPageAssetUtil.createDummyPage("index", "index", "index",
                    template, Fahtml, site);
            pages.add(page6);
            HTMLPageAsset page7 = HTMLPageAssetUtil.createDummyPage("something", "something",
                    "something", template, Fahtml, site);
            pages.add(page7);
            HTMLPageAsset page8 = HTMLPageAssetUtil.createDummyPage("index", "index", "index",
                    template, Fabhtml, site);
            pages.add(page8);
            HTMLPageAsset page9 = HTMLPageAssetUtil.createDummyPage("something", "something",
                    "something", template, Fabhtml, site);
            pages.add(page9);

            extlinks = new String[]{
                    page6.getURI(), page7.getURI(), page8.getURI(), page9.getURI(), // direct hit!
                    "/a_html_asset_test/", "/a_html_asset_test/b_html_asset_test",
                    // should be good as it hits index page
                    "/a_html_asset_test/notnotnot_exists",  // a bad one!
                    page7.getURI() + "?a=1&b=2" // with query string
            };

            sb = new StringBuilder("<html><body>\n");
            for (String ll : extlinks)
                sb.append("<a href='").append(ll).append("'>link</a>\n");
            sb.append("</body></html>");

            con = new Contentlet();
            con.setStructureInode(structure.getInode());
            con.setStringProperty("html", sb.toString());
            con.setHost(site.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(invalids.size(), 1);
            assertEquals(invalids.get(0).getUrl(), "/a_html_asset_test/notnotnot_exists");

            // content on host2 referencing pages on host
            con = new Contentlet();
            con.setStringProperty("html", "<html><body>" +
                    "<a href='" + page6.getURI() + "'>thislink</a>" +
                    "<a href='" + page7.getURI() + "'>thislink</a>" +
                    "<a href='" + page8.getURI() + "'>thislink</a>" +
                    "</body></html>");
            con.setStructureInode(structure.getInode());
            con.setHost(site2.getIdentifier());
            con.setIndexPolicy(IndexPolicy.FORCE);
            con = APILocator.getContentletAPI().checkin(con, sysuser, false);

            // now all those links should be broken
            invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
            assertTrue(invalids != null);
            assertEquals(3, invalids.size());
            links = new HashSet<>(
                    Arrays.asList(new String[]{page6.getURI(), page7.getURI(), page8.getURI()}));

            APILocator.getVersionableAPI().setWorking(page6);
            APILocator.getVersionableAPI().setWorking(page7);
            APILocator.getVersionableAPI().setWorking(page8);
            APILocator.getVersionableAPI().setWorking(page9);
            HibernateUtil.flush();
            HibernateUtil.closeSession();

            for (InvalidLink link : invalids)
                assertTrue(links.remove(link.getUrl()));
        } finally {
            HttpServletRequestThreadLocal.INSTANCE.setRequest(previousRequest);
        }
    }

}
