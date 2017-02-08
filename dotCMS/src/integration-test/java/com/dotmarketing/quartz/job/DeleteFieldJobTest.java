package com.dotmarketing.quartz.job;


import com.dotcms.IntegrationTestBase;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionException;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DeleteFieldJobTest extends IntegrationTestBase {

    final DeleteFieldJob instance = new DeleteFieldJob();

    @BeforeClass
    public static void prepare() throws Exception{
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
    }

    @Test
    public void deleteContentTypeField() throws DotDataException, DotSecurityException, JobExecutionException {
        User systemUser = APILocator.getUserAPI().getSystemUser();
        Host host = APILocator.getHostAPI().findDefaultHost(systemUser, true);
        long langId =APILocator.getLanguageAPI().getDefaultLanguage().getId();
        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Create content types
        Structure contentType = new Structure();
        contentType.setHost(host.getIdentifier());
        contentType.setDescription("Testing delete content types's field");
        contentType.setName("DeleteField");
        contentType.setVelocityVarName("deletefield");
        contentType.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        contentType.setFixed(false);
        contentType.setOwner(systemUser.getUserId());
        contentType.setExpireDateVar("");
        contentType.setPublishDateVar("");

        Contentlet contentlet = null;

        try{
            StructureFactory.saveStructure(contentType);

            //Add fileds
            Field floatFied = new Field("float",Field.FieldType.TEXT,Field.DataType.FLOAT,contentType,true,true,true,1,"", "", "", false, false, true);
            floatFied.setVelocityVarName("float");
            FieldFactory.saveField(floatFied);
            FieldsCache.addField(floatFied);

            Field textFied = new Field("text",Field.FieldType.TEXT,Field.DataType.TEXT,contentType,true,true,true,1,"", "", "", false, false, true);
            textFied.setVelocityVarName("text");
            FieldFactory.saveField(textFied);
            FieldsCache.addField(textFied);

            Structure stFromDB = CacheLocator.getContentTypeCache().getStructureByName("DeleteField");
            List<Field> fieldsBySortOrder = stFromDB.getFieldsBySortOrder();

            assertEquals(2, fieldsBySortOrder.size());

            //content
            contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.getInode());
            contentlet.setHost(host.getIdentifier());
            contentlet.setLanguageId(langId);

            contentletAPI.setContentletProperty( contentlet, floatFied, 2f );
            contentletAPI.setContentletProperty( contentlet, textFied, "content" );

            contentlet = contentletAPI.checkin(contentlet, systemUser, true);

            //Delete fields
            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", floatFied, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textFied, "user", systemUser));

            //asserts
            stFromDB = CacheLocator.getContentTypeCache().getStructureByName("DeleteField");
            fieldsBySortOrder = stFromDB.getFieldsBySortOrder();
            assertEquals(0, fieldsBySortOrder.size());

            assertEquals(2f, contentlet.get("float"));
            assertEquals("content", contentlet.get("text"));

            Contentlet contentletFromDB = CacheLocator.getContentletCache().get(contentlet.getInode());
            assertNull(contentletFromDB.get("float"));
            assertNull("content", contentletFromDB.get("text"));
        }catch(Exception e){

            if (contentType != null){
                StructureFactory.deleteStructure(contentType);
            }

            if (contentlet != null) {
                contentletAPI.delete(contentlet, systemUser, true);
            }

            throw new RuntimeException();
        }
    }


}
