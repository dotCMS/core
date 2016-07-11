package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentTypes;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableFileAssetContentType;
import com.dotcms.contenttype.model.type.ImmutableFormContentType;
import com.dotcms.contenttype.model.type.ImmutablePageContentType;
import com.dotcms.contenttype.model.type.ImmutablePersonaContentType;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.ImmutableWidgetContentType;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.test.DataSourceForTesting;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.folders.business.FolderAPI;

public class ContentTypeFactoryImplTest {

	ContentTypeFactory factory = new ContentTypeFactoryImpl();

	@BeforeClass
	public static void initDb() throws FileNotFoundException, Exception{
		//DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
	}
	@BeforeClass
	public static void cleanDb() throws DotDataException, Exception{
		DbConnectionFactory.overrideDefaultDatasource(new DataSourceForTesting().getDataSource());
		DotConnect dc = new DotConnect();
		dc.setSQL("delete from field where structure_inode in (select inode from structure where structure.velocity_var_name like 'velocityVarNameTesting%')");
		dc.loadResult();		

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();
		

		dc.setSQL("delete from structure where structure.velocity_var_name like 'velocityVarNameTesting%' ");
		dc.loadResult();
		dc.setSQL("delete from inode where type='structure' and inode not in  (select inode from structure)");
		dc.loadResult();
		dc.setSQL("delete from field where structure_inode not in (select inode from structure)");
		dc.loadResult();		
		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();
		dc.setSQL("update structure set structure.url_map_pattern =null, structure.page_detail=null where structuretype =3" );
		dc.loadResult();
	}

	@Test
	public void testDifferentContentTypes() throws Exception {
		
		ContentType content = factory.find(Constants.CONTENT);
		ContentType news = factory.find(Constants.NEWS);
		ContentType widget = factory.find(Constants.WIDGET);
		ContentType form = factory.find(Constants.FORM);
		ContentType fileAsset = factory.find(Constants.FILEASSET);
		ContentType htmlPage = factory.find(Constants.HTMLPAGE);
		ContentType persona = factory.find(Constants.PERSONA);
		
		// Test all the types
		assertThat("ContentType is type Content", content.baseType() == BaseContentTypes.CONTENT);
		assertThat("ContentType is type Content", content instanceof ImmutableSimpleContentType);
		assertThat("News is not simple content", !news.equals(content));
		
		assertThat("ContentType is type FILEASSET", fileAsset.baseType() == BaseContentTypes.FILEASSET);
		assertThat("ContentType is type FILEASSET", fileAsset instanceof ImmutableFileAssetContentType);
		assertThat("ContentType is type WIDGET", widget.baseType() == BaseContentTypes.WIDGET);
		assertThat("ContentType is type WIDGET", widget instanceof ImmutableWidgetContentType);
		
		assertThat("ContentType is type FORM", form.baseType() == BaseContentTypes.FORM);
		assertThat("ContentType is type FORM", form instanceof ImmutableFormContentType);
		assertThat("ContentType is type PERSONA", persona.baseType() == BaseContentTypes.PERSONA);
		assertThat("ContentType is type PERSONA", persona instanceof ImmutablePersonaContentType);
		assertThat("ContentType is type HTMLPAGE", htmlPage.baseType() == BaseContentTypes.HTMLPAGE);
		assertThat("ContentType is type HTMLPAGE", htmlPage instanceof ImmutablePageContentType );

	}

	@Test
	public void testFindMethodEquals() throws Exception {
		List<ContentType> types = factory.findAll();
		for (ContentType type : types) {
			ContentType contentType = factory.find(type.inode());
			ContentType contentType2 = factory.findByVar(type.velocityVarName());
			try {
				assertThat("ContentType == ContentType2", contentType.equals(contentType2) && contentType.equals(type));
			} catch (Throwable t) {

				throw t;
			}
		}
	}

	@Test
	public void testFindAll() throws Exception {
		List<ContentType> types = factory.findAll();
		assertThat("findAll sort by Name has same size as find all", factory.findAll("name").size() == types.size());
	}
	
	
	
	@Test
	public void testAddingContentTypes() throws Exception{
		int count = factory.searchCount(null);
		int runs = 20;
		
		for(int i=0;i<runs;i++){
			long time = System.currentTimeMillis();
			int base=(i % 5)+1;
			Thread.sleep(1);
			ContentType type = ContentTypeBuilder.builder(BaseContentTypes.getContentTypeClass(base))
				.description("description" + time)
				.folder(FolderAPI.SYSTEM_FOLDER)
				.host(Constants.SYSTEM_HOST)
				.name("ContentTypeTesting" + time)
				.owner("owner")
				.velocityVarName("velocityVarNameTesting" + time)
				.build();
			type = factory.save(type);
		}
		int count2 = factory.searchCount(null);
		assertThat("contenttypes are added", count == count2-runs);
	}
	
	
	@Test
	public void testAddingWidgets() throws Exception{

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countWidgets = factory.searchCount(null, BaseContentTypes.WIDGET);
		
		for(int i=0;i<runs;i++){
			addWidget();
			Thread.sleep(1);
		}
		
		
		int countAll2 = factory.searchCount(null);
		int countWidgets2 = factory.searchCount(null, BaseContentTypes.WIDGET);
		assertThat("counts are working", countAll == countAll2-runs);
		assertThat("counts are working", countAll2 >countWidgets2);
		assertThat("counts are working", countWidgets == countWidgets2-runs);

	}
	
