package com.dotmarketing.portlets.structure.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.IntegrationTestBase;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Field.FieldType;
import com.dotmarketing.portlets.structure.model.FieldVariable;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

public class FieldAPITest extends IntegrationTestBase {
	
    @BeforeClass
    public static void prepare () throws Exception {
    	
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }
    
    @Test
    public void getFieldVariablesForField() throws Exception {
        // make sure its cached. see https://github.com/dotCMS/dotCMS/issues/2465
        User user=APILocator.getUserAPI().getSystemUser();
        Structure st=new Structure();
        st.setFolder(APILocator.getFolderAPI().findSystemFolder().getInode());
        st.setHost(APILocator.getHostAPI().findDefaultHost(user, false).getIdentifier());
        st.setName("FieldAPITest_"+UUIDGenerator.generateUuid());
        st.setVelocityVarName(st.getName());
        st.setOwner(user.getUserId());
        st.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        StructureFactory.saveStructure(st);
        Field ff=new Field("title",FieldType.TEXT,Field.DataType.TEXT,st,true,true,true,1,false,false,true);
        ff = FieldFactory.saveField(ff);
        FieldVariable fv=new FieldVariable();
        fv.setFieldId(ff.getInode());
        fv.setName("variable1");
        fv.setKey("variable1");
        fv.setValue("value");
        fv.setLastModifierId(user.getUserId());
        fv.setLastModDate(new Date());
        APILocator.getFieldAPI().saveFieldVariable(fv, user, false);
        
        // this should make it live in cache
        List<FieldVariable> list=APILocator.getFieldAPI().getFieldVariablesForField(ff.getInode(), user, false);
        assertEquals(1,list.size());
        assertEquals(list.get(0).getKey(),fv.getKey());
        assertEquals(list.get(0).getValue(),fv.getValue());
        
        //List<FieldVariable> clist=(List<FieldVariable>) CacheLocator.getCacheAdministrator()
        //        .get(FieldsCache.getFieldsVarGroup()+ff.getInode(), 
        //             FieldsCache.getFieldsVarGroup());
        //assertNotNull(clist);
        //assertEquals(1,clist.size());
        //assertEquals(clist.get(0).getKey(),fv.getKey());
        //assertEquals(clist.get(0).getValue(),fv.getValue());
        
        // problems with second save ?
        // https://github.com/dotCMS/dotCMS/issues/2649
        
        FieldVariable fg=new FieldVariable();
        fg.setFieldId(ff.getInode());
        fg.setName("variable2");
        fg.setKey("variable2");
        fg.setValue("value");
        fg.setLastModifierId(user.getUserId());
        fg.setLastModDate(new Date());
        APILocator.getFieldAPI().saveFieldVariable(fg, user, false);
        
        list=APILocator.getFieldAPI().getFieldVariablesForField(ff.getInode(), user, false);
        assertEquals(2,list.size());
        assertEquals(list.get(0).getKey(),fv.getKey());
        assertEquals(list.get(0).getValue(),fv.getValue());
        assertEquals(list.get(1).getKey(),fg.getKey());
        assertEquals(list.get(1).getValue(),fg.getValue());
        
        try{
        	HibernateUtil.startTransaction();
        	FieldFactory.deleteField(ff);
        	StructureFactory.deleteStructure(st);
        	HibernateUtil.closeAndCommitTransaction();
        }catch(Exception e){
        	HibernateUtil.rollbackTransaction();
        	Logger.error(FieldAPITest.class, e.getMessage());
        }
       
    }
}
