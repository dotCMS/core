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
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;
import org.quartz.JobExecutionException;

import java.util.Date;
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

        Boolean boolValue = Boolean.TRUE;
        Date dateValue = new Date();
        Integer integerValue = 23;
        Float floatValue = 2f;
        String textValue = "Some content";
        String textAreaValue = "Some content,Some content,Some content,Some content,Some content,Some content,Some content," +
                "Some content,Some content,Some content,Some content,Some content,Some content,Some content";

        String currentTime = String.valueOf(new Date().getTime());
        String contentTypeName = "DeleteFieldContentType_" + currentTime;
        String textAreaFieldVarName = "textAreaFieldVarName_" + currentTime;
        String integerFieldVarName = "integerFieldVarName_" + currentTime;
        String floatFieldVarName = "floatFieldVarName_" + currentTime;
        String textFieldVarName = "textFieldVarName_" + currentTime;
        String dateFieldVarName = "dateFieldVarName_" + currentTime;
        String radioFieldVarName = "radioFieldVarName_" + currentTime;

        //Create content types
        Structure contentType = new Structure();
        contentType.setHost(host.getIdentifier());
        contentType.setDescription("Testing delete content types's field");
        contentType.setName(contentTypeName);
        contentType.setVelocityVarName("deleteFieldVarName_" + currentTime);
        contentType.setStructureType(Structure.STRUCTURE_TYPE_CONTENT);
        contentType.setFixed(false);
        contentType.setOwner(systemUser.getUserId());
        contentType.setExpireDateVar("");
        contentType.setPublishDateVar("");

        Contentlet contentlet = null;

        try{

            //Save the test structure
            StructureFactory.saveStructure(contentType);

            //Adding the test fields

            Field radioField = new Field("radioField_" + currentTime, Field.FieldType.RADIO, Field.DataType.BOOL,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            radioField.setVelocityVarName(radioFieldVarName);
            FieldFactory.saveField(radioField);
            FieldsCache.addField(radioField);

            Field dateField = new Field("dateField_" + currentTime, Field.FieldType.DATE, Field.DataType.DATE,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            dateField.setVelocityVarName(dateFieldVarName);
            FieldFactory.saveField(dateField);
            FieldsCache.addField(dateField);

            Field textAreaField = new Field("textAreaField_" + currentTime, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            textAreaField.setVelocityVarName(textAreaFieldVarName);
            FieldFactory.saveField(textAreaField);
            FieldsCache.addField(textAreaField);

            Field integerField = new Field("integerField_" + currentTime, Field.FieldType.TEXT, Field.DataType.INTEGER,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            integerField.setVelocityVarName(integerFieldVarName);
            FieldFactory.saveField(integerField);
            FieldsCache.addField(integerField);

            Field floatField = new Field("floatField_" + currentTime, Field.FieldType.TEXT, Field.DataType.FLOAT,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            floatField.setVelocityVarName(floatFieldVarName);
            FieldFactory.saveField(floatField);
            FieldsCache.addField(floatField);

            Field textField = new Field("textField_" + currentTime, Field.FieldType.TEXT, Field.DataType.TEXT,
                    contentType, true, true, true, 1, "", "",
                    "", false, false, true);
            textField.setVelocityVarName(textFieldVarName);
            FieldFactory.saveField(textField);
            FieldsCache.addField(textField);

            //Validate the fields were properly saved
            Structure stFromDB = CacheLocator.getContentTypeCache().getStructureByName(contentTypeName);
            List<Field> fieldsBySortOrder = stFromDB.getFieldsBySortOrder();

            assertEquals(6, fieldsBySortOrder.size());

            //Create a new content of the DeleteFieldContentType type
            contentlet = new Contentlet();
            contentlet.setStructureInode(contentType.getInode());
            contentlet.setHost(host.getIdentifier());
            contentlet.setLanguageId(langId);

            //Set the fields values
            contentletAPI.setContentletProperty(contentlet, dateField, dateValue);
            contentletAPI.setContentletProperty(contentlet, radioField, boolValue);
            contentletAPI.setContentletProperty(contentlet, textAreaField, textAreaValue);
            contentletAPI.setContentletProperty(contentlet, integerField, integerValue);
            contentletAPI.setContentletProperty(contentlet, floatField, floatValue);
            contentletAPI.setContentletProperty(contentlet, textField, textValue);

            //Save the content
            contentlet = contentletAPI.checkin(contentlet, systemUser, true);

            //Delete fields
            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", dateField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", radioField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textAreaField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", integerField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", floatField, "user", systemUser));

            TestJobExecutor.execute(instance,
                    CollectionsUtils.map("structure", contentType, "field", textField, "user", systemUser));

            //Validate we deleted those fields properly
            stFromDB = CacheLocator.getContentTypeCache().getStructureByName(contentTypeName);
            fieldsBySortOrder = stFromDB.getFieldsBySortOrder();
            assertEquals(0, fieldsBySortOrder.size());

            //Make sure the values are not in cache
            Contentlet contentletFromDB = CacheLocator.getContentletCache().get(contentlet.getInode());
            assertNull(contentletFromDB.get(dateFieldVarName));
            assertNull(contentletFromDB.get(radioFieldVarName));
            assertNull(contentletFromDB.get(textAreaFieldVarName));
            assertNull(contentletFromDB.get(integerFieldVarName));
            assertNull(contentletFromDB.get(floatFieldVarName));
            assertNull(contentletFromDB.get(textFieldVarName));
        }catch(Exception e){

            if (contentType != null){
                try {
                    StructureFactory.deleteStructure(contentType);
                } catch (DotDataException e1) {
                    //Do nothing....
                }
            }

            if (contentlet != null) {
                try {
                    contentletAPI.delete(contentlet, systemUser, true);
                } catch (Exception e1) {
                    //Do nothing....
                }
            }

            throw new RuntimeException(e);
        }
    }


}
