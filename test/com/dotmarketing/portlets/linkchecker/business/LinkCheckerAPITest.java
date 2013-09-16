package com.dotmarketing.portlets.linkchecker.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;


public class LinkCheckerAPITest extends TestBase {
    
    protected static User sysuser=null;
    protected static Structure structure=null,urlmapstructure=null;
    protected static Host host=null,host2=null;
    protected static Field field=null,urlmapfield=null;
    protected static Template template=null;
    protected static Container container=null;
    protected static String pageExt=null; 
    protected static HTMLPage detailPage=null;
    
    @BeforeClass
    public static void createStructure() throws Exception {
        try {
            pageExt = Config.getStringProperty("VELOCITY_PAGE_EXTENSION");
            
            String uuid=UUIDGenerator.generateUuid();
            sysuser = APILocator.getUserAPI().getSystemUser();
            
            host = new Host();
            host.setHostname("lickcheckertesthost"+uuid.replaceAll("-", "_")+".demo.dotcms.com");
            host=APILocator.getHostAPI().save(host, sysuser, false);
            APILocator.getContentletAPI().isInodeIndexed(host.getInode());
            
            host2 = new Host();
            host2.setHostname("lickcheckertesthost_2_"+uuid.replaceAll("-", "_")+".demo.dotcms.com");
            host2=APILocator.getHostAPI().save(host2, sysuser, false);
            APILocator.getContentletAPI().isInodeIndexed(host2.getInode());
            
            structure=new Structure();
            structure.setHost(host.getIdentifier());
            structure.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
            structure.setName("linkchecker_test_structure_name"+uuid);
            structure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            structure.setOwner(sysuser.getUserId());
            structure.setVelocityVarName("linkchecker_test_structure"+uuid.replaceAll("-", "_"));
            StructureFactory.saveStructure(structure);
            StructureCache.addStructure(structure);
    
            field = new Field("html", Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, structure,
                    true, true, true, 1, "", "", "", true, false, true);
            field.setVelocityVarName("html");
            FieldFactory.saveField(field);
            FieldsCache.addField(field);
                    
            container=new Container();
            container.setTitle("LinkChecker Container "+uuid);
            container.setCode("$html");
            container.setStructureInode(structure.getInode());
            container.setMaxContentlets(0);
            container=APILocator.getContainerAPI().save(container, structure, host, sysuser, false);
            
            template=new Template();
            template.setTitle("empty template "+uuid);
            template.setBody("<html><body>\n #parseContainer('"+container.getIdentifier()+"')\n </body></html>");
            template=APILocator.getTemplateAPI().saveTemplate(template, host, sysuser, false);
            
            // detail page for url mapped structure
            Folder folder=APILocator.getFolderAPI().createFolders("/detail/", host, sysuser, false);
            detailPage=new HTMLPage();
            detailPage.setFriendlyName("detail");        
            detailPage.setPageUrl("detail."+pageExt);
            detailPage.setTitle("index page");
            detailPage.setTemplateId(template.getIdentifier());
            detailPage=APILocator.getHTMLPageAPI().saveHTMLPage(detailPage, template, folder, sysuser, false);
            
            // url mapped structure
            urlmapstructure=new Structure();
            urlmapstructure.setHost(host.getIdentifier());
            urlmapstructure.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
            urlmapstructure.setName("linkchecker_test_urlmapstructure_name"+uuid);
            urlmapstructure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
            urlmapstructure.setOwner(sysuser.getUserId());
            urlmapstructure.setVelocityVarName("linkchecker_test_urlmapstructure"+uuid.replaceAll("-", "_"));
            urlmapstructure.setDetailPage(detailPage.getIdentifier());
            urlmapstructure.setUrlMapPattern("/test_mapped/{a}");
            StructureFactory.saveStructure(urlmapstructure);
            StructureCache.addStructure(urlmapstructure);
    
            urlmapfield = new Field("a", Field.FieldType.TEXT, Field.DataType.TEXT, urlmapstructure,
                    true, true, true, 1, "", "", "", true, false, true);
            urlmapfield.setVelocityVarName("a");
            FieldFactory.saveField(urlmapfield);
            FieldsCache.addField(urlmapfield);
    
        }
        finally {
            HibernateUtil.closeSession();
        }
    }
    
