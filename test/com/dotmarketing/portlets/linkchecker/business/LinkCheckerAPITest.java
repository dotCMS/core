package com.dotmarketing.portlets.linkchecker.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.TestBase;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.portlets.linkchecker.bean.InvalidLink;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.UUIDGenerator;


public class LinkCheckerAPITest extends TestBase {
    
    protected Structure structure=null;
    
    @BeforeClass
    public void createStructure() throws Exception {
        String uuid=UUIDGenerator.generateUuid();
        structure=new Structure();
        structure.setHost(APILocator.getHostAPI().findSystemHost().getIdentifier());
        structure.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
        structure.setName("linkchecker_test_structure_name"+uuid);
        structure.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        structure.setOwner(APILocator.getUserAPI().getSystemUser().getUserId());
        structure.setVelocityVarName("linkchecker_test_structure"+uuid.replaceAll("-", "_"));
        StructureFactory.saveStructure(structure);
        StructureCache.addStructure(structure);

        Field field = new Field("html", Field.FieldType.WYSIWYG, Field.DataType.LONG_TEXT, structure,
                true, true, true, 1, "", "", "", true, false, true);
        field.setVelocityVarName("html");
        FieldFactory.saveField(field);
        FieldsCache.addField(field);
    }
    
    @Test
    public void findInvalidLinks() throws Exception {
        // testing simple external links
        String[] extlinks=new String[]{
            "http://thissitedoesntexists.imshureaboutthat.badextension",
            "https://somebadhostovergoogle.google.com", // hope they don't create it in the future
            "http://thisisabadhostover.dotcms.comx", // yeah small typo
            "https://dotcms.com" // this is a good link
        };
        HashSet<String> links=new HashSet<String>(Arrays.asList(extlinks));
        StringBuilder sb=new StringBuilder("<html><body>\n");
        for(String ll : extlinks) sb.append("<a href='").append(ll).append("' title='short title'>this is a link</a>\n");
        sb.append("</body></html>");
        List<InvalidLink> invalids = APILocator.getLinkCheckerAPI().findInvalidLinks(sb.toString(), APILocator.getUserAPI().getSystemUser());
        assertNotNull(invalids);
        assertEquals(invalids.size(), extlinks.length-1);
        for(InvalidLink il : invalids)
            assertTrue(links.remove(il.getUrl()));
        assertEquals(links.size(),1);
        assertEquals(links.toArray()[0],extlinks[extlinks.length-1]);
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