	@Test
	public void testAddingPersonas() throws Exception{

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countPersonas = factory.searchCount(null, BaseContentTypes.PERSONA);
		
		for(int i=0;i<runs;i++){
			addPersona();
			Thread.sleep(1);
		}
		
		
		int countAll2 = factory.searchCount(null);
		int countPersonas2 = factory.searchCount(null, BaseContentTypes.PERSONA);
		assertThat("counts are working", countAll == countAll2-runs);
		assertThat("counts are working", countAll2 >countPersonas2);
		assertThat("counts are working", countPersonas == countPersonas2-runs);

	}
	
	@Test
	public void testAddingForms() throws Exception{

		int countAll = factory.searchCount(null);
		int runs = 20;
		int countForms = factory.searchCount(null, BaseContentTypes.FORM);
		
		for(int i=0;i<runs;i++){
			addForm();
			Thread.sleep(1);
		}
		
		
		int countAll2 = factory.searchCount(null);
		int countForms2 = factory.searchCount(null, BaseContentTypes.FORM);
		assertThat("counts are working", countAll == countAll2-runs);
		assertThat("counts are working", countAll2 >countForms2);
		assertThat("counts are working", countForms == countForms2-runs);

	}
	public void addWidget() throws DotDataException{

		long i = System.currentTimeMillis();

		ContentType type = ImmutableWidgetContentType.builder()
			.description("description" + i)
			.expireDateVar(null)
			.folder(FolderAPI.SYSTEM_FOLDER)
			.host(Constants.SYSTEM_HOST)
			.name("widgetTesting" + i)
			.owner("owner")

			.velocityVarName("velocityVarNameTesting" + i)
			.build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ImmutableWidgetContentType.builder().name("test").velocityVarName("rewarwa").build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());
		
		for(int j=0;j<fields.size();j++){
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields are correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields are correct:", field.variable().equals(baseField.variable()));
			assertThat("fields are correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields are correct:", field.name().equals(baseField.name()));
			assertThat("fields are correct:", field.sortOrder()==baseField.sortOrder());
		}
	}
	
	public void addForm() throws DotDataException{

		long i = System.currentTimeMillis();

		ContentType type = ImmutableFormContentType.builder()
			.description("description" + i)
			.expireDateVar(null)
			.folder(FolderAPI.SYSTEM_FOLDER)
			.host(Constants.SYSTEM_HOST)
			.name("FormTesting" + i)
			.owner("owner")
			.velocityVarName("velocityVarNameTesting" + i)
			.build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ImmutableFormContentType.builder().name("test").velocityVarName("rewarwa").build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());
		
		for(int j=0;j<fields.size();j++){
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields are correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields are correct:", field.variable().equals(baseField.variable()));
			assertThat("fields are correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields are correct:", field.name().equals(baseField.name()));
			assertThat("fields are correct:", field.sortOrder()==baseField.sortOrder());
		}
	}
		
	public void addPersona() throws DotDataException{

		long i = System.currentTimeMillis();

		ContentType type = ImmutablePersonaContentType.builder()
			.description("description" + i)
			.expireDateVar(null)
			.folder(FolderAPI.SYSTEM_FOLDER)
			.host(Constants.SYSTEM_HOST)
			.name("PersonaTesting" + i)
			.owner("owner")
			.velocityVarName("velocityVarNameTesting" + i)
			.build();
		type = factory.save(type);

		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.inode());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(type).build().requiredFields();
		assertThat("fields are all added", fields.size() == baseTypeFields.size());
		
		for(int j=0;j<fields.size();j++){
			Field field = fields.get(j);
			Field baseField = baseTypeFields.get(j);
			assertThat("fields dataType correct:", field.dataType().equals(baseField.dataType()));
			assertThat("fields variable correct:", field.variable().equals(baseField.variable()));
			assertThat("fields getClass correct:", field.getClass().equals(baseField.getClass()));
			assertThat("fields name are correct:", field.name().equals(baseField.name()));
			assertThat("fields sortOrder are correct:", field.sortOrder()==baseField.sortOrder());
		}
	}
	
	
}