    @AfterClass
    public static void disposeThings() throws Exception {
        try {
            HibernateUtil.closeSession();
            List<Contentlet> contentList=new ArrayList<Contentlet>();
            contentList.addAll(APILocator.getContentletAPI().findByStructure(structure.getInode(), sysuser, false, 0, 0));
            contentList.addAll(APILocator.getContentletAPI().findByStructure(urlmapstructure.getInode(), sysuser, false, 0, 0));
            APILocator.getContentletAPI().delete(contentList, sysuser, false);
            
            for(HTMLPage pp : APILocator.getHTMLPageAPI().findHtmlPages(sysuser, false, null, null, null, null, template.getIdentifier(), 0, -1, null))
                APILocator.getHTMLPageAPI().delete(pp, sysuser, false);

            APILocator.getTemplateAPI().delete(template, sysuser, false);
            APILocator.getContainerAPI().delete(container, sysuser, false);
            
            FieldFactory.deleteField(field);
            FieldFactory.deleteField(urlmapfield);
            StructureFactory.deleteStructure(structure);
            StructureFactory.deleteStructure(urlmapstructure);
            APILocator.getHostAPI().archive(host, sysuser, false);
            APILocator.getHostAPI().delete(host, sysuser, false);
            APILocator.getHostAPI().archive(host2, sysuser, false);
            APILocator.getHostAPI().delete(host2, sysuser, false);
        }
        finally {
            HibernateUtil.closeSession();
        }
    }
    
