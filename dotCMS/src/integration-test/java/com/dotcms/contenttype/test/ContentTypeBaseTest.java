package com.dotcms.contenttype.test;

import javax.servlet.http.HttpServletRequest;

import org.junit.BeforeClass;

import com.dotcms.IntegrationTestBase;
import com.dotcms.api.web.HttpServletRequestThreadLocal;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.ContentTypeFactory;
import com.dotcms.contenttype.business.ContentTypeFactoryImpl;
import com.dotcms.contenttype.business.FieldAPIImpl;
import com.dotcms.contenttype.business.FieldFactoryImpl;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHttpRequest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.common.db.DotConnect;
import com.liferay.portal.model.User;

public class ContentTypeBaseTest extends IntegrationTestBase {

	protected static User user;
	protected static ContentTypeFactory contentTypeFactory;
	protected static ContentTypeAPIImpl contentTypeApi;
	protected static FieldFactoryImpl fieldFactory;
	protected static FieldAPIImpl fieldApi;

	@BeforeClass
	public static void prepare () throws Exception {
		//Setting web app environment
		IntegrationTestInitService.getInstance().init();

		user = APILocator.systemUser();
		contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
		contentTypeFactory = new ContentTypeFactoryImpl();
		fieldFactory = new FieldFactoryImpl();
		fieldApi = new FieldAPIImpl();


		HttpServletRequest pageRequest = new MockSessionRequest(
				new MockAttributeRequest(
						new MockHttpRequest("localhost", "/").request()
						).request())
				.request();
		HttpServletRequestThreadLocal.INSTANCE.setRequest(pageRequest);


		DotConnect dc = new DotConnect();
		String structsToDelete = "(select inode from structure where structure.velocity_var_name like 'velocityVarNameTesting%' )";

		dc.setSQL("delete from field where structure_inode in " + structsToDelete);
		dc.loadResult();

		dc.setSQL("delete from inode where type='field' and inode not in  (select inode from field)");
		dc.loadResult();

		dc.setSQL("delete from contentlet_version_info where identifier in (select identifier from contentlet where structure_inode in "
				+ structsToDelete + ")");
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
	}
}
