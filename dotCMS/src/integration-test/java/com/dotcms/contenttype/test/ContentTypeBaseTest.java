package com.dotcms.contenttype.test;

import static org.hamcrest.MatcherAssert.assertThat;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.contenttype.model.field.ConstantField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.ImmutableSimpleContentType;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.languagevariable.business.LanguageVariableAPI;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.google.common.collect.ImmutableList;
import com.liferay.portal.model.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class ContentTypeBaseTest extends IntegrationTestBase {

	protected static User user;
	protected static ContentTypeFactory contentTypeFactory;
	protected static ContentTypeAPIImpl contentTypeApi;
	protected static FieldFactoryImpl fieldFactory;
	protected static FieldAPIImpl fieldApi;

	private static User chrisPublisher;
	private static ContentType languageVariableContentType;
	protected static ContentType newsLikeContentType;

	@BeforeClass
	public static void prepare () throws Exception {
		//Setting web app environment
		IntegrationTestInitService.getInstance().init();

		user = APILocator.systemUser();
		contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
		contentTypeFactory = new ContentTypeFactoryImpl();
		fieldFactory = new FieldFactoryImpl();
		fieldApi = new FieldAPIImpl();

		
		CacheLocator.getContentTypeCache2().clearCache();
		
		
		

		HttpServletRequest pageRequest = new MockSessionRequest(
				new MockAttributeRequest(
						new MockHttpRequest("localhost", "/").request()
						).request())
				.request();
		HttpServletRequestThreadLocal.INSTANCE.setRequest(pageRequest);


		DotConnect dc = new DotConnect();
		String structsToDelete = "(select inode from structure where structure.velocity_var_name like 'velocityVarNameTesting%' )";
		String contentsStructsToDelete = "(select identifier from contentlet c inner join structure s on c.structure_inode = s.inode where s.velocity_var_name like 'velocityVarNameTesting%')";

		dc.setSQL("delete from field where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("delete from contentlet_version_info where identifier in (select identifier from contentlet where structure_inode in "
				+ structsToDelete + ")");
		dc.loadResult();

		new DotConnect()
				.setSQL("delete from workflow_comment where workflowtask_id   in " + contentsStructsToDelete)
				.loadResult();

		new DotConnect()
				.setSQL("delete from workflow_history where workflowtask_id   in " + contentsStructsToDelete)
				.loadResult();

		new DotConnect()
				.setSQL("delete from workflowtask_files where workflowtask_id in " + contentsStructsToDelete)
				.loadResult();

		dc.setSQL("delete from workflow_task where webasset in " + contentsStructsToDelete);
		dc.loadResult();


		dc.setSQL("delete from contentlet where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='contentlet' and inode not in  (select inode from contentlet)");
		dc.loadResult();

		dc.setSQL("delete from structure where  structure.velocity_var_name like 'velocityVarNameTesting%' ");
		dc.loadResult();

		dc.loadResult();
		dc.setSQL("delete from inode where type='structure' and inode not in  (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from field where structure_inode not in (select inode from structure)");
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("update structure set url_map_pattern =null, page_detail=null where structuretype =3");
		dc.loadResult();

		//Creating test users
		chrisPublisher = TestUserUtils.getChrisPublisherUser();

		//Test news content type
		newsLikeContentType = TestDataUtils.getNewsLikeContentType();

		final String contentTypeVelocityVarName = LanguageVariableAPI.LANGUAGEVARIABLE;
		try {
			// Using the provided Language Variable Content Type
			languageVariableContentType = APILocator.getContentTypeAPI(user)
					.find(contentTypeVelocityVarName);
		} catch (Exception e) {

			// Content Type not found, then create it
			final String contentTypeName = "Language Variable";
			final Host site = APILocator.getHostAPI().findSystemHost(user, Boolean.FALSE);

			languageVariableContentType = new ContentTypeDataGen()
					.baseContentType(BaseContentType.KEY_VALUE)
					.host(site)
					.description("Testing the Language Variable API.")
					.name(contentTypeName)
					.velocityVarName(contentTypeVelocityVarName)
					.fixed(Boolean.TRUE)
					.user(user).nextPersisted();
		}
	}

	@AfterClass
	public static void cleanUp() {
		ContentTypeDataGen.remove(languageVariableContentType);
		UserDataGen.remove(chrisPublisher);
	}

	protected void insert(BaseContentType baseType) throws Exception {

		ContentTypeAPI contentTypeApi = APILocator.getContentTypeAPI(APILocator.systemUser());

		long i = System.currentTimeMillis();

		//Create a new content type
		ContentTypeBuilder builder = ContentTypeBuilder.builder(baseType.immutableClass())
				.description("description" + i)
				.expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
				.name(baseType.name() + "Testing" + i).owner("owner")
				.variable("velocityVarNameTesting" + i);

		ContentType type = builder.build();
		type = contentTypeApi.save(type);

		//Search for the new content type
		ContentType type2 = contentTypeApi.find(type.id());
		try {
			assertThat("Type saved correctly", type2.equals(type));
		} catch (Throwable t) {
			System.out.println("Old and New Content Types are NOT the same");
			System.out.println(type);
			System.out.println(type2);
			throw t;
		}

		//Getting the fields of the just saved content type
		List<Field> fields = new FieldFactoryImpl().byContentTypeId(type.id());
		List<Field> baseTypeFields = ContentTypeBuilder.builder(baseType.immutableClass())
				.name("test").variable("rewarwa").build().requiredFields();

		fields = sortListByVariable(fields);
		baseTypeFields = sortListByVariable(baseTypeFields);

		try {
			assertThat("fields are all added:\n" + fields + "\n" + baseTypeFields,
					fields.size() == baseTypeFields.size());
		} catch (Throwable e) {
			System.out.println(e.getMessage());
			System.out.println("Saved  db: " + fields);
			System.out.println("not saved: " + baseTypeFields);
			System.out.println("\n");
			throw e;
		}

		for (int j = 0; j < fields.size(); j++) {
			Field field = fields.get(j);
			Field baseField = null;
			try {
				baseField = baseTypeFields.get(j);
				assertThat("field datatypes are not correct:",
						field.dataType().equals(baseField.dataType()));
				assertThat("fields variable is not correct:",
						field.variable().equals(baseField.variable()));
				assertThat("field class is not correct:",
						field.getClass().equals(baseField.getClass()));
				assertThat("field name is  not correct:", field.name().equals(baseField.name()));

				assertThat("field sort order is not correct",
						field.sortOrder() == baseField.sortOrder());
			} catch (Throwable e) {
				System.out.println(e.getMessage());
				System.out.println("Saved  db: " + field);
				System.out.println("not saved: " + baseField);
				System.out.println("\n");
				throw e;

			}
		}
	}

	/**
	 * Sorts an unmodifiable list by variable name
	 *
	 * @param list The list to be sorted
	 * @return Sorted List of Field
	 */
	protected List<Field> sortListByVariable(List<Field> list) {
	    List<Field> sortedList = new ArrayList<>();
	    sortedList.addAll(list);
	    sortedList.sort(Comparator.comparing(Field::variable));
		return Collections.unmodifiableList(sortedList);
	}

	
	

	
	
	
	
	
	

}