    @Test
    public void findInvalidLinks() throws Exception {
        
        
        //////////////////////////
        // basic external links //
        //////////////////////////
        String[] extlinks=new String[]{
            "http://thissitedoesntexists.imsureaboutthat.badextension",
            "https://somebadhostovergoogle.google.com", // hope they don't create it in the future
            "http://thisisabadhostover.dotcms.comx", // yeah small typo
            "mailto:dev@dotcms.com", // should ignore this one
            "webcalc://somehostnomatter.itsbad", // should ignore this
            "http://www.oracle.com/index.html" // this is a good link
        };
        HashSet<String> links=new HashSet<String>(Arrays.asList(extlinks));
        StringBuilder sb=new StringBuilder("<html><body>\n");
        for(String ll : extlinks) sb.append("<a href='").append(ll).append("' title='short title'>this is a link</a>\n");
        sb.append("</body></html>");
        
        Contentlet con=new Contentlet();
        con.setStructureInode(structure.getInode());
        con.setStringProperty("html", sb.toString());
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        
        List<InvalidLink> invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
        assertTrue(invalids!=null);
        assertTrue(invalids.size()>0);
        assertEquals(3,invalids.size());
        
        for(InvalidLink il : invalids) {
            assertEquals(il.getTitle(),"short title");
            assertTrue(links.remove(il.getUrl()));
        }
        
        ///////////////////////////////////////
        // basic internal links to htmlpages //
        ///////////////////////////////////////
        
        APILocator.getFolderAPI().createFolders("/a_test/b_test/", host, sysuser, false);
        Folder Fa=APILocator.getFolderAPI().findFolderByPath("/a_test/", host, sysuser, false);
        Folder Fab=APILocator.getFolderAPI().findFolderByPath("/a_test/b_test", host, sysuser, false);
        
        HTMLPage page1=new HTMLPage();
        page1.setFriendlyName("index");        
        page1.setPageUrl("index."+pageExt);
        page1.setTitle("index page");
        page1.setTemplateId(template.getIdentifier());
        page1=APILocator.getHTMLPageAPI().saveHTMLPage(page1, template, Fa, sysuser, false);
        
        HTMLPage page2=new HTMLPage();
        page2.setFriendlyName("something");
        page2.setPageUrl("something."+pageExt);
        page2.setTitle("something");
        page2.setTemplateId(template.getIdentifier());
        page2=APILocator.getHTMLPageAPI().saveHTMLPage(page2, template, Fa, sysuser, false);
        
        HTMLPage page3=new HTMLPage();
        page3.setFriendlyName("index");
        page3.setPageUrl("index."+pageExt);
        page3.setTitle("index page");
        page3.setTemplateId(template.getIdentifier());
        page3=APILocator.getHTMLPageAPI().saveHTMLPage(page3, template, Fab, sysuser, false);
        
        HTMLPage page4=new HTMLPage();
        page4.setFriendlyName("something");
        page4.setPageUrl("something."+pageExt);
        page4.setTitle("something");
        page4.setTemplateId(template.getIdentifier());
        page4=APILocator.getHTMLPageAPI().saveHTMLPage(page4, template, Fab, sysuser, false);
        
        extlinks=new String[] {
            page1.getURI(), page2.getURI(), page3.getURI(), page4.getURI(), // direct hit!
            "/a_test/", "/a_test/b_test", // should be good as it hits index page
            "/a_test/notnotnot_exists."+pageExt,  // a bad one!
            "?relative=yes", // should be ignored
            "#id_inside_page", // ignored too 
            page2.getURI()+"?a=1&b=2" // with query string
        };
        
        sb=new StringBuilder("<html><body>\n");
        for(String ll : extlinks) sb.append("<a href='").append(ll).append("'>link</a>\n");
        sb.append("</body></html>");
        
        con=new Contentlet();
        con.setStructureInode(structure.getInode());
        con.setStringProperty("html", sb.toString());
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        
        invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
        assertTrue(invalids!=null);
        assertEquals(invalids.size(),1);
        assertEquals(invalids.get(0).getUrl(),"/a_test/notnotnot_exists."+pageExt);
        
        ////////////////////////
        // basic urlmap links //
        ////////////////////////
        con=new Contentlet();
        con.setStructureInode(urlmapstructure.getInode());
        con.setStringProperty("a", "url1");
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        
        con=new Contentlet();
        con.setStructureInode(urlmapstructure.getInode());
        con.setStringProperty("a", "url2");
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        
        extlinks=new String[] {
           "/test_mapped/url1","/test_mapped/url2/", // those should be good
           "/test_mapped/url1#ignorethis",
           "/test_mapped/url2/#againignore",
           "/test_mapped/url3" // a bad one
        };
        sb=new StringBuilder("<html><body>\n");
        for(String ll : extlinks) sb.append("<a href='").append(ll).append("'>link</a>\n");
        sb.append("</body></html>");
        
        con=new Contentlet();
        con.setStructureInode(structure.getInode());
        con.setStringProperty("html", sb.toString());
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        
        invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
        assertTrue(invalids!=null);
        assertEquals(invalids.size(),1);
        assertEquals(invalids.get(0).getUrl(),"/test_mapped/url3");
        
        /* Now using two hosts. In host1 we gonna put a content with a valid
         * internal link to a page. Then we add the contentlet in a page that
         * lives in another host. That should break the internal link */ 
        con=new Contentlet();
        con.setStringProperty("html", "<html><body>" +
        		              "<a href='"+page2.getURI()+"'>thislink</a>" +
        		              "<a href='"+page3.getURI()+"'>thislink</a>" +
        		              "<a href='"+page4.getURI()+"'>thislink</a>" +
        				      "</body></html>");
        con.setStructureInode(structure.getInode());
        con=APILocator.getContentletAPI().checkin(con, sysuser, false);
        APILocator.getContentletAPI().isInodeIndexed(con.getInode());
        MultiTree mtree=new MultiTree();
        mtree.setParent1(page1.getIdentifier());
        mtree.setParent2(container.getIdentifier());
        mtree.setChild(con.getIdentifier());
        mtree.setTreeOrder(1);
        MultiTreeFactory.saveMultiTree(mtree);
        
        // that should be ok. It is in the same host where those pages are valid
        invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
        assertTrue(invalids!=null);
        assertEquals(invalids.size(),0);
        
        // now lets add some salt here. If the content is added in a page in host2 it 
        // should break the internal links
        Folder home=APILocator.getFolderAPI().createFolders("/home/", host2, sysuser, false);
        HTMLPage page5=new HTMLPage();
        page5.setFriendlyName("something");
        page5.setPageUrl("something."+pageExt);
        page5.setTitle("something");
        page5.setTemplateId(template.getIdentifier());
        page5=APILocator.getHTMLPageAPI().saveHTMLPage(page5, template, home, sysuser, false);
        
        mtree=new MultiTree();
        mtree.setParent1(page5.getIdentifier());
        mtree.setParent2(container.getIdentifier());
        mtree.setChild(con.getIdentifier());
        mtree.setTreeOrder(1);
        MultiTreeFactory.saveMultiTree(mtree);
        
        // now all those links should be broken
        invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(con);
        assertTrue(invalids!=null);
        assertEquals(3,invalids.size());
        links=new HashSet<String>(Arrays.asList(new String[] {page2.getURI(),page3.getURI(),page4.getURI()}));
        for(InvalidLink link : invalids)
            assertTrue(links.remove(link.getUrl()));
    }
    
    @Test
    public void saveInvalidLinks() {
        
    }
    
    @Test
    public void deleteInvalidLinks() {
        
    }
    
    @Test
    public void findByInode()  {
        
    }
    
    @Test
    public void findAll() {
        
    }
    
    @Test
    public void findAllCount() {
        
    }
}
